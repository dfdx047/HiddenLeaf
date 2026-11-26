#include "match_session.h"
#include "net_version.h"
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <random>
#include <sstream>
#include <thread>

bool parseSessionFile(const std::string& text, SessionFile& out) {
    out = SessionFile{};
    bool role = false;
    std::istringstream lines(text);
    for (std::string line; std::getline(lines, line);) {
        while (!line.empty() && (line.back() == '\r' || line.back() == ' ')) line.pop_back();
        const auto eq = line.find('=');
        if (eq == std::string::npos) continue;
        const std::string key = line.substr(0, eq), value = line.substr(eq + 1);
        if (key == "role") {out.host = value == "host"; out.spectator = value == "spectator"; role = value == "host" || value == "guest" || value == "spectator";}
        else if (key == "at") out.at = std::strtoll(value.c_str(), nullptr, 10);
        else if (key == "port") out.port = std::uint16_t(std::strtoul(value.c_str(), nullptr, 10));
        else if (key == "code") out.code = value;
        else if (key == "me") out.myName = value;
        else if (key == "peer") out.peerName = value;
        else if (key == "addr" && !value.empty() && out.addresses.size() < 8) out.addresses.push_back(value);
        else if (key == "delay") {const unsigned long d = std::strtoul(value.c_str(), nullptr, 10); out.delayFrames = d <= 4 ? unsigned(d) : 0;}
        else if (key == "relay") out.relay = value == "1";
        else if (key == "broadcast_spec") out.broadcastSpec = value == "1";
        else if (key == "ranked") out.ranked = value == "1";
        else if (key == "v") out.version = unsigned(std::strtoul(value.c_str(), nullptr, 10));
        else if (key == "token") out.token = value;
        else if (key == "input_mode") out.inputMode = value;
    }
    return role && !out.code.empty() && (!out.addresses.empty() || out.relay || out.spectator);
}

std::string formatSessionFile(const SessionFile& f) {
    std::string t = "v=" + std::to_string(f.version) + "\nrole=" + (f.host ? "host" : "guest") + "\nat=" + std::to_string(f.at) + "\nport=" + std::to_string(f.port) +
                    "\ncode=" + f.code + "\nme=" + f.myName + "\npeer=" + f.peerName + "\ndelay=" + std::to_string(f.delayFrames) + "\nrelay=" + (f.relay ? "1" : "0") + "\n";
    if (!f.token.empty()) t += "token=" + f.token + "\n";
    if (!f.inputMode.empty()) t += "input_mode=" + f.inputMode + "\n";
    for (const auto& a : f.addresses) t += "addr=" + a + "\n";
    return t;
}

std::uint32_t MatchSession::defaultNow() {
    return std::uint32_t(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now().time_since_epoch()).count());
}

void MatchSession::sleep(unsigned ms) const {
    if (env_.sleepMs) env_.sleepMs(ms);
    else std::this_thread::sleep_for(std::chrono::milliseconds(ms));
}

bool MatchSession::begin(const SessionFile& file, Env env) {
    file_ = file;
    env_ = std::move(env);
    if (!link_.open(file.port)) {log("online: the match could not open its socket"); return false;}
    link_.setGiveUpMs(env_.meetTimeoutMs);          // (the other side's engine starts a few seconds either way)
    if (file.relay && env_.channel.send) link_.setChannel(env_.channel);
    link_.start(file.code, file.addresses, file.token);
    if (!file.host) {ctl_.begin(false, SessionControl::Config{}); ctlBegun_ = true;}
    active_ = true;
    phase_ = Phase::Meeting;
    log("online: match " + std::string(file.host ? "(host)" : "(guest)") + " with " + file.peerName + ", room " + file.code + ", socket on port " +
        std::to_string(link_.port()) + (file.relay && env_.channel.send ? ", the room's relay ready" : ", no relay"));
    return true;
}

void MatchSession::pump() {
    link_.poll();
    for (const auto& n : link_.takeNotes()) log(n);
    Link::Message m;
    while (link_.popMessage(m)) {
        if (m.type == Lockstep::messageType) {lock_.receive(m.payload.data(), m.payload.size()); ctl_.peerIsRunning();}   // (a packet of the lockstep means the other side is past the meeting)
        else ctl_.onMessage(m.type, m.payload.data(), m.payload.size());
    }
    const std::uint32_t t = now();
    if (!ctlBegun_ && file_.host && link_.state() == Link::Up) {
        // the host offers the match's settings once it knows the line's round trip (a few pings), or after a moment without one
        if (!upSince_) upSince_ = t;
        if (link_.roundTripMs() >= 0 || t - upSince_ > 1500 || file_.delayFrames) {
            std::mt19937 rng(std::random_device{}() ^ t);
            cfg_.delay = std::uint8_t(file_.delayFrames ? file_.delayFrames * 2 : Lockstep::delayFor(link_.roundTripMs()));
            cfg_.hostIsP1 = true;                            // the room's creator is player 1 and drives the menus
            cfg_.seed = rng();
            ctl_.begin(true, cfg_);
            ctlBegun_ = true;
            ctl_.setArrived(0);                              // (the game is at its first vsync: begin starts the conversation afresh)
            log("online: the host offers delay " + std::to_string(cfg_.delay) + " fields (" + (file_.delayFrames ? "chosen" : "automatic") + "), round trip " +
                std::to_string(link_.roundTripMs()) + " ms" + (link_.viaRelay() ? ", through the room's relay" : ", direct"));
        }
    }
    if (ctlBegun_) {
        std::uint8_t type, buf[64];
        for (int n; (n = ctl_.next(t, type, buf, sizeof buf)) >= 0;) link_.sendMessage(type, buf, std::size_t(n));
    }
}

