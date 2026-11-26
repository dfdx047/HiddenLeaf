// Hidden Leaf: the online match in the engine. The app's Online screen (android/.../ui/screens/OnlineScreen.kt) finds the other player
// through the rooms service (online/worker), writes <root>/online/session.txt and starts the game; this file:
//   - reads the session file once (and renames it, so the next start is an ordinary one);
//   - makes the two games start alike: a fresh memory card of their own (<root>/online/saves), the same clock (HL_FIXED_CLOCK in the
//     runtime's sceCdReadClock, patch 0097, and TZ=UTC for the system's time zone), every character open, the one-button special
//     and the tap assist off (they change the game's pad in memory: both phones must do the same); the camera is each phone's own
//     (only the projection reads it: un5_main.cpp applyVideo);
//   - from the first vsync runs the match (match_session.h) once per vsync, with this phone's pad, and hands the two players' pads to
//     the game's two pad ports (the pad hook in the runtime's Pad.cpp, patch 0097): player 1, the room's creator, on the first port,
//     player 2 on the second. Before the other game is there the first vsync waits (the screen stays black);
//   - checks now and then that the two games' memories are the same, leaving out the pages docs/online.md found to differ between two
//     phones without the game going apart (the clock, two lists, the video structure); a difference is logged, not fatal, unless
//     HL_ONLINE_STRICT=1;
//   - tells the app what is going on (OnlineBridge.nativeStatus) and carries the room's relay: the app keeps its WebSocket and the
//     packets go through two queues (nativeRelayIn, nativeRelayOut).
//
// Switches in hl_env.txt (testing): HL_ONLINE_SKIP="406000-407000,..." more ranges left out of the check (hex), HL_ONLINE_STRICT=1,
// HL_ONLINE_DUMP=N the game's memory at lockstep field N into <root>/ramdumps/online_<host|guest>_<N>.bin (the same field on both
// phones: compare them with tools/ramdiff.py or cmp).
#include "hl_online.h"
#include "match_session.h"
#include <array>
#include <map>
#include <thread>
#include "net_version.h"
#include "../hl_game.h"
#include "../hl_host_pad.h"

#include "ps2_runtime.h"

void hl_spu2_mute(bool muted); // (spu2/hl_spu2.cpp)

#include <jni.h>
#include <sys/stat.h>

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <deque>
#include <filesystem>
#include <fstream>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

// the runtime's Pad.cpp (patch 0097): the online match's pads, port by port
extern bool (*g_hlPadPortHook)(int port, uint16_t *buttons, uint8_t *lx, uint8_t *ly, uint8_t *rx, uint8_t *ry);

namespace
{
    constexpr std::size_t kRamSize = 32u << 20;
    // 2026-01-01 12:00:00 UTC: the clock both games read (any moment does, as long as it is the same)
    constexpr const char *kFixedClock = "1767268800";
    // a session file older than this is not a match to start (the app wrote it a moment before starting the game)
    constexpr long long kSessionMaxAge = 180;

    bool g_active = false;
    bool g_trainingFast = false;
    std::atomic<uint32_t> g_trainingTick{0};
    constexpr uint32_t kTrainingEnd = 4660u; // right on Free Battle 1P vs CPU Character Select (before P2 START at 4700)
    std::string g_root;
    SessionFile g_file;
    MatchSession g_match;
    bool g_begun = false, g_failed = false;
    void (*g_prevVsync)(PS2Runtime *, uint64_t) = nullptr;

    // the two players' pads of the field the game is in: buttons (16 bits) | lx << 16 | ly << 24 | rx << 32 | ry << 40
    std::atomic<uint64_t> g_pads[2];
    std::atomic<bool> g_padsOn{false};

    // what the app shows (nativeStatus): written on the game's thread, read on the app's
    std::mutex g_statusMutex;
    std::string g_status = "phase=starting";

