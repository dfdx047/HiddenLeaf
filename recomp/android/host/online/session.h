#pragma once
#include <cstddef>
#include <cstdint>
#include "net_version.h"

// The conversation two games have between "the line is up" and "the lockstep begins", from Dragon Rage's BT2 online plugin
// (bt2/plugin/online/session.h; BT3: T_CONFIG, T_CONFIG_ACK, T_ARRIVED in net.c), unchanged but for its version:
//   1. the host offers the match's settings (the input delay, whether the host is player 1, a seed, the version of what goes over the
//      line) and repeats them until the other side confirms;
//   2. each game says "arrived" (in Hidden Leaf at its engine's first vsync, field 0: both start from a fresh start of the engine and
//      play every field in step from there) until it has heard the other's;
//   3. when both have arrived and the settings are known, the lockstep (lockstep.h) begins at that field on both sides.
// Nothing here knows the network or the game: `next` gives the messages to send (the match sends them as the line's message types
// config / config_ack / arrived), `onMessage` takes the ones that came. Tested on a computer with lost, repeated and damaged messages.
class SessionControl {
public:
    enum Type : std::uint8_t {tConfig = 6, tConfigAck = 7, tArrived = 8};   // the line's message types (link.h)
    static constexpr std::uint32_t version = HL_NET_VERSION;             // what goes over the line: a game with another one does not start a match
    struct Config {std::uint8_t delay = 3; bool hostIsP1 = true; std::uint32_t seed = 0;};

    // The host offers `offer`; the other side takes what it is given.
    void begin(bool host, const Config& offer);
    bool begun() const {return begun_;}
    bool isHost() const {return host_;}

    bool configKnown() const {return host_ ? acked_ : haveConfig_;}      // the host: the other confirmed it; the other: it came
    const Config& config() const {return cfg_;}
    bool versionBad() const {return versionBad_;}                       // the other side speaks another version

    // This game is ready at `field` (its own count of fields since the engine's start: 0 in Hidden Leaf).
    void setArrived(std::uint32_t field);
    bool met() const {return configKnown() && arrived_ && peerArrived_;}
    std::uint32_t myField() const {return myField_;}
    std::uint32_t peerField() const {return peerField_;}
    bool fieldsDiffer() const {return met() && peerField_ && myField_ != peerField_;}  // the two games did not arrive at the same field: their states may differ
    // An input packet of the lockstep came: the other side is past the meeting.
    void peerIsRunning() {if (arrived_) peerArrived_ = true; if (host_) acked_ = true;}   // (it only runs with the settings in hand)

    // The next message to send now, or -1. Returns the payload length; `type` is the message type. Call until it returns -1.
    int next(std::uint32_t nowMs, std::uint8_t& type, std::uint8_t* out, std::size_t cap);
    // A message that came; false: not one of this conversation or damaged (nothing taken).
    bool onMessage(std::uint8_t type, const std::uint8_t* payload, std::size_t size);

private:
    static constexpr std::uint32_t every = 100;          // ms between two repeats
    static std::uint32_t sum(const std::uint8_t* p, std::size_t n);
    bool begun_ = false, host_ = false, acked_ = false, haveConfig_ = false, versionBad_ = false;
    bool arrived_ = false, peerArrived_ = false, ackDue_ = false, replyDue_ = false;
    Config cfg_;
    std::uint32_t myField_ = 0, peerField_ = 0, lastConfig_ = 0, lastArrived_ = 0;
    bool sentConfig_ = false, sentArrived_ = false;
};
