#pragma once
#include <cstdint>
#include <deque>
#include <functional>
#include <string>
#include <vector>
#include "stun.h"

// The line between the two games of an online match (UDP), from Dragon Rage's BT2 online plugin (bt2/plugin/online/link.h). The rooms
// service (the app talks to it) only tells each side the other's name and addresses; this is what makes sure the other game is really
// there and how long a packet takes.
//
// Both sides know the other's addresses ("ip:port") from the room. Each sends a HELLO to every address at the same time, over and over:
// a router that only lets in what the home sent out lets the other side's HELLO in after its own was sent (the "hole punch"). The first
// HELLO or HELLO_ACK that comes back from the other game, with the room's session number, makes the line UP; after that a PING goes
// every half second and its PONG gives the round trip. Without an answer in `giveUp` milliseconds the line has FAILED.
//
// The relay: when the two phones cannot reach each other directly (a strict router, a mobile network), the room's relay passes the same
// packets on. In Hidden Leaf it is a WebSocket to the rooms service that the app keeps (engine/OnlineGame.kt); this class only sees a
// channel with a send and a receive (`setChannel`). After `relayAfterMs` without an answer, the hellos also go through the channel, and
// whichever way answers first is the line. BT2's TURN client is not here.
//
// A packet is 20 bytes, little-endian: magic "HLN5", the network version (net_version.h), type, 2 reserved, the session (a hash of the
// room's code), a number, and the sender's clock in milliseconds (echoed back by a PONG); then the message, if any.
class Link {
public:
    enum State {Closed, Open, Linking, Up, Failed};

    // The relay's channel: `send` hands one packet to the relay (false: it is not connected), `receive` gives the next packet that came
    // from it into `out` (its size, or 0 when there is none). Both are called on the game's thread and must not wait.
    struct Channel {
        std::function<bool(const std::uint8_t*, std::size_t)> send;
        std::function<std::size_t(std::uint8_t*, std::size_t)> receive;
    };

    ~Link();
    // The socket; `wanted` is the port to take if it is free (the app's lobby used that port, and the other side's router only knows
    // that one), else any port. False: no socket could be made.
    bool open(std::uint16_t wanted = 0);
    void close();
    // This game's addresses: the one the router shows to the internet (from STUN, stun.h), then the home's own. For the log.
    std::vector<std::string> localAddresses() const;
    bool symmetricRouter() const {return stun_.symmetric();}
    void setChannel(Channel channel) {channel_ = std::move(channel);}
    void setRelayAfterMs(std::uint32_t ms) {relayAfter_ = ms;}
    // Starts the exchange with the other game: `code` names the session, `addresses` are the other side's "ip:port" strings.
    void start(const std::string& code, const std::vector<std::string>& addresses);
    void poll();                                         // as often as possible: sends what is due, reads what came
    State state() const {return state_;}
    int roundTripMs() const {return rtt_;}               // smoothed, milliseconds; -1 before the first PONG
    bool peerAddress(std::string& out) const;            // where the other game answered from, once UP
    bool peerLeft() const {return peerLeft_;}            // the other game said goodbye (a BYE packet)
    int silentMs() const;                                // milliseconds since anything came from the other game (while UP)
    void sayBye();                                       // tells the other game this one is leaving
    bool viaRelay() const {return remote_.relayed;}      // the line goes through the room's relay
    // Messages of the game's own conversation (session.h, lockstep.h) travel on the same line, once it is UP: a type from 6 up and a payload.
    // Like everything here they may be lost, repeated or late: whoever sends them repeats them as long as it needs.
    struct Message {std::uint8_t type = 0; std::vector<std::uint8_t> payload;};
    static constexpr std::uint8_t first_message_type = 6;
    static constexpr std::size_t max_payload = 1200;
    bool sendMessage(std::uint8_t type, const std::uint8_t* payload, std::size_t size);   // false: the line is not UP, or the message is too big
    bool popMessage(Message& out);                       // the oldest message that came, if any
    std::uint16_t port() const {return port_;}           // the socket's port on this phone
    void setGiveUpMs(std::uint32_t ms) {giveUp_ = ms;}   // how long the hellos go on without an answer
    void setTestLoss(int percent) {testLoss_ = percent;} // tests: that share of what is sent is dropped
    void setTestNoDirect(bool on) {testNoDirect_ = on;}  // tests: nothing goes out of the socket (only the channel works)
    // What happened on the line, in words, for the game's log (taken each frame): the addresses, the hellos sent and what came back,
    // why it failed. Without this a failed match leaves nothing to tell a blocked router from a wrong address.
    std::vector<std::string> takeNotes() {auto out = std::move(notes_); notes_.clear(); return out;}

private:
    struct Candidate {std::uint32_t ip = 0; std::uint16_t port = 0; bool relayed = false;};
    void send(std::uint8_t type, std::uint32_t number, std::uint32_t echo, const Candidate& to, const std::uint8_t* payload = nullptr, std::size_t size = 0);
    void take(const std::uint8_t* p, std::size_t got, const Candidate& source, std::uint32_t t);
    static std::uint32_t now();

    void note(const std::string& text) {if (notes_.size() < 64) notes_.push_back(text);}
    static std::string text(const Candidate& c);

    int socket_ = -1;
    std::vector<std::string> notes_;
    std::deque<Message> inbox_;
    Channel channel_;
    std::uint32_t relayAfter_ = 3000;
    bool relayNoted_ = false;
    int testLoss_ = 0;
    bool testNoDirect_ = false;
    std::uint32_t giveUp_ = 15000;
    std::uint32_t lossState_ = 2463534242u;
    std::uint32_t sent_ = 0, got_ = 0, foreign_ = 0, lastNote_ = 0;
    Candidate lastForeign_;
    State state_ = Closed;
    std::uint16_t port_ = 0;
    std::uint32_t session_ = 0, number_ = 0, started_ = 0, lastHello_ = 0, lastPing_ = 0;
    std::vector<std::string> local_;
    std::vector<Candidate> candidates_;
    Candidate remote_;
    int rtt_ = -1;
    bool peerLeft_ = false;
    std::uint32_t heard_ = 0;
    Stun stun_;
    std::uint32_t openedAt_ = 0;
};