    // the room's relay: packets from the app's WebSocket (in) and to it (out)
    std::mutex g_relayMutex;
    std::condition_variable g_relayOutReady;
    std::deque<std::vector<uint8_t>> g_relayIn, g_relayOut;
    constexpr std::size_t kRelayQueue = 256;

    uint64_t pack(const uint8_t s[6])
    {
        return uint64_t(s[0]) | uint64_t(s[1]) << 8 | uint64_t(s[2]) << 16 | uint64_t(s[3]) << 24 | uint64_t(s[4]) << 32 | uint64_t(s[5]) << 40;
    }

    bool padPortHook(int port, uint16_t *buttons, uint8_t *lx, uint8_t *ly, uint8_t *rx, uint8_t *ry)
    {
        if (!g_padsOn.load(std::memory_order_acquire) || port < 0 || port > 1)
            return false;
        const uint64_t v = g_pads[port].load(std::memory_order_acquire);
        *buttons = static_cast<uint16_t>(v & 0xFFFFu);
        *lx = static_cast<uint8_t>(v >> 16);
        *ly = static_cast<uint8_t>(v >> 24);
        *rx = static_cast<uint8_t>(v >> 32);
        *ry = static_cast<uint8_t>(v >> 40);
        return true;
    }

    void log(const std::string &text) { std::fprintf(stderr, "%s\n", text.c_str()); }

    // phase: starting (before the first vsync), linking (waiting for the other game), playing, over, failed
    void publish(const char *phase)
    {
        std::ostringstream s;
        s << "phase=" << phase << ";me=" << g_file.myName << ";peer=" << g_file.peerName << ";host=" << (g_file.host ? 1 : 0)
          << ";p1=" << (g_match.iAmPlayer1() ? 1 : 0) << ";rtt=" << g_match.roundTripMs() << ";delay=" << g_match.delay()
          << ";field=" << g_match.field() << ";waits=" << g_match.waits() << ";desyncs=" << g_match.desyncs()
          << ";relay=" << (g_match.viaRelay() ? 1 : 0) << ";want_relay=" << ((g_match.wantsRelay() || g_file.broadcastSpec || g_file.spectator) ? 1 : 0)
          << ";linked=" << (g_match.linked() ? 1 : 0) << ";input_mode=" << g_file.inputMode
          << ";reason=" << (g_match.ended() ? g_match.endReason() : std::string());
        std::lock_guard<std::mutex> lock(g_statusMutex);
        g_status = s.str();
    }

    void publishNow()
    {
        if (g_failed)
            publish("failed");
        else if (g_match.ended())
            publish("over");
        else if (g_match.running())
            publish("playing");
        else
            publish("linking");
    }

    // [from, to) ranges in hex, "406000-407000,632000-633000"
    void readRanges(const char *text, std::vector<std::pair<uint32_t, uint32_t>> &out)
    {
        for (const char *p = text; p && *p;)
        {
            char *e = nullptr;
            const unsigned long from = std::strtoul(p, &e, 16);
            if (e == p || *e != '-')
                break;
            const char *q = e + 1;
            const unsigned long to = std::strtoul(q, &e, 16);
            if (e == q)
                break;
            if (to > from)
                out.emplace_back(static_cast<uint32_t>(from), static_cast<uint32_t>(to));
            p = *e == ',' ? e + 1 : e;
        }
    }

    // (the way into the match, further down)
    extern bool g_entry;
    uint32_t entryEnd();
    void entryPads(uint32_t field, uint8_t p1[6], uint8_t p2[6]);

