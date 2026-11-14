#pragma once
#include "link.h"
#include "lockstep.h"
#include "session.h"
#include <atomic>
#include <cstdint>
#include <functional>
#include <string>
#include <utility>
#include <vector>

// The engine's side of an online match, from Dragon Rage's BT2 online plugin (bt2/plugin/online/fight_session.h). The app's Online
// screen (android/.../ui/screens/OnlineScreen.kt) finds the other player through the rooms service, writes what it learned to the
// session file and starts the engine afresh. In that fresh process this class makes the line to the other game (on the port the
// app's lobby used, so the other side's router still knows it), tells the other side the match's settings, and from the engine's
// first vsync runs the lockstep (lockstep.h): once per vsync, from the vsync hook (hl_online.cpp), with both players' pads.
//
// Unlike BT2, nothing walks the menus alone: both games run every field in step from the very first vsync, so the two engines go
// through the same boot, the same menus and the same fights with the same two pads, and player 1 drives the menus. That is what
// docs/online.md measured (the same route of buttons by vsync gives the same game on two phones), and it needs no knowledge of the
// game's menus.
//
// It runs on the game's own thread: `padField` is called once per vsync and may wait inside (that is how the game waits for the
// other player: its time stands still). Nothing here knows the game; the glue gives it the memory for the state check.
struct SessionFile {
    bool host = false;                          // created the room
    std::uint16_t port = 0;                     // the UDP port of the app's lobby socket
    std::string code, myName, peerName;
    std::vector<std::string> addresses;         // where the other game may be reached ("ip:port")
    std::int64_t at = 0;                        // when the app wrote it (seconds since 1970): an old file is not a match to start
    unsigned delayFrames = 0;                   // the host's choice: 0 automatic, else 1 to 4 of the game's frames (two fields each)
    bool relay = false;                         // the app keeps the room's relay open for this match (engine/OnlineGame.kt)
    unsigned version = 0;                       // the app's network version: must be HL_NET_VERSION
};
bool parseSessionFile(const std::string& text, SessionFile& out);
std::string formatSessionFile(const SessionFile& file);

class MatchSession {
public:
    struct Env {
        std::function<void(const std::string&)> log;
        const std::uint8_t* ram = nullptr;      // the game's memory, for the state check (null: none)
        std::size_t ramSize = 0;
        std::vector<std::pair<std::uint32_t, std::uint32_t>> skip;   // [from, to) of the memory left out of the check (docs/online.md)
        std::function<void()> onMet;            // both games are there: the lockstep begins
        std::function<void(const std::string& why)> onEnded;   // the match is over (once)
        std::function<void()> onWait;           // called now and then while the game waits (the glue refreshes what the app shows)
        std::function<std::uint32_t()> nowMs;   // (tests: another clock)
        std::function<void(unsigned)> sleepMs;  // (tests: another wait)
        Link::Channel channel;                  // the room's relay, when the app keeps one
        bool strictDesync = false;              // a difference of the state checks ends the match (else it is only said)
        unsigned meetTimeoutMs = 90000;         // how long the first game waits for the other's engine
        unsigned silentRunningMs = 30000;       // how long the other game may say nothing during the match (a long load on its side)
    };
    enum class Phase {Meeting, Running, Ended};

    bool begin(const SessionFile& file, Env env);
    bool active() const {return active_;}
    // The vsync hook's call: `local` is this phone's pad; `p1` and `p2` receive the two players' pads for this field. 1 while the match
    // goes on; 0 when it is over (the game then goes on with the phone's own pad).
    int padField(const std::uint8_t local[6], std::uint8_t p1[6], std::uint8_t p2[6]);
    Phase phase() const {return phase_;}
    bool running() const {return phase_ == Phase::Running;}
    bool ended() const {return phase_ == Phase::Ended;}
    const std::string& endReason() const {return reason_;}
    const std::string& myName() const {return file_.myName;}
    const std::string& peerName() const {return file_.peerName;}
    bool iAmPlayer1() const {return p1_;}
    unsigned delay() const {return cfg_.delay;}          // fields
    std::uint32_t field() const {return field_;}         // lockstep fields run
    int roundTripMs() const {return link_.roundTripMs();}
    bool viaRelay() const {return link_.state() == Link::Up && link_.viaRelay();}
    bool linked() const {return link_.state() == Link::Up;}
    unsigned waits() const {return waits_;}              // fields the game had to wait for the other's pad
    unsigned desyncs() const {return desyncs_;}
    std::uint32_t desyncField() const {return lock_.desyncField();}
    void leave(const std::string& why);                  // this side ends it (says goodbye); on the game's thread
    void requestLeave() {leaveRequested_.store(true, std::memory_order_release);}   // from any thread: the next field ends the match
    Link& link() {return link_;}                         // (tests)

    // The state check's value at `field`: one 64-byte block in eight of `ram` (a different eighth each check, so every byte is looked
    // at within eight checks), without the blocks in `skip`.
    static std::uint64_t digestOf(const std::uint8_t* ram, std::size_t size, const std::vector<std::pair<std::uint32_t, std::uint32_t>>& skip, std::uint32_t field);

private:
    void pump();
    void sendInputs();
    void end(const std::string& why, bool said);
    const char* lineBad() const;
    std::uint32_t now() const {return env_.nowMs ? env_.nowMs() : defaultNow();}
    void sleep(unsigned ms) const;
    static std::uint32_t defaultNow();
    void log(const std::string& text) const {if (env_.log) env_.log(text);}
    void waited();

    SessionFile file_;
    Env env_;
    Link link_;
    SessionControl ctl_;
    Lockstep lock_;
    SessionControl::Config cfg_;
    bool active_ = false, p1_ = false, ctlBegun_ = false, desyncSaid_ = false;
    std::atomic<bool> leaveRequested_{false};
    std::atomic<Phase> phase_{Phase::Meeting};
    std::string reason_;
    std::uint32_t field_ = 0;
    std::uint32_t upSince_ = 0, lastSend_ = 0, lastWaitCall_ = 0, lastField_ = 0;
    unsigned waits_ = 0, desyncs_ = 0, mismatchesSaid_ = 0;
    std::uint32_t mismatchSaidAt_ = 0;
};
