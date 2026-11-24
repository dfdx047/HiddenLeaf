#include "link.h"
#include "net_version.h"
#include <arpa/inet.h>
#include <cerrno>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <ifaddrs.h>
#include <net/if.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

namespace {
constexpr std::uint32_t magic = 0x354E4C48; // "HLN5"
constexpr std::uint8_t version = std::uint8_t(HL_NET_VERSION);
enum Type : std::uint8_t {hello = 1, hello_ack = 2, ping = 3, pong = 4, bye = 5};
constexpr std::size_t packet_size = 20;
constexpr std::uint32_t hello_every = 100, ping_every = 500;

void put32(std::uint8_t* p, std::uint32_t v) {for (int i = 0; i < 4; ++i) p[i] = std::uint8_t(v >> (8 * i));}
std::uint32_t get32(const std::uint8_t* p) {return std::uint32_t(p[0]) | std::uint32_t(p[1]) << 8 | std::uint32_t(p[2]) << 16 | std::uint32_t(p[3]) << 24;}
std::uint32_t fnv(const std::string& text) {
    std::uint32_t h = 2166136261u;
    for (unsigned char c : text) h = (h ^ c) * 16777619u;
    return h;
}
}

std::string Link::text(const Candidate& c) {
    if (c.relayed) return "the room's relay";
    char buffer[INET_ADDRSTRLEN] = "?";
    in_addr ip{};
    ip.s_addr = c.ip;
    ::inet_ntop(AF_INET, &ip, buffer, sizeof buffer);
    return std::string(buffer) + ":" + std::to_string(c.port);
}

std::uint32_t Link::now() {
    return static_cast<std::uint32_t>(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now().time_since_epoch()).count());
}

Link::~Link() {close();}

bool Link::open(std::uint16_t wanted) {
    close();
    socket_ = ::socket(AF_INET, SOCK_DGRAM, 0);
    if (socket_ < 0) return false;
    ::fcntl(socket_, F_SETFL, ::fcntl(socket_, F_GETFL, 0) | O_NONBLOCK);
    // (2026-11-14: on a home Wi-Fi the packets of a LAN match went missing for whole seconds; marked as voice (DSCP EF), the
    // phone's Wi-Fi and the router put them in their first queue (WMM voice), as games and calls do)
    const int tos = 0xB8, priority = 6;
    ::setsockopt(socket_, IPPROTO_IP, IP_TOS, &tos, sizeof tos);
    ::setsockopt(socket_, SOL_SOCKET, SO_PRIORITY, &priority, sizeof priority);
    sockaddr_in any{};
    any.sin_family = AF_INET;
    any.sin_addr.s_addr = htonl(INADDR_ANY);
    bool bound = false, fell = false;
    if (wanted) {
        const int yes = 1;
        ::setsockopt(socket_, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof yes);
        any.sin_port = htons(wanted);
        bound = ::bind(socket_, reinterpret_cast<sockaddr*>(&any), sizeof any) == 0;
        if (!bound) {fell = true; any.sin_port = 0;}
    }
    if (!bound && ::bind(socket_, reinterpret_cast<sockaddr*>(&any), sizeof any) != 0) {close(); return false;}
    socklen_t length = sizeof any;
    ::getsockname(socket_, reinterpret_cast<sockaddr*>(&any), &length);
    port_ = ntohs(any.sin_port);
    // this phone's addresses on its networks (not the loopback), each with the socket's port: Wi-Fi and cable first; the carrier's
    // translation address (192.0.0.x) and link-local ones are of no use to anybody else and are left out
    local_.clear();
    ifaddrs* list = nullptr;
    if (::getifaddrs(&list) == 0) {
        std::vector<std::string> home, other;
        for (ifaddrs* a = list; a; a = a->ifa_next) {
            if (!a->ifa_addr || a->ifa_addr->sa_family != AF_INET || !(a->ifa_flags & IFF_UP) || (a->ifa_flags & IFF_LOOPBACK)) continue;
            const auto* in = reinterpret_cast<sockaddr_in*>(a->ifa_addr);
            const std::uint32_t ip = ntohl(in->sin_addr.s_addr);
            if ((ip >> 24) == 0 || (ip >> 16) == 0xA9FE || (ip >> 8) == 0xC00000) continue;   // 0.x, 169.254.x.x, 192.0.0.x
            char text[INET_ADDRSTRLEN];
            if (!::inet_ntop(AF_INET, &in->sin_addr, text, sizeof text)) continue;
            const std::string name = a->ifa_name ? a->ifa_name : "";
            const bool lan = name.rfind("wlan", 0) == 0 || name.rfind("swlan", 0) == 0 || name.rfind("eth", 0) == 0 || name.rfind("ap", 0) == 0;
            (lan ? home : other).push_back(std::string(text) + ":" + std::to_string(port_));
        }
        ::freeifaddrs(list);
        for (const auto* group : {&home, &other})
            for (const auto& a : *group) if (local_.size() < 3) local_.push_back(a);
    }
    state_ = Open;
    rtt_ = -1;
    peerLeft_ = false;
    relayNoted_ = false;
    sent_ = got_ = foreign_ = 0;
    notes_.clear();
    inbox_.clear();
    remote_ = Candidate{};
    if (fell) note("online: port " + std::to_string(wanted) + " is not free, " + std::to_string(port_) + " instead (the other side's router knows the old one)");
    openedAt_ = now();
    stun_.begin();
    std::string mine;
    for (const auto& a : local_) mine += " " + a;
    note("online: socket open on port " + std::to_string(port_) + ", own addresses:" + (mine.empty() ? " none" : mine));
    return true;
}