    void begin(PS2Runtime *runtime)
    {
        g_begun = true;
        MatchSession::Env env;
        env.log = log;
        if (g_entry)
        {   // (the developer, 2026-11-14: from the game's online menu the players must land on the character select at once, not
            // watch the boot's menus go by: each phone runs the way in alone, fast and unheard, and the two meet at its end)
            env.localUntil = entryEnd();
            env.localPads = entryPads;
        }
        env.ram = runtime->memory().getRDRAM();
        env.ramSize = kRamSize;
        // docs/online.md: between two phones (and between two speeds of one) these differ with the game still the same: the clock
        // (0x632241), two lists whose blocks change order (0x406000, 0x40D000) and the video structure that follows the picture's
        // settings (0x938000)
        env.skip = {{0x406000u, 0x407000u}, {0x40D000u, 0x40E000u}, {0x632000u, 0x633000u}, {0x938000u, 0x939000u},
                    // ... and between two phones (Cube × Honor, RAM at fields 64 and 9000 of a fight, 2026-11-14), with the same fight on
                    // both screens: the kernel's area (threads' stacks, two buffers each phone is on the other one of), the sound's samples,
                    // 64-bit timestamps and counters that go by the phone's speed, blocks of a list in another order
                    {0x0u, 0x100000u}, {0x633000u, 0x638000u}, {0x650000u, 0x656000u},
                    {0x3E9F00u, 0x3EA100u}, {0x3EDEC0u, 0x3EDF00u}, {0x3F0F40u, 0x3F0FC0u}, {0x3F6180u, 0x3F6400u}, {0x3F7C00u, 0x3F7CC0u},
                    {0x3FD640u, 0x3FD880u}, {0x3FDE80u, 0x3FDF40u}, {0x409440u, 0x409480u}, {0x4099C0u, 0x409CC0u}, {0x40EAC0u, 0x40EB00u},
                    {0x40F300u, 0x40F380u}, {0x410E00u, 0x410E40u}, {0x61A100u, 0x61A140u}, {0x623A80u, 0x623AC0u}, {0x625A80u, 0x625AC0u},
                    {0x62DDC0u, 0x62DE00u}, {0xC67600u, 0xC67640u}, {0xDE8E40u, 0xDE8E80u}, {0xDED180u, 0xDED1C0u}, {0xE973C0u, 0xE97400u},
                    {0xE97BC0u, 0xE97C80u}, {0xE98240u, 0xE98280u}};
        readRanges(std::getenv("HL_ONLINE_SKIP"), env.skip);
        env.strictDesync = std::getenv("HL_ONLINE_STRICT") != nullptr;
        env.onWait = publishNow;
        env.onMet = [] { publish("playing"); };
        env.onEnded = [](const std::string &) {
            g_padsOn.store(false, std::memory_order_release);
            publish("over");
        };
        if (g_file.relay)
        {
            env.channel.send = [](const uint8_t *p, std::size_t n) {
                {
                    std::lock_guard<std::mutex> lock(g_relayMutex);
                    if (g_relayOut.size() >= kRelayQueue)
                        g_relayOut.pop_front(); // (a stuck WebSocket: the oldest gives way, the lockstep says everything again)
                    g_relayOut.emplace_back(p, p + n);
                }
                g_relayOutReady.notify_one();
                return true;
            };
            env.channel.receive = [](uint8_t *out, std::size_t cap) -> std::size_t {
                std::lock_guard<std::mutex> lock(g_relayMutex);
                if (g_relayIn.empty())
                    return 0;
                const std::size_t n = std::min(cap, g_relayIn.front().size());
                std::memcpy(out, g_relayIn.front().data(), n);
                g_relayIn.pop_front();
                return n;
            };
        }
        if (!g_match.begin(g_file, env))
        {
            g_failed = true;
            publish("failed");
            return;
        }
        publish("linking");
    }

    void dumpRam(PS2Runtime *runtime, uint32_t field)
    {
        const std::string dir = g_root + "/ramdumps";
        ::mkdir(dir.c_str(), 0775);
        const std::string name = dir + "/online_" + (g_file.host ? "host_" : "guest_") + std::to_string(field) + ".bin";
        if (FILE *f = std::fopen(name.c_str(), "wb"))
        {
            std::fwrite(runtime->memory().getRDRAM(), 1, kRamSize, f);
            std::fclose(f);
        }
        log("online: the game's memory at field " + std::to_string(field) + " in " + name);
    }