void MatchSession::sendInputs() {
    std::uint8_t pkt[1100];
    if (field_ < env_.localUntil) {lastSend_ = now(); return;}
    if (const std::size_t n = lock_.makePacket(pkt, sizeof pkt, lockField())) link_.sendMessage(Lockstep::messageType, pkt, n);
    lastSend_ = now();
}

void MatchSession::end(const std::string& why, bool said) {
    if (phase_ == Phase::Ended) return;
    if (!said) link_.sayBye();
    reason_ = why;
    phase_ = Phase::Ended;
    log("online: the match ends: " + why + " (" + std::to_string(field_) + " fields played, " + std::to_string(waits_) + " waited for, " +
        std::to_string(lock_.mismatches()) + " of " + std::to_string(lock_.compared()) + " check values differed)");
    link_.close();
    if (env_.onEnded) env_.onEnded(why);
}

void MatchSession::leave(const std::string& why) {
    if (!active_ || phase_ == Phase::Ended) return;
    end(why, false);
}

const char* MatchSession::lineBad() const {
    if (ctl_.versionBad()) return "the other game speaks another version of the match";
    if (link_.peerLeft()) return "the other player left";
    if (link_.state() == Link::Failed) return "the other game could not be reached";
    if (link_.state() == Link::Up && link_.silentMs() > int(phase_ == Phase::Running ? env_.silentRunningMs : 20000u)) return "the other game stopped answering";
    return nullptr;
}

void MatchSession::waited() {
    const std::uint32_t t = now();
    if (env_.onWait && t - lastWaitCall_ >= 100) {lastWaitCall_ = t; env_.onWait();}
}

std::uint64_t MatchSession::digestOf(const std::uint8_t* ram, std::size_t size, const std::vector<std::pair<std::uint32_t, std::uint32_t>>& skip, std::uint32_t field) {
    if (!ram || size < 64) return 0;
    std::uint64_t h[4] = {0x9E3779B97F4A7C15ull, 0xC2B2AE3D27D4EB4Full, 0x165667B19E3779F9ull, 0x27D4EB2F165667C5ull};
    const std::size_t blocks = size / 64;
    for (std::size_t b = (field / Lockstep::digest_every) % 8; b < blocks; b += 8) {
        const std::uint32_t at = std::uint32_t(b * 64);
        bool out = false;
        for (const auto& s : skip) out = out || (at + 64 > s.first && at < s.second);
        if (out) continue;
        std::uint64_t w[8];
        std::memcpy(w, ram + at, 64);
        for (int i = 0; i < 8; ++i) {h[i & 3] = (h[i & 3] ^ w[i]) * 0x9E3779B97F4A7C15ull; h[i & 3] ^= h[i & 3] >> 29;}
    }
    return h[0] ^ (h[1] << 1) ^ (h[2] << 2) ^ (h[3] << 3) ^ field;
}