void Link::close() {
    stun_.reset();
    if (socket_ >= 0) ::close(socket_);
    socket_ = -1;
    state_ = Closed;
    candidates_.clear();
    local_.clear();
    rtt_ = -1;
}

std::vector<std::string> Link::localAddresses() const {
    std::vector<std::string> out = stun_.mapped();
    for (const auto& a : local_) out.push_back(a);
    return out;
}

void Link::start(const std::string& code, const std::vector<std::string>& addresses, const std::string& token) {
    if (state_ != Open) return;
    session_ = token.empty() ? fnv(code) : fnv(code + ":" + token);
    candidates_.clear();
    for (const auto& a : addresses) {
        const auto at = a.rfind(':');
        if (at == std::string::npos) continue;
        in_addr ip{};
        const int port = std::atoi(a.c_str() + at + 1);
        if (port <= 0 || port > 65535 || ::inet_pton(AF_INET, a.substr(0, at).c_str(), &ip) != 1) continue;
        bool have = false;
        for (const auto& c : candidates_) have = have || (c.ip == ip.s_addr && c.port == port);
        if (!have) candidates_.push_back({ip.s_addr, std::uint16_t(port)});
    }
    started_ = now();
    heard_ = started_;
    lastHello_ = started_ - hello_every;
    lastPing_ = 0;
    number_ = 0;
    lastNote_ = started_;
    const bool relay = static_cast<bool>(channel_.send);
    state_ = candidates_.empty() && !relay ? Failed : Linking;
    std::string to;
    for (const auto& c : candidates_) to += " " + text(c);
    note("online: linking, room " + code + ", " + std::to_string(candidates_.size()) + " address(es) to say hello to:" + (to.empty() ? " none" : to) +
         (relay ? "; the room's relay after " + std::to_string(relayAfter_) + " ms" : "; no relay"));
    if (state_ == Failed) note("online: FAILED: the other player gave no usable address and there is no relay");
}

void Link::send(std::uint8_t type, std::uint32_t number, std::uint32_t echo, const Candidate& to, const std::uint8_t* payload, std::size_t size) {
    if (size > max_payload) return;
    if (testLoss_ > 0) {
        lossState_ ^= lossState_ << 13; lossState_ ^= lossState_ >> 17; lossState_ ^= lossState_ << 5;
        if (int(lossState_ % 100) < testLoss_) return;
    }
    std::uint8_t p[packet_size + max_payload] = {};
    put32(p, magic);
    p[4] = version;
    p[5] = type;
    put32(p + 8, session_);
    put32(p + 12, number);
    put32(p + 16, echo);
    if (size) std::memcpy(p + packet_size, payload, size);
    const std::size_t total = packet_size + size;
    ++sent_;
    if (to.relayed) {
        if (channel_.send) channel_.send(p, total);
        return;
    }
    if (socket_ < 0 || testNoDirect_) return;
    sockaddr_in a{};
    a.sin_family = AF_INET;
    a.sin_addr.s_addr = to.ip;
    a.sin_port = htons(to.port);
    if (::sendto(socket_, p, total, 0, reinterpret_cast<sockaddr*>(&a), sizeof a) < 0) {++sendFailed_; lastSendErrno_ = errno;}
}

bool Link::peerAddress(std::string& out) const {
    if (state_ != Up) return false;
    out = text(remote_);
    return true;
}

bool Link::sendMessage(std::uint8_t type, const std::uint8_t* payload, std::size_t size) {
    if (state_ != Up || type < first_message_type || size > max_payload) return false;
    send(type, ++number_, 0, remote_, payload, size);
    return true;
}

bool Link::popMessage(Message& out) {
    if (inbox_.empty()) return false;
    out = std::move(inbox_.front());
    inbox_.pop_front();
    return true;
}

int Link::silentMs() const {return state_ == Up ? static_cast<int>(now() - heard_) : 0;}

void Link::sayBye() {
    if (state_ != Up) return;
    for (int i = 0; i < 3; ++i) send(bye, ++number_, 0, remote_);   // (three, in case one is lost)
}