    // The way into the match (the developer, 2026-11-13: both players must land on the character select, not on the language screen):
    // the first vsyncs of the match press the menus' buttons by themselves, the same on both phones because they go through the
    // lockstep like any other input. Player 1 (the host) presses the European disc's route from boot to Free Battle's character
    // select, the one the victory route of build/lab (env_victory.txt) took on every phone: English, the films and the title by
    // START, New Game, Free Battle, its first choices. Player 2 presses START on the select to come in as 2P. Each press is held
    // 6 vsyncs, as the test routes of Pad.cpp do. Until kEntryEnd the phones' own controllers are not read.
    struct EntryStep
    {
        uint32_t at;
        uint16_t buttons;
        bool player1;
    };
    constexpr uint16_t kStart = 1u << 3, kUp = 1u << 4, kDown = 1u << 6, kCross = 1u << 14;
    constexpr EntryStep kEntry[] = {
        {650u, kCross, true},  {2866u, kStart, true}, {3116u, kStart, true}, {3306u, kUp, true},     {3436u, kCross, true},
        {3965u, kDown, true},  {4001u, kCross, true}, {4373u, kCross, true}, {4409u, kCross, true}, {4700u, kStart, false},
    };
    constexpr uint32_t kEntryEnd = 4760u, kEntryHold = 6u;
    bool g_entry = true; // (HL_ONLINE_ENTRY=0: the match starts at boot with the players' own controllers, as before)

    uint16_t entryButtons(uint32_t field, bool player1)
    {
        for (const EntryStep &e : kEntry)
            if (e.player1 == player1 && field >= e.at && field < e.at + kEntryHold)
                return e.buttons;
        return 0u;
    }

    uint32_t entryEnd() { return kEntryEnd; }

    // Both players' pads on the way in, the same on both phones without asking the other (MatchSession::Env::localPads)
    void entryPads(uint32_t field, uint8_t p1[6], uint8_t p2[6])
    {
        const uint16_t a = entryButtons(field, true), b = entryButtons(field, false);
        const uint8_t one[6] = {static_cast<uint8_t>(a), static_cast<uint8_t>(a >> 8), 0x80u, 0x80u, 0x80u, 0x80u};
        const uint8_t two[6] = {static_cast<uint8_t>(b), static_cast<uint8_t>(b >> 8), 0x80u, 0x80u, 0x80u, 0x80u};
        std::memcpy(p1, one, 6);
        std::memcpy(p2, two, 6);
    }