int MatchSession::padField(const std::uint8_t local[6], std::uint8_t p1[6], std::uint8_t p2[6]) {
    if (!active_ || phase_ == Phase::Ended) return 0;      // (a match that is over: the game goes on with the phone's own pad)
    if (leaveRequested_.load(std::memory_order_acquire)) {end("the player left", false); return 0;}
    // (2026-11-14: a match died after 15 s of silence near the character select; the line only says something when the game comes
    // here, so a game busy elsewhere for long is mute: say how long it was away)
    if (phase_ == Phase::Running) {
        const std::uint32_t t0 = now();
        if (lastField_ && t0 - lastField_ > 1000)
            log("online: this game was away " + std::to_string(t0 - lastField_) + " ms before field " + std::to_string(field_) + " (link silent " +
                std::to_string(link_.silentMs()) + " ms)");
        lastField_ = t0;
    }
    pump();
    if (const char* bad = lineBad()) {end(bad, link_.peerLeft()); return 0;}
    if (phase_ == Phase::Meeting) {
        if (ctlBegun_) ctl_.setArrived(0);
        const std::uint32_t since = now();
        while (!ctl_.met()) {                    // the game's time stands still here: the engine that started first waits for the other
            pump();
            if (ctlBegun_) ctl_.setArrived(0);   // (the guest's conversation began at once; the host's once the line is up)
            if (const char* bad = lineBad()) {end(bad, link_.peerLeft()); return 0;}
            if (leaveRequested_.load(std::memory_order_acquire)) {end("the player left", false); return 0;}
            if (now() - since > env_.meetTimeoutMs) {end("the other game did not start in time", false); return 0;}
            waited();
            sleep(1);
        }
        cfg_ = ctl_.config();
        p1_ = file_.host == cfg_.hostIsP1;
        lock_.begin(p1_ ? 0 : 1, cfg_.delay);
        phase_ = Phase::Running;
        field_ = 0;
        log("online: both games are there; I am player " + std::string(p1_ ? "1" : "2") + ", delay " + std::to_string(cfg_.delay) + " fields, " +
            (link_.viaRelay() ? "through the room's relay" : "direct") + (ctl_.fieldsDiffer() ? "; the two arrived at DIFFERENT fields" : ""));
        if (env_.onMet) env_.onMet();
    }
    // The way in, the same on both phones: no waiting for the other (the link still pings and is heard, in pump)
    if (field_ < env_.localUntil && env_.localPads) {
        env_.localPads(field_, p1, p2);
        ++field_;
        if (field_ == env_.localUntil) log("online: the way in is over at field " + std::to_string(field_) + "; the lockstep begins");
        return 1;
    }
    // Running: this field's pad goes out; the game goes on when the other side's for this field is here.
    const std::uint32_t lf = lockField();
    Lockstep::Sample mine;
    std::memcpy(mine.data(), local, 6);
    lock_.pushLocal(lf, mine);
    // Over reliable WebSocket relay, batch 2 fields per packet (25 Hz, matching UN5's 25 Hz logic) unless we are about to wait,
    // cutting Cloudflare Durable Object message volume in half without adding latency.
    const bool needNow = !lock_.ready(lf);
    if (!link_.viaRelay() || needNow || (lf & 1u) == 0u) sendInputs();
    if (needNow) {
        ++waits_;
        const std::uint32_t waitFrom = now();
        std::uint32_t nextNote = waitFrom + 2000;
        const std::uint32_t resendEvery = link_.viaRelay() ? 250u : 8u;
        while (!lock_.ready(lf)) {
            if (now() >= nextNote) {
                nextNote += 2000;
                log("online: waiting " + std::to_string(now() - waitFrom) + " ms for the other's field " + std::to_string(field_) + "; link silent " +
                    std::to_string(link_.silentMs()) + " ms, packets in " + std::to_string(link_.packetsIn()) + ", out " + std::to_string(link_.packetsOut()) +
                    (link_.sendsFailed() ? " (" + std::to_string(link_.sendsFailed()) + " refused by the phone, errno " + std::to_string(link_.lastSendErrno()) + ")" : "") +
                    "; " + lock_.stateText());
            }
            pump();
            if (const char* bad = lineBad()) {end(bad, link_.peerLeft()); return 0;}
            if (leaveRequested_.load(std::memory_order_acquire)) {end("the player left", false); return 0;}
            if (now() - lastSend_ >= resendEvery) sendInputs(); // TCP relay: 250 ms keepalive; UDP: 8 ms resend
            waited();
            sleep(1);
        }
        lastField_ = now();
    }
    if (lf % Lockstep::digest_every == 0) lock_.noteDigest(lf, digestOf(env_.ram, env_.ramSize, env_.skip, lf));
    if (lock_.desync() && !desyncSaid_) {
        desyncSaid_ = true;
        ++desyncs_;
        log("online: the two games differ at field " + std::to_string(lock_.desyncField()) + " (the check values of the memory are not the same)");
        if (env_.strictDesync) {end("the two games are no longer the same", false); return 0;}
    }
    // (every later difference too, at most one line each 2 s: which fields still differ tells a counter of the boot from a match gone apart)
    if (lock_.mismatches() > mismatchesSaid_ && now() - mismatchSaidAt_ >= 2000) {
        log("online: the check values differ at field " + std::to_string(lock_.lastMismatch()) + " (" + std::to_string(lock_.mismatches()) +
            " of " + std::to_string(lock_.compared()) + " compared so far)");
        mismatchesSaid_ = lock_.mismatches();
        mismatchSaidAt_ = now();
    }
    const Lockstep::Pair pair = lock_.pair(lf);
    std::memcpy(p1, pair.p1.data(), 6);
    std::memcpy(p2, pair.p2.data(), 6);
    lock_.done(lf);
    ++field_;
    return 1;
}
