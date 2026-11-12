// MatchSession (match_session.cpp) as the vsync hook uses it, from Dragon Rage's bt2/plugin/tests/fight_tests.cpp: two games in two
// threads on real UDP sockets (the loopback), or only through a relay channel standing in for the room's WebSocket. Each asks for its
// pads once per field like the vsync hook. What both are given must be the two pads the players made, the host's as player 1; the one
// that starts first must wait for the other; a game that leaves must end the other's match; a difference of the memory must be found,
// and one in the pages left out of the check must not. recomp/android/host/online/tests/run.sh
#include "match_session.h"
#include "net_version.h"
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <mutex>
#include <random>
#include <thread>
#include <vector>

namespace {
int failures = 0, checks = 0;
std::mutex printing;
#define CHECK(cond, ...) do {++checks; if (!(cond)) {++failures; std::lock_guard<std::mutex> l(printing); std::printf("FAIL %s:%d: ", __FILE__, __LINE__); std::printf(__VA_ARGS__); std::printf("\n");}} while (0)

using Sample = Lockstep::Sample;
using Skip = std::vector<std::pair<std::uint32_t, std::uint32_t>>;

// The room's relay: what one side sends, the other receives, in order (a WebSocket keeps order and loses nothing).
struct Relay {
    std::mutex m;
    std::deque<std::vector<std::uint8_t>> toHost, toGuest;
    Link::Channel side(bool host) {
        Link::Channel c;
        c.send = [this, host](const std::uint8_t* p, std::size_t n) {
            std::lock_guard<std::mutex> l(m);
            (host ? toGuest : toHost).emplace_back(p, p + n);
            return true;
        };
        c.receive = [this, host](std::uint8_t* out, std::size_t cap) -> std::size_t {
            std::lock_guard<std::mutex> l(m);
            auto& q = host ? toHost : toGuest;
            if (q.empty()) return 0;
            const std::size_t n = std::min(cap, q.front().size());
            std::memcpy(out, q.front().data(), n);
            q.pop_front();
            return n;
        };
        return c;
    }
};

struct Game {
    MatchSession fs;
    std::vector<Sample> script;                 // what this player makes, field by field
    std::vector<Lockstep::Pair> seen;
    std::atomic<int> endedWith{0};              // 1: the match ended while it was running
    std::string reason;
    bool began = false;
    std::vector<std::uint8_t> ram = std::vector<std::uint8_t>(1 << 20, 0);
    std::thread th;
};

std::vector<Sample> makeScript(unsigned seed, unsigned n) {
    std::mt19937 r(seed);
    std::vector<Sample> v;
    for (unsigned i = 0; i < n; ++i) {Sample s{}; for (auto& b : s) b = std::uint8_t(r()); v.push_back(s);}
    return v;
}

struct Options {
    int loss = 0;
    unsigned hostStartMs = 0, guestStartMs = 0, leaveAfter = 0;
    bool relayOnly = false;                     // nothing goes over UDP: the relay channel only
    unsigned delayFrames = 0;
    Skip skip;
    std::uint32_t tamperAt = 0;                 // the guest's memory changes at this address from field 100 on (0: never)
};

void play(Game& g, SessionFile file, unsigned fields, const Options& o, Relay* relay) {
    std::this_thread::sleep_for(std::chrono::milliseconds(file.host ? o.hostStartMs : o.guestStartMs));
    MatchSession::Env env;
    env.log = [&](const std::string& t) {if (std::getenv("MATCH_LOG")) {std::lock_guard<std::mutex> l(printing); std::printf("[%s] %s\n", file.host ? "host" : "guest", t.c_str()); std::fflush(stdout);}};
    env.onEnded = [&](const std::string& why) {g.reason = why;};
    env.ram = g.ram.data();
    env.ramSize = g.ram.size();
    env.skip = o.skip;
    env.meetTimeoutMs = 20000;
    if (relay) env.channel = relay->side(file.host);
    g.began = g.fs.begin(file, env);
    if (!g.began) return;
    g.fs.link().setTestLoss(o.loss);
    if (o.relayOnly) {g.fs.link().setTestNoDirect(true); g.fs.link().setRelayAfterMs(200);}
    for (;;) {                                  // the game: one call per field
        std::uint8_t p1[6], p2[6], local[6] = {};
        if (g.fs.field() < g.script.size()) std::memcpy(local, g.script[g.fs.field()].data(), 6);
        // the game's memory: the same on both sides (it follows the field), but for the address the test changes on one of them
        const std::uint32_t f = g.fs.field();
        std::memcpy(g.ram.data() + 0x1000, &f, 4);
        if (o.tamperAt && !file.host && f >= 100) g.ram[o.tamperAt] = 0x5A;
        const int r = g.fs.padField(local, p1, p2);
        if (r) {
            Lockstep::Pair p;
            std::memcpy(p.p1.data(), p1, 6); std::memcpy(p.p2.data(), p2, 6);
            g.seen.push_back(p);
            if (g.seen.size() >= fields) break;
            if (o.leaveAfter && file.host && g.seen.size() == o.leaveAfter) {g.fs.leave("the player left"); break;}
        } else {g.endedWith = 1; break;}
        std::this_thread::sleep_for(std::chrono::microseconds(500));   // (a field's work)
    }
}

SessionFile fileFor(bool host, std::uint16_t myPort, std::uint16_t peerPort, const std::string& code, const Options& o) {
    SessionFile f;
    f.version = HL_NET_VERSION;
    f.host = host; f.port = myPort; f.code = code; f.myName = host ? "Host" : "Guest"; f.peerName = host ? "Guest" : "Host";
    f.addresses = {"127.0.0.1:" + std::to_string(peerPort)};
    f.delayFrames = o.delayFrames;
    f.relay = o.relayOnly;
    return f;
}

// One match of `fields` fields. Returns whether both played them all with the right pads.
bool match(unsigned seed, unsigned fields, const Options& o, Game* keepHost = nullptr, Game* keepGuest = nullptr) {
    // Ports: two sockets are opened first to learn two free ports, then closed and taken again by the matches (as the engine takes
    // the port the app's lobby used).
    Link a, b;
    a.open(); b.open();
    const std::uint16_t pa = a.port(), pb = b.port();
    a.close(); b.close();
    Game hostOwn, guestOwn;
    Game& host = keepHost ? *keepHost : hostOwn;
    Game& guest = keepGuest ? *keepGuest : guestOwn;
    host.script = makeScript(seed * 2 + 1, fields + 64);
    guest.script = makeScript(seed * 2 + 2, fields + 64);
    Relay relay;
    const std::string code = std::to_string(100000 + seed);
    host.th = std::thread(play, std::ref(host), fileFor(true, pa, pb, code, o), fields, std::cref(o), o.relayOnly ? &relay : nullptr);
    guest.th = std::thread(play, std::ref(guest), fileFor(false, pb, pa, code, o), fields, std::cref(o), o.relayOnly ? &relay : nullptr);
    host.th.join(); guest.th.join();
    CHECK(host.began && guest.began, "seed %u: a match could not open its socket", seed);
    if (o.leaveAfter) {
        CHECK(host.seen.size() == o.leaveAfter, "seed %u: the leaving side played %zu fields", seed, host.seen.size());
        CHECK(guest.endedWith == 1, "seed %u: the other side's match did not end when this one left", seed);
        CHECK(guest.reason.find("left") != std::string::npos, "seed %u: the reason was \"%s\"", seed, guest.reason.c_str());
        return true;
    }
    CHECK(host.seen.size() == fields && guest.seen.size() == fields, "seed %u loss %d: the games played %zu and %zu of %u fields (%s / %s)", seed, o.loss,
          host.seen.size(), guest.seen.size(), fields, host.reason.c_str(), guest.reason.c_str());
    if (host.seen.size() != fields || guest.seen.size() != fields) return false;
    CHECK(host.fs.iAmPlayer1() && !guest.fs.iAmPlayer1(), "seed %u: the host is not player 1", seed);
    const unsigned delay = host.fs.delay();
    CHECK(delay == guest.fs.delay() && delay >= Lockstep::min_delay && delay <= Lockstep::max_delay, "seed %u: delays %u and %u", seed, delay, guest.fs.delay());
    if (o.delayFrames) CHECK(delay == o.delayFrames * 2, "seed %u: the host chose %u frames, the delay is %u fields", seed, o.delayFrames, delay);
    if (o.relayOnly) CHECK(host.fs.viaRelay() || host.fs.ended() || guest.fs.viaRelay(), "seed %u: a relay-only match did not say it was through the relay", seed);
    for (unsigned f = 0; f < fields; ++f) {
        Sample w1 = Lockstep::neutralSample(), w2 = Lockstep::neutralSample();
        if (f >= delay) {w1 = host.script[f - delay]; w2 = guest.script[f - delay];}
        for (const Game* g : {&host, &guest})
            if (g->seen[f].p1 != w1 || g->seen[f].p2 != w2) {CHECK(false, "seed %u loss %d: a game was given other pads at field %u", seed, o.loss, f); return false;}
    }
    if (!o.tamperAt) CHECK(host.fs.desyncs() == 0 && guest.fs.desyncs() == 0, "seed %u: a false difference of the state checks", seed);
    return true;
}
}