    void onTrainingVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_prevVsync)
            g_prevVsync(runtime, tick);
        const uint32_t f = static_cast<uint32_t>(tick);
        g_trainingTick.store(f, std::memory_order_relaxed);
        if (f < kTrainingEnd)
        {
            const uint16_t a = entryButtons(f, true);
            const uint8_t one[6] = {static_cast<uint8_t>(a), static_cast<uint8_t>(a >> 8), 0x80u, 0x80u, 0x80u, 0x80u};
            const uint8_t rest[6] = {0u, 0u, 0x80u, 0x80u, 0x80u, 0x80u};
            g_pads[0].store(pack(one), std::memory_order_release);
            g_pads[1].store(pack(rest), std::memory_order_release);
            g_padsOn.store(true, std::memory_order_release);
        }
        else if (g_padsOn.load(std::memory_order_relaxed))
        {
            g_padsOn.store(false, std::memory_order_release);
            hl_spu2_mute(false);
        }
    }

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_prevVsync)
            g_prevVsync(runtime, tick);
        if (!g_begun)
            begin(runtime);
        if (g_failed || g_match.ended())
            return;
        static const long long s_dumpAt = std::getenv("HL_ONLINE_DUMP") ? std::atoll(std::getenv("HL_ONLINE_DUMP")) : -1;
        if (s_dumpAt >= 0 && g_match.running() && g_match.field() == static_cast<uint32_t>(s_dumpAt))
            dumpRam(runtime, g_match.field());
        unsigned buttons = g_hlPadButtons.load(std::memory_order_relaxed);
        unsigned axes = g_hlPadAxes.load(std::memory_order_relaxed); // lx | ly << 8 | rx << 16 | ry << 24
        if (g_entry && g_match.running() && g_match.field() < kEntryEnd)
        {
            buttons = entryButtons(g_match.field(), g_match.iAmPlayer1());
            axes = 0x80808080u;
        }
        const bool blockStart = g_file.ranked && g_match.running() && g_match.field() >= kEntryEnd && hl_game::inFightDeterministic();
        if (blockStart)
            buttons &= ~static_cast<unsigned>(kStart);
        const uint8_t local[6] = {static_cast<uint8_t>(buttons), static_cast<uint8_t>(buttons >> 8), static_cast<uint8_t>(axes),
                                  static_cast<uint8_t>(axes >> 8), static_cast<uint8_t>(axes >> 16), static_cast<uint8_t>(axes >> 24)};
        uint8_t p1[6], p2[6];
        if (g_match.padField(local, p1, p2))
        {
            // Ranked anti-grief: deterministically strip START (0x08) for BOTH players during an active fight
            // so neither side can spam-pause the match (works identically on both phones with zero desync).
            if (g_file.ranked && g_match.field() >= kEntryEnd && hl_game::inFightDeterministic())
            {
                p1[0] &= static_cast<uint8_t>(~0x08u);
                p2[0] &= static_cast<uint8_t>(~0x08u);
            }
            g_pads[0].store(pack(p1), std::memory_order_release);
            g_pads[1].store(pack(p2), std::memory_order_release);
            g_padsOn.store(true, std::memory_order_release);
            // Broadcast authoritative [field, p1, p2] to live spectators when hosting a Custom Lobby match
            if (g_file.broadcastSpec && g_match.iAmPlayer1() && g_match.field() >= kEntryEnd)
            {
                const uint32_t f = g_match.field();
                std::vector<uint8_t> sp(20);
                sp[0] = 0x53u; // 'S'
                sp[1] = 0x50u; // 'P'
                sp[2] = 0u;
                sp[3] = 0u;
                sp[4] = static_cast<uint8_t>(f);
                sp[5] = static_cast<uint8_t>(f >> 8);
                sp[6] = static_cast<uint8_t>(f >> 16);
                sp[7] = static_cast<uint8_t>(f >> 24);
                std::memcpy(sp.data() + 8, p1, 6);
                std::memcpy(sp.data() + 14, p2, 6);
                {
                    std::lock_guard<std::mutex> lock(g_relayMutex);
                    if (g_relayOut.size() >= kRelayQueue)
                        g_relayOut.pop_front();
                    g_relayOut.push_back(std::move(sp));
                }
                g_relayOutReady.notify_one();
            }
        }
        else if (g_match.ended())
        {   // (the developer, 2026-11-14: after a drop each phone went on alone, both choosing 1P vs 2P. A match that is over stands
            // still, both pads let go, until the player goes back to the menu from the app's notice)
            const uint8_t still[6] = {0u, 0u, 0x80u, 0x80u, 0x80u, 0x80u};
            g_pads[0].store(pack(still), std::memory_order_release);
            g_pads[1].store(pack(still), std::memory_order_release);
            g_padsOn.store(true, std::memory_order_release);
        }
        else
            g_padsOn.store(false, std::memory_order_release);
        static bool s_heard = false;
        if (!s_heard && (!g_entry || g_match.ended() || g_match.field() >= kEntryEnd))
        {
            s_heard = true;
            hl_spu2_mute(false);
        }
        if (tick % 10u == 0u || g_match.ended())
            publishNow();
    }

    // ---- Live Spectator playback (read-only: replays host's [field, p1, p2] frames in real time)
    std::atomic<bool> g_specQuit{false};
    std::map<uint32_t, std::array<uint8_t, 12>> g_specFrames;
    uint32_t g_specField = 0u;

    void drainSpecPackets()
    {
        std::deque<std::vector<uint8_t>> batch;
        {
            std::lock_guard<std::mutex> lock(g_relayMutex);
            batch.swap(g_relayIn);
        }
        for (const auto &pkt : batch)
        {
            for (std::size_t off = 0; off + 20u <= pkt.size(); off += 20u)
            {
                if (pkt[off] != 0x53u || pkt[off + 1u] != 0x50u)
                    break;
                const uint32_t f = static_cast<uint32_t>(pkt[off + 4u]) |
                                   (static_cast<uint32_t>(pkt[off + 5u]) << 8) |
                                   (static_cast<uint32_t>(pkt[off + 6u]) << 16) |
                                   (static_cast<uint32_t>(pkt[off + 7u]) << 24);
                std::array<uint8_t, 12> pads{};
                std::memcpy(pads.data(), pkt.data() + off + 8u, 12u);
                g_specFrames[f] = pads;
            }
        }
    }

    void publishSpectator(const char *phase, const char *reason = "")
    {
        std::ostringstream s;
        s << "phase=" << phase << ";me=" << g_file.myName << ";peer=" << g_file.peerName
          << ";host=0;p1=0;rtt=0;delay=0;field=" << g_specField
          << ";waits=0;desyncs=0;relay=1;want_relay=1;linked=1;spectator=1;reason=" << reason;
        std::lock_guard<std::mutex> lock(g_statusMutex);
        g_status = s.str();
    }

    void onSpectatorVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_prevVsync)
            g_prevVsync(runtime, tick);
        if (g_specQuit.load(std::memory_order_acquire))
        {
            const uint8_t still[6] = {0u, 0u, 0x80u, 0x80u, 0x80u, 0x80u};
            g_pads[0].store(pack(still), std::memory_order_release);
            g_pads[1].store(pack(still), std::memory_order_release);
            g_padsOn.store(true, std::memory_order_release);
            publishSpectator("over", "the player left");
            return;
        }
        const uint32_t f = g_specField++;
        if (f < kEntryEnd)
        {
            uint8_t p1[6], p2[6];
            entryPads(f, p1, p2);
            g_pads[0].store(pack(p1), std::memory_order_release);
            g_pads[1].store(pack(p2), std::memory_order_release);
            g_padsOn.store(true, std::memory_order_release);
            if (f % 15u == 0u)
                publishSpectator("spectating");
            return;
        }
        static bool s_specHeard = false;
        if (!s_specHeard)
        {
            s_specHeard = true;
            hl_spu2_mute(false);
        }
        // Wait for the host's [field, p1, p2] frame from the spectator WebSocket
        const auto waitStart = std::chrono::steady_clock::now();
        for (;;)
        {
            drainSpecPackets();
            auto it = g_specFrames.find(f);
            if (it != g_specFrames.end())
            {
                g_pads[0].store(pack(it->second.data()), std::memory_order_release);
                g_pads[1].store(pack(it->second.data() + 6), std::memory_order_release);
                g_padsOn.store(true, std::memory_order_release);
                g_specFrames.erase(g_specFrames.begin(), std::next(it));
                break;
            }
            if (g_specQuit.load(std::memory_order_acquire))
            {
                publishSpectator("over", "the player left");
                return;
            }
            const auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - waitStart).count();
            if (elapsedMs > 45000)
            {
                g_specQuit.store(true, std::memory_order_release);
                publishSpectator("over", "the other game stopped answering");
                return;
            }
            if ((elapsedMs % 500) < 10)
                publishSpectator("spectating");
            std::this_thread::sleep_for(std::chrono::milliseconds(4));
        }
        if (f % 10u == 0u)
            publishSpectator("spectating");
    }

    std::string readFile(const std::string &path)
    {
        std::ifstream in(path, std::ios::binary);
        std::ostringstream s;
        s << in.rdbuf();
        return s.str();
    }
}