// One packet that came, from the socket or from the relay.
void Link::take(const std::uint8_t* p, std::size_t got, const Candidate& source, std::uint32_t t) {
    if (state_ != Linking && state_ != Up) return;
    if (got < packet_size || get32(p) != magic || p[4] != version || get32(p + 8) != session_) {
        ++foreign_;
        lastForeign_ = source;
        if (got >= packet_size && get32(p) == magic && p[4] != version && get32(p + 8) == session_ && foreign_ < 4)
            note("online: the other game speaks network version " + std::to_string(p[4]) + ", this one " + std::to_string(version));
        return;
    }
    // Once UP, bind strictly to the authenticated peer endpoint (blocks off-path UDP injection/spoofing).
    if (state_ == Up && (source.relayed != remote_.relayed || source.ip != remote_.ip || source.port != remote_.port)) {
        ++foreign_;
        lastForeign_ = source;
        return;
    }
    ++got_;
    heard_ = t;
    const State before = state_;
    const std::uint32_t echo = get32(p + 16);
    const auto upVia = [&] {if (state_ == Linking) {state_ = Up; remote_ = source;}};
    switch (p[5]) {
    case hello:
        send(hello_ack, get32(p + 12), echo, source);
        upVia();
        break;
    case hello_ack:
        upVia();
        break;
    case ping:
        send(pong, get32(p + 12), echo, source);
        upVia();                                         // (the other side is already up and has heard us)
        break;
    case pong: {
        upVia();
        const int sample = static_cast<int>(t - echo);
        if (sample >= 0 && sample < 10000) rtt_ = rtt_ < 0 ? sample : (rtt_ * 3 + sample) / 4;
        break;
    }
    case bye:
        peerLeft_ = true;
        break;
    default:
        if (p[5] >= first_message_type && got <= packet_size + max_payload) {
            upVia();                                     // (the other side is already talking: it has heard us)
            if (inbox_.size() < 256) inbox_.push_back({p[5], std::vector<std::uint8_t>(p + packet_size, p + got)});
        }
        break;
    }
    if (before == Linking && state_ == Up) note("online: UP via " + text(remote_) + " after " + std::to_string(t - started_) + " ms, " + std::to_string(sent_) + " sent");
}

void Link::poll() {
    if (socket_ >= 0) stun_.poll(socket_);
    if (socket_ < 0 || state_ == Closed) return;
    const std::uint32_t t = now();
    const bool relayDue = channel_.send && t - started_ >= relayAfter_;
    if (state_ == Linking) {
        if (t - started_ > giveUp_) {
            state_ = Failed;
            note("online: FAILED after " + std::to_string((t - started_) / 1000) + " s: " + std::to_string(sent_) + " packets sent, " + std::to_string(got_) +
                 " came back from the other game, " + std::to_string(foreign_) + " arrived that were not for this match" +
                 (foreign_ ? " (last from " + text(lastForeign_) + ")" : "") + (got_ == 0 ? "; nothing reached this phone: the other side's router or ours blocks it" : ""));
            return;
        }
        if (t - lastNote_ >= 3000) {
            lastNote_ = t;
            note("online: still linking (" + std::to_string((t - started_) / 1000) + " s): " + std::to_string(sent_) + " sent, " + std::to_string(got_) + " answered, " + std::to_string(foreign_) + " foreign");
        }
        if (relayDue && !relayNoted_) {
            relayNoted_ = true;
            note("online: no direct answer in " + std::to_string(relayAfter_) + " ms: the hellos go through the room's relay too");
        }
        if (t - lastHello_ >= hello_every) {
            lastHello_ = t;
            for (const auto& c : candidates_) send(hello, ++number_, t, c); // (to every address: the one that works answers)
            if (relayDue) send(hello, ++number_, t, Candidate{0, 0, true});
        }
    } else if (state_ == Up && t - lastPing_ >= (remote_.relayed ? 2000u : ping_every)) {
        lastPing_ = t;
        send(ping, ++number_, t, remote_);
    }
    // what came: at most a few dozen packets a call, from the socket, then from the relay
    for (int n = 0; n < 64; ++n) {
        std::uint8_t buffer[1600];
        sockaddr_in from{};
        socklen_t length = sizeof from;
        const ssize_t got = ::recvfrom(socket_, buffer, sizeof buffer, 0, reinterpret_cast<sockaddr*>(&from), &length);
        if (got < 0) break;
        if (stun_.handle(buffer, int(got), from.sin_addr.s_addr, from.sin_port)) continue;
        if (testNoDirect_) continue;
        take(buffer, std::size_t(got), Candidate{from.sin_addr.s_addr, ntohs(from.sin_port)}, t);
    }
    if (channel_.receive) {
        for (int n = 0; n < 64; ++n) {
            std::uint8_t buffer[1600];
            const std::size_t got = channel_.receive(buffer, sizeof buffer);
            if (!got) break;
            take(buffer, got, Candidate{0, 0, true}, t);
        }
    }
}