int main() {
    // the session file the app writes (android/.../data/Online.kt) reads back
    {
        SessionFile f, g;
        f.version = HL_NET_VERSION; f.host = true; f.port = 39550; f.code = "123456"; f.myName = "Eu"; f.peerName = "Voce";
        f.addresses = {"177.1.2.3:34599", "192.168.1.16:39550"}; f.delayFrames = 3; f.relay = true; f.at = 1800000000;
        CHECK(parseSessionFile(formatSessionFile(f), g) && g.host && g.port == 39550 && g.code == "123456" && g.peerName == "Voce" && g.addresses.size() == 2 &&
                  g.delayFrames == 3 && g.relay && g.version == HL_NET_VERSION && g.at == 1800000000, "the session file does not read back");
        CHECK(!parseSessionFile("garbage", g) && !parseSessionFile("role=host\ncode=X\n", g), "a session file without addresses or role was taken");
        CHECK(parseSessionFile("role=guest\ncode=1\nrelay=1\n", g) && g.addresses.empty(), "a relay-only session file was refused");
        CHECK(parseSessionFile("role=guest\ncode=1\naddr=1.2.3.4:5\ndelay=9\n", g) && g.delayFrames == 0, "a delay out of 0..4 was taken");
    }
    // the state check leaves out the pages it is told to, and nothing else
    {
        std::vector<std::uint8_t> a(1 << 16, 7), b = a;
        const Skip skip = {{0x4000, 0x5000}};
        for (std::uint32_t field = 0; field < 8 * Lockstep::digest_every; field += Lockstep::digest_every)
            CHECK(MatchSession::digestOf(a.data(), a.size(), skip, field) == MatchSession::digestOf(b.data(), b.size(), skip, field), "equal memory, other values");
        b[0x4321] ^= 1;
        bool same = true;
        for (std::uint32_t field = 0; field < 8 * Lockstep::digest_every; field += Lockstep::digest_every)
            same = same && MatchSession::digestOf(a.data(), a.size(), skip, field) == MatchSession::digestOf(b.data(), b.size(), skip, field);
        CHECK(same, "a change in a skipped page was seen");
        b[0x6321] ^= 1;
        bool found = false;
        for (std::uint32_t field = 0; field < 8 * Lockstep::digest_every; field += Lockstep::digest_every)
            found = found || MatchSession::digestOf(a.data(), a.size(), skip, field) != MatchSession::digestOf(b.data(), b.size(), skip, field);
        CHECK(found, "a change outside the skipped pages was not seen in eight checks");
    }
    // a clean line, the guest's engine starts a moment after the host's
    {Options o; o.guestStartMs = 400; match(1, 400, o);}
    // either one first, lost packets
    {Options o; o.loss = 20; o.hostStartMs = 300; match(2, 300, o);}
    for (unsigned seed = 3; seed < 8; ++seed) {Options o; o.loss = 25; o.hostStartMs = seed * 20 % 200; o.guestStartMs = seed * 70 % 500; match(seed, 250, o);}
    // the host's chosen delay (3 frames: 6 fields) reaches both
    {Options o; o.delayFrames = 3; match(9, 200, o);}
    // only the room's relay works: the match goes through it
    {Options o; o.relayOnly = true; o.guestStartMs = 100; match(10, 300, o);}
    // a game that leaves ends the other's match
    {Options o; o.guestStartMs = 100; o.leaveAfter = 120; match(20, 300, o);}
    // the memory differs on one side: found; the same difference in a page left out: not found
    {
        Game host, guest;
        Options o; o.tamperAt = 0x30000;
        match(30, 500, o, &host, &guest);
        CHECK(host.fs.desyncs() == 1 && guest.fs.desyncs() == 1, "a difference of the memory was not found (%u, %u)", host.fs.desyncs(), guest.fs.desyncs());
        CHECK(host.fs.desyncField() >= 100 && host.fs.desyncField() < 100 + 9 * Lockstep::digest_every, "found at field %u", host.fs.desyncField());
    }
    {
        Game host, guest;
        Options o; o.tamperAt = 0x30000; o.skip = {{0x30000, 0x31000}};
        match(31, 500, o, &host, &guest);
        CHECK(host.fs.desyncs() == 0 && guest.fs.desyncs() == 0, "a difference in a page left out was taken as a desync");
    }
    std::printf("%d checks, %d failures\n", checks, failures);
    return failures ? 1 : 0;
}