bool hl_online::active() { return g_active; }

bool hl_online::setup(const std::string &root)
{
    const std::string dir = root + "/online", path = dir + "/session.txt";
    const std::string trainPath = dir + "/training.txt";
    std::error_code error;
    if (!std::filesystem::exists(path, error))
    {
        // Training Mode while queuing: fast-forward boot & menus directly to Free Battle Character Select!
        if (std::filesystem::exists(trainPath, error))
        {
            std::filesystem::remove(trainPath, error);
            g_trainingFast = true;
            g_trainingTick.store(0u, std::memory_order_relaxed);
            if (!std::getenv("HL_FAST_UNTIL"))
                setenv("HL_FAST_UNTIL", std::to_string(kTrainingEnd).c_str(), 1);
            hl_spu2_mute(true);
            const uint8_t rest[6] = {0, 0, 0x80, 0x80, 0x80, 0x80};
            g_pads[0].store(pack(rest));
            g_pads[1].store(pack(rest));
            g_padsOn.store(true);
            g_prevVsync = g_hlVsyncHook;
            g_hlVsyncHook = &onTrainingVsync;
            g_hlPadPortHook = &padPortHook;
            log("online: training fast-entry enabled (skipping straight to Character Select)");
        }
        return false;
    }
    const std::string text = readFile(path);
    std::filesystem::rename(path, dir + "/session.used.txt", error); // (once: the next start is an ordinary one)
    SessionFile file;
    if (!parseSessionFile(text, file))
    {
        log("online: the session file cannot be read; an ordinary start");
        return false;
    }
    const long long age = static_cast<long long>(std::time(nullptr)) - file.at;
    if (age < -60 || age > kSessionMaxAge)
    {
        log("online: the session file is " + std::to_string(age) + " s old; an ordinary start");
        return false;
    }
    if (file.version != HL_NET_VERSION)
    {
        log("online: the app wrote network version " + std::to_string(file.version) + ", the engine is " + std::to_string(HL_NET_VERSION) + "; an ordinary start");
        return false;
    }
    g_root = root;
    g_file = file;
    g_active = true;
    if (const char *e = std::getenv("HL_ONLINE_ENTRY"))
        g_entry = std::atoi(e) != 0;
    if (g_entry)
    {   // the way in without waiting for the clock (the vsyncs are counted in the EE's cycles: the game is the same, only sooner),
        // and without its sound
        if (!std::getenv("HL_FAST_UNTIL"))
            setenv("HL_FAST_UNTIL", std::to_string(kEntryEnd).c_str(), 1);
        hl_spu2_mute(true);
    }

    // a memory card of the match's own, empty: what the game finds at boot must be the same on both phones
    const std::string saves = dir + "/saves";
    std::filesystem::remove_all(saves, error);
    std::filesystem::create_directories(saves, error);
    setenv("HL_MC_ROOT", saves.c_str(), 1);
    // the same clock, in UTC (the runtime's sceCdReadClock, MemoryCard and the system configuration's time zone)
    setenv("HL_FIXED_CLOCK", kFixedClock, 1);
    setenv("TZ", "UTC0", 1);
    tzset();
    // Strict online/LAN determinism:
    //  - HL_IOP_THREAD=0: run IOP synchronously on exact EE cycles (async IopWorker lagged up to 54 ms / 2.7 vsyncs
    //    and caused SIF/CDVD/Ougi audio & post-match loads to complete on different vsyncs between fast and slow phones!)
    //  - HL_ONLINE_DETERMINISTIC=1: never gate/reorder EE Alarms or VBlankEnd by host wall clock during lockstep waits
    //  - HL_SKIP_INTRO=0: prevent per-phone intro-skip settings from altering boot timing
    setenv("HL_IOP_THREAD", "0", 1);
    setenv("HL_ONLINE_DETERMINISTIC", "1", 1);
    setenv("HL_SKIP_INTRO", "0", 1);
    // the launcher's assists change the pads in the game's memory (hl_game.cpp): both games must do the same, so the game's own rules
    hl_game::g_unlockAll.store(true, std::memory_order_relaxed);
    hl_game::g_specialOn.store(false, std::memory_order_relaxed);
    hl_game::g_mashPercent.store(100, std::memory_order_relaxed);

    // until the match begins both ports are at rest: a pad the game reads before its first vsync must be the same on both phones
    const uint8_t rest[6] = {0, 0, 0x80, 0x80, 0x80, 0x80};
    g_pads[0].store(pack(rest));
    g_pads[1].store(pack(rest));
    g_padsOn.store(true);
    g_prevVsync = g_hlVsyncHook;
    g_hlVsyncHook = file.spectator ? &onSpectatorVsync : &onVsync;
    g_hlPadPortHook = &padPortHook;
    std::string addrs;
    for (const auto &a : file.addresses)
        addrs += " " + a;
    log("online: " + std::string(file.spectator ? "spectating" : file.host ? "hosting" : "joining") + " room " + file.code + " (" + file.peerName + "), network version " +
        std::to_string(HL_NET_VERSION) + ", delay " + (file.delayFrames ? std::to_string(file.delayFrames) + " frames" : std::string("automatic")) +
        ", relay " + (file.relay ? "on" : "off") + ", broadcastSpec " + (file.broadcastSpec ? "on" : "off") + ", addresses" + (addrs.empty() ? " none" : addrs));
    if (file.spectator)
        publishSpectator("spectating");
    else
        publish("starting");
    return true;
}

// ---- the app's side (engine/OnlineBridge.kt)

extern "C" JNIEXPORT jstring JNICALL Java_com_dfdx047_hiddenleaf_engine_OnlineBridge_nativeStatus(JNIEnv *env, jclass)
{
    if (!g_active)
    {
        if (g_trainingFast)
        {
            const std::string s = "phase=training;field=" + std::to_string(g_trainingTick.load(std::memory_order_relaxed));
            return env->NewStringUTF(s.c_str());
        }
        return env->NewStringUTF("");
    }
    std::lock_guard<std::mutex> lock(g_statusMutex);
    return env->NewStringUTF(g_status.c_str());
}

// The player leaves the match (the panel's quit, or the back key): the other side is told at the next vsync.
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_OnlineBridge_nativeLeave(JNIEnv *, jclass)
{
    if (g_active)
    {
        if (g_file.spectator)
            g_specQuit.store(true, std::memory_order_release);
        else
            g_match.requestLeave();
    }
}

// A packet that came through the room's relay (the app's WebSocket).
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_OnlineBridge_nativeRelayIn(JNIEnv *env, jclass, jbyteArray data)
{
    if (!g_active || data == nullptr)
        return;
    const jsize n = env->GetArrayLength(data);
    if (n <= 0 || n > 4096)
        return;
    std::vector<uint8_t> packet(static_cast<std::size_t>(n));
    env->GetByteArrayRegion(data, 0, n, reinterpret_cast<jbyte *>(packet.data()));
    std::lock_guard<std::mutex> lock(g_relayMutex);
    if (g_relayIn.size() >= kRelayQueue)
        g_relayIn.pop_front();
    g_relayIn.push_back(std::move(packet));
}

// The next packet for the room's relay, waiting up to timeoutMs for one; null when none came.
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_dfdx047_hiddenleaf_engine_OnlineBridge_nativeRelayOut(JNIEnv *env, jclass, jint timeoutMs)
{
    std::vector<uint8_t> packet;
    {
        std::unique_lock<std::mutex> lock(g_relayMutex);
        if (!g_relayOutReady.wait_for(lock, std::chrono::milliseconds(timeoutMs > 0 ? timeoutMs : 0), [] { return !g_relayOut.empty(); }))
            return nullptr;
        packet = std::move(g_relayOut.front());
        g_relayOut.pop_front();
    }
    jbyteArray out = env->NewByteArray(static_cast<jsize>(packet.size()));
    if (out != nullptr)
        env->SetByteArrayRegion(out, 0, static_cast<jsize>(packet.size()), reinterpret_cast<const jbyte *>(packet.data()));
    return out;
}
