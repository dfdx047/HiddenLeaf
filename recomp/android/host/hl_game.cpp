// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself. All addresses are of the European ELF (SLES-55605).
#include "hl_game.h"

#include <SDL3/SDL.h>

#include "ps2_runtime.h"
#include "ps2_runtime_macros.h"
#include "runtime/hl_vif_dump.h"
#include "runtime/ee_scheduler.h"

#include <algorithm>
#include <cctype>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <string>
#include <sys/system_properties.h>
#include <thread>
#include <vector>

#if defined(HL_WATCH_BUILD)
namespace hl_watch
{
    extern PS2Runtime *g_runtime;
}
#endif
namespace hl_game
{
    std::atomic<int> g_paused{0};
    std::atomic<float> g_cameraXScale{1.0f};
    std::atomic<bool> g_unlockAll{false};
    std::atomic<bool> g_specialOn{true};
    std::atomic<uint32_t> g_specialMask{0x20u};
    std::atomic<int> g_mashPercent{100};
    bool (*g_nativeOn)() = nullptr;
    void (*g_gsDump)(const char *, int) = nullptr;
    std::string g_gsDumpDir;
    std::string g_vifDumpDir;
}


extern PS2Runtime::RecompiledFunction un5_btl_table[] __attribute__((weak));
extern const uint32_t un5_btl_base __attribute__((weak));
extern const uint32_t un5_btl_slots __attribute__((weak));

namespace
{
    constexpr uint32_t kOverlayBase = 0x6C6D00u;   // where BTL.BIN, ADV.BIN and ETC.BIN are loaded
    constexpr uint32_t kSavePointer = 0x617CF8u;
    constexpr uint32_t kSaveMatrix = 0x38u, kSaveMatrixBytes = 0x8D0u, kSaveRoster = 0x909u, kSaveRosterEnd = 0x970u;
    constexpr uint32_t kSaveTables = 0x990u, kSaveTablesEnd = 0xB6Cu, kSaveProgress = 0xE68u, kSaveProgressMax = 0x6Eu;
    constexpr uint32_t kGfxPointer = 0x617AFCu;    // gp-0x3F4: the graphics state; byte 0 counts vsyncs, byte 1 is how many a frame waits for (0x108510)
    constexpr uint32_t kCameraMatrices = 0x10DD00u; // builds a camera's matrices from its fields; +0x274 is the horizontal scale
    constexpr uint32_t kCameraXScale = 0x274u;     // (the same field the community's widescreen patch writes)
    constexpr uint32_t kCameraList = 0x617ACCu;    // *(gp-0x424), gp 0x617EF0: the first live camera
    constexpr uint32_t kCameraNext = 0x268u;       // the next one

    uint8_t *g_rdram = nullptr;
    PS2Runtime::RecompiledFunction g_cameraOriginal = nullptr;
    int g_divider = 0; // HL_GFX_DIV: experiment, vsyncs per game frame

    inline uint32_t read32(uint32_t address)
    {
        uint32_t v;
        std::memcpy(&v, g_rdram + (address & 0x1FFFFFFu), 4);
        return v;
    }
    inline float readf(uint32_t address)
    {
        float v;
        std::memcpy(&v, g_rdram + (address & 0x1FFFFFFu), 4);
        return v;
    }
    inline uint16_t read16(uint32_t address)
    {
        uint16_t v;
        std::memcpy(&v, g_rdram + (address & 0x1FFFFFFu), 2);
        return v;
    }

    uint64_t g_tick = 0u;          // vsyncs
    uint64_t g_fighterTick = 0u;   // the last vsync a fighter read its input (0x21E660): a fight is going on

    // The wide projection is for the 3D of a fight (while the fighters read their input, and an Ougi's film) and of the
    // campaign. Everything else drawn through a camera (the result screen's picture, menus) is 2D made for 4:3: squeezed, it
    // leaves the sides of the screen with nothing drawn on them. The result screen builds its matrices once, in the vsyncs
    // right after the fight: hence the short wait (6 vsyncs, the fighters read their input every 2).
    bool ougiOnScreen();
    bool wideCamera()
    {
        const int mode = hl_game::overlay();
        return mode == 2 || (mode == 1 && ((g_fighterTick != 0u && g_tick - g_fighterTick < 6u) || ougiOnScreen()));
    }

    void cameraScale(uint8_t *rdram, uint32_t camera)
    {
        const float scale = wideCamera() ? hl_game::g_cameraXScale.load(std::memory_order_relaxed) : 1.0f;
        static float s_last = 1.0f;
        if (scale != s_last)
        {
            s_last = scale;
            std::fprintf(stderr, "[cam] v%llu x %.3f\n", static_cast<unsigned long long>(g_tick), scale);
        }
        std::memcpy(rdram + ((camera + kCameraXScale) & 0x1FFFFFFu), &scale, 4);
        // (the cameras the fight made wide: a picture may be built later from one of them without coming back here)
        static uint32_t s_cameras[16];
        static uint32_t s_count = 0u;
        if (scale != 1.0f)
        {
            bool known = false;
            for (uint32_t i = 0; i < s_count; ++i)
                known = known || s_cameras[i] == camera;
            if (!known && s_count < 16u)
                s_cameras[s_count++] = camera;
        }
        else if (s_count != 0u)
        {
            // Only where the field still holds the wide scale we wrote: when the fight ends the game frees some of these
            // cameras, and writing into a freed (or reused) block broke the heap's free list (the hang after the KO, the
            // allocator 0x118E60 reading a bad pointer forever).
            // And only the cameras still in the game's list of live cameras (head *(gp-0x424), next at +0x268, the list
            // 0x10F420 walks): a camera taken out of it may already be a free block, whatever its +0x274 holds.
            const float wide = hl_game::g_cameraXScale.load(std::memory_order_relaxed);
            uint32_t live[64];
            uint32_t liveCount = 0u;
            for (uint32_t c = read32(kCameraList); c != 0u && liveCount < 64u; c = read32(c + kCameraNext))
            {
                if ((c & 3u) != 0u || c < 0x100000u || c >= 0x2000000u)
                    break;
                live[liveCount++] = c;
            }
            uint32_t back = 0u;
            for (uint32_t i = 0; i < s_count; ++i)
            {
                bool isLive = false;
                for (uint32_t j = 0; j < liveCount; ++j)
                    isLive = isLive || live[j] == s_cameras[i];
                if (!isLive)
                    continue;
                uint8_t *field = rdram + ((s_cameras[i] + kCameraXScale) & 0x1FFFFFFu);
                float now;
                std::memcpy(&now, field, 4);
                if (now == wide)
                {
                    std::memcpy(field, &scale, 4);
                    ++back;
                }
            }
            std::fprintf(stderr, "[cam] v%llu %u of %u cameras back to 1 (%u cameras live)\n", static_cast<unsigned long long>(g_tick), back,
                         s_count, liveCount);
            s_count = 0u;
        }
    }

#if defined(HL_WATCH_BUILD)
    // Lab builds only (HL_WATCH_BUILD): HL_FREECHECK=1 checks every free of the game's heap (0x1181B0, a0 = the pointer; the
    // block's header 16 bytes before it: +0 the block before it in memory, +4 the one after, +8 the size, +0xC the tag) and
    // logs a free whose header is not linked both ways (the pointer, its header and the caller); HL_FREECHECK=2 also skips it.
    constexpr uint32_t kHeapFree = 0x1181B0u;
    PS2Runtime::RecompiledFunction g_freeOriginal = nullptr;
    int g_freeCheck = 0;

    void heapFree(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t p = GPR_U32(ctx, 4);
        if (p != 0u)
        {
            const uint32_t h = p - 0x10u;
            auto ok = [](uint32_t a) { return (a & 15u) == 0u && a >= 0x100000u && a < 0x2000000u; };
            const uint32_t before = read32(h), after = read32(h + 4u);
            const bool linked = ok(h) && ok(before) && ok(after) && read32(before + 4u) == h && read32(after) == h;
            if (!linked)
            {
                static int s_count = 0;
                if (s_count++ < 60)
                {
                    // the callers: the code addresses on the guest stack (the free wrapper 0x117570 keeps its ra at sp+0)
                    const uint32_t sp = GPR_U32(ctx, 29);
                    char calls[256];
                    int n = 0, found = 0;
                    calls[0] = 0;
                    for (uint32_t o = 0; o < 0x200u && found < 10 && n < 200; o += 4u)
                    {
                        const uint32_t w = read32(sp + o);
                        if (w >= 0x100008u && w < 0x3d5760u && (w & 3u) == 0u)
                        {
                            n += std::snprintf(calls + n, sizeof(calls) - n, " sp+%x:%x", o, w);
                            ++found;
                        }
                    }
                    std::fprintf(stderr, "[free] v%llu bad free 0x%08x: header %08x %08x %08x %08x, ra 0x%x, sp 0x%x, thread %d%s; stack%s\n",
                                 static_cast<unsigned long long>(g_tick), p, before, after, read32(h + 8u), read32(h + 12u),
                                 GPR_U32(ctx, 31), sp, runtime->eeScheduler().currentThreadId(), g_freeCheck >= 2 ? " (skipped)" : "", calls);
                }
                if (g_freeCheck >= 2)
                {
                    ctx->pc = GPR_U32(ctx, 31);
                    return;
                }
            }
        }
        g_freeOriginal(rdram, ctx, runtime);
    }

    // HL_LISTCHECK=2: 0x18DB30(obj, flag) frees the node list at obj+0x14 (next at +0x9C). Before it runs, walk the list and,
    // if a link is not a heap pointer, log obj, every node, the bytes around the last good node's +0x9C and the callers.
    void stackCalls(uint32_t sp, char *out, size_t size);
    constexpr uint32_t kListFree = 0x18DB30u;
    PS2Runtime::RecompiledFunction g_listFreeOriginal = nullptr;

    void listFree(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t obj = GPR_U32(ctx, 4);
        auto ok = [](uint32_t a) { return a == 0u || ((a & 15u) == 0u && a >= 0x100000u && a < 0x2000000u); };
        static int s_logged = 0;
        if (obj != 0u && ok(obj) && s_logged < 20)
        {
            uint32_t nodes[64];
            int n = 0;
            uint32_t q = read32(obj + 0x14u);
            while (q != 0u && ok(q) && n < 64)
            {
                nodes[n++] = q;
                q = read32(q + 0x9Cu);
            }
            if (q != 0u && n < 64)
            {
                ++s_logged;
                char list[64 * 10 + 1];
                int m = 0;
                list[0] = 0;
                for (int i = 0; i < n && m < static_cast<int>(sizeof(list)) - 10; ++i)
                    m += std::snprintf(list + m, sizeof(list) - m, " %x", nodes[i]);
                char calls[200];
                stackCalls(GPR_U32(ctx, 29), calls, sizeof(calls));
                std::fprintf(stderr, "[listfree] v%llu obj 0x%08x (header %08x %08x %08x %08x): %d nodes%s, then 0x%08x; stack%s\n",
                             static_cast<unsigned long long>(g_tick), obj, read32(obj - 0x10u), read32(obj - 0xCu), read32(obj - 8u),
                             read32(obj - 4u), n, list, q, calls);
                const uint32_t last = n > 0 ? nodes[n - 1] : obj;
                for (uint32_t a = last - 0x10u; a < last + 0xC0u; a += 16u)
                    std::fprintf(stderr, "[listfree]   %08x: %08x %08x %08x %08x\n", a, read32(a), read32(a + 4u), read32(a + 8u), read32(a + 12u));
            }
        }
        g_listFreeOriginal(rdram, ctx, runtime);
    }

    // HL_LISTCHECK=1: the list the bad frees come from. 0x18D410(a0, a1) appends a 0xA0-byte node (next at +0x9C) through
    // the tail pointer at a1+8 (node+0x9C = *tail; *tail = node; tail = &node+0x9C): a tail slot that is not 0 is a tail
    // left pointing into a freed node. Logs a1, the tail, its value and the callers on the stack.
    constexpr uint32_t kListAppend = 0x18D410u;
    PS2Runtime::RecompiledFunction g_appendOriginal = nullptr;

    void stackCalls(uint32_t sp, char *out, size_t size)
    {
        int n = 0, found = 0;
        out[0] = 0;
        for (uint32_t o = 0; o < 0x200u && found < 8 && n < static_cast<int>(size) - 24; o += 4u)
        {
            const uint32_t w = read32(sp + o);
            if (w >= 0x100008u && w < 0x3d5760u && (w & 3u) == 0u)
            {
                n += std::snprintf(out + n, size - n, " %x", w);
                ++found;
            }
        }
    }

    void listAppend(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t list = GPR_U32(ctx, 5);
        const uint32_t tail = read32(list + 8u);
        const uint32_t at = read32(tail);
        static int s_logged = 0, s_seen = 0;
        ++s_seen;
        if ((at != 0u || s_seen <= 3) && s_logged < 60)
        {
            ++s_logged;
            char calls[200];
            stackCalls(GPR_U32(ctx, 29), calls, sizeof(calls));
            std::fprintf(stderr, "[list] v%llu append #%d: owner 0x%08x list 0x%08x tail 0x%08x holds 0x%08x%s, ra 0x%x, thread %d; stack%s\n",
                         static_cast<unsigned long long>(g_tick), s_seen, GPR_U32(ctx, 4), list, tail, at, at != 0u ? " (STALE TAIL)" : "",
                         GPR_U32(ctx, 31), runtime->eeScheduler().currentThreadId(), calls);
        }
        g_appendOriginal(rdram, ctx, runtime);
    }
#endif

    void cameraMatrices(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        cameraScale(rdram, GPR_U32(ctx, 4));
        g_cameraOriginal(rdram, ctx, runtime);
    }

    // The launcher's "unlock everything": the game's save state in memory (what it writes to data01..04, the pointer is
    // 0x617CF8). +0x38: 94 rows of 0x18 bytes, 2 bits per pair, 3 = the character / the link is open (the first pair, a
    // character with itself, is 3 once the structure is filled in). +0x908 + id (1..0x67): the character (bit 0 = open).
    // +0x990..0xB6C: the collection's tables, one byte per entry (under 2 = locked, 2 = new, 3 = seen). +0xE68: the
    // progress counter the collection compares against (0x3E, 0x66, 0x6E). Rewritten every second, since loading a save
    // brings the old values back; the game itself writes them to the card the next time the player saves.
    void unlockEverything()
    {
        const uint32_t save = read32(kSavePointer);
        if (save < 0x100000u || save > 0x1F00000u)
            return;
        uint8_t *const state = g_rdram + save;
        if ((state[kSaveMatrix] & 3u) != 3u)
            return;
        std::memset(state + kSaveMatrix, 0xFF, kSaveMatrixBytes);
        std::memset(state + kSaveRoster, 3, kSaveRosterEnd - kSaveRoster);
        for (uint32_t i = kSaveTables; i < kSaveTablesEnd; ++i)
            if (state[i] < 3u)
                state[i] = 3u;
        uint32_t progress;
        std::memcpy(&progress, state + kSaveProgress, 4);
        if (progress < kSaveProgressMax)
        {
            progress = kSaveProgressMax;
            std::memcpy(state + kSaveProgress, &progress, 4);
        }
    }

    // HL_FTRACE=1: every change of a fighter's action and input, for finding how the game's moves work
    PS2Runtime::RecompiledFunction g_inputOriginal = nullptr;

    // ---- "Special with one button" (the launcher's Controls tab). Ultimate Ninja 5's Jutsu and Ougi are a tap of chakra (each
    // spends 5 of the fighter's 15) and then the attack. Two places, for a human fighter only:
    //  - the fighter's input of the frame (fighter+0x338, what the moves read): a tap of the special button standing still turns
    //    into the sequence, the Ougi with 10 of chakra or more, the Jutsu with 5. Without chakra it is left alone.
    //  - the sequence of an Ougi (0x36E690, the same for every character: obj+0x112 + (player << 8) the buttons, obj+0x312 +
    //    player * 2 the one it is at, obj+0x1A + player nonzero for the CPU): a tap counts as the button of the moment.
    //  - a prompt of one button (0x6C99B0, in BTL.BIN; +0x28 its pad): the tap is its button.
    //  - the steps of pressing repeatedly (each reads "the attack came down" on the pad on its own, 0x7AF440, 0x819D10,
    //    0x820FD0): while an Ougi is on screen or two fighters dispute one, a tap is worth as many presses as the multiplier
    //    says, given on the pad's read (0x113910).
    // Fighter fields: +0x18E action (0 idle, 1 walking, 8 a move), +0x24 its input controller (+0x64 its pad), +0x60 bits 5..8
    // nonzero for the CPU, +0x70 chakra, +0xA4C the move (+0x10 flags), +0xB00 the dispute between two fighters.
    constexpr uint32_t kPadRead = 0x113910u, kFighterInput = 0x21E660u, kSequenceStep = 0x36E690u, kPromptPressed = 0x6C99B0u;
    constexpr uint32_t kPadCurrent = 0x64u, kPadPressed = 0x68u, kPadRepeat = 0x70u;
    constexpr uint32_t kFighterAction = 0x18Eu, kFighterController = 0x24u, kFighterKind = 0x60u, kFighterChakra = 0x70u, kFighterWord = 0x338u;
    constexpr uint32_t kFighterMove = 0xA4Cu, kFighterDispute = 0xB00u;
    constexpr uint32_t kMoveSpecial = 0x00FF0000u; // (0xF0000 the jutsu, 0xF00000 the ougi)
    constexpr uint32_t kInChakra = 0x08000000u, kInAttack = 0x40001000u, kInDirections = 0xFFFu;
    constexpr uint32_t kPadFace = 0x00F0u, kPadDirections = 0xF000u, kPadCircle = 0x20u;
    constexpr uint32_t kChakraTap = 5u;

    PS2Runtime::RecompiledFunction g_padOriginal = nullptr;
    uint32_t g_fighters[4] = {};
    uint32_t g_padTapped[2] = {};   // the special button came down on this pad in this frame
    uint32_t g_padAdded[2] = {};    // bits of the current word that are ours, taken out before the next read
    int g_padCredit[2] = {};        // hundredths of a press still to give
    uint32_t g_padSynthetic[2] = {}; // the presses of this frame that are ours
    uint32_t g_padSpecialDown[2] = {}; // the special button came down in this frame (g_padTapped is taken by the fighter)
    int g_testTap = 0;              // HL_SP_TAP: a tap of the special button every so many vsyncs, for testing
    int g_testCpu = 0;              // HL_SP_CPU: the second fighter does an Ougi when it can, every so many vsyncs
    uint64_t g_nextCpu = 0u;

    struct Sequence
    {
        uint32_t fighter;
        int step, length;
        float chakra;
    };
    Sequence g_sequence[4] = {};

    int fighterSlot(uint32_t f)
    {
        for (int i = 0; i < 4; ++i)
        {
            if (g_fighters[i] == f)
                return i;
            if (g_fighters[i] == 0u)
            {
                g_fighters[i] = f;
                g_sequence[i] = Sequence{f, -1, 0, 0.0f};
                return i;
            }
        }
        std::memset(g_fighters, 0, sizeof(g_fighters)); // (a new fight)
        g_fighters[0] = f;
        g_sequence[0] = Sequence{f, -1, 0, 0.0f};
        return 0;
    }

    inline bool validAddress(uint32_t a) { return a >= 0x100000u && a < 0x1F00000u; }

    inline bool humanFighter(uint32_t f) { return ((read16(f + kFighterKind) >> 5) & 0xFu) == 0u; }

    // An Ougi on the screen, or a dispute: from then on the taps answer the game's prompts.
    bool ougiOnScreen()
    {
        if (hl_game::overlay() != 1)
            return false;
        for (int i = 0; i < 4; ++i)
        {
            const uint32_t f = g_fighters[i];
            if (!validAddress(f))
                continue;
            if ((read32(f + kFighterDispute) & 0xFF00u) != 0u)
                return true;
            const uint32_t move = read32(f + kFighterMove);
            if (read16(f + kFighterAction) == 8u && validAddress(move) && (read32(move + 0x10u) & kMoveSpecial) != 0u)
                return true;
        }
        return false;
    }

    // The pad's bit the game's Controls option gives to the attack (0x1FA7E0: a table of 8 shorts per player).
    uint32_t attackBit(uint32_t player)
    {
        const uint32_t table = 0x6C5550u + 16u * (player + 1u);
        const uint32_t bit = read16(table + 2u);
        return bit != 0u ? bit : kPadCircle;
    }

    // The sequence's reader: the tap of this frame is the button it wants. A press we made up for the steps of pressing
    // repeatedly would be a wrong one here.
    PS2Runtime::RecompiledFunction g_sequenceOriginal = nullptr;
    void sequenceStep(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        static constexpr uint32_t kBits[8] = {0x10u, 0x40u, 0x20u, 0x80u, 0x1000u, 0x4000u, 0x2000u, 0x8000u};
        const uint32_t obj = GPR_U32(ctx, 4);
        const uint32_t gfx = read32(kGfxPointer);
        uint32_t saved[2] = {};
        const bool on = hl_game::g_specialOn.load(std::memory_order_relaxed);
        for (uint32_t p = 0; on && p < 2u; ++p)
        {
            const uint32_t pad = gfx + 0x1Cu + p * 0x78u;
            saved[p] = read32(pad + kPadPressed);
            uint32_t pressed = saved[p] & ~g_padSynthetic[p];
            if (g_padSpecialDown[p] != 0u && rdram[(obj + 0x1Au + p) & 0x1FFFFFFu] == 0u)
            {
                const int16_t at = static_cast<int16_t>(read16(obj + 0x312u + p * 2u));
                const int code = static_cast<int8_t>(rdram[(obj + 0x112u + (p << 8) + static_cast<uint32_t>(at)) & 0x1FFFFFFu]);
                if (at >= 0 && at < 0x100 && code >= 0 && code < 8)
                    pressed = kBits[code];
            }
            std::memcpy(rdram + pad + kPadPressed, &pressed, 4);
        }
        g_sequenceOriginal(rdram, ctx, runtime);
        for (uint32_t p = 0; on && p < 2u; ++p)
            std::memcpy(rdram + gfx + 0x1Cu + p * 0x78u + kPadPressed, &saved[p], 4);
    }

    PS2Runtime::RecompiledFunction g_promptOriginal = nullptr;
    void promptPressed(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t obj = GPR_U32(ctx, 4);
        g_promptOriginal(rdram, ctx, runtime);
        const uint32_t pad = read16(obj + 0x28u);
        if (GPR_U32(ctx, 2) == 0u && pad < 2u && g_padSpecialDown[pad] != 0u && rdram[(obj + 0x2Eu) & 0x1FFFFFFu] != 1u && hl_game::g_specialOn.load(std::memory_order_relaxed))
            SET_GPR_U32(ctx, 2, 1u);
    }

    // BTL.BIN's functions are in its own table (recomp/overlays.py), which answers only while it is the one in memory.
    void hookOverlay()
    {
        if (&un5_btl_table == nullptr || g_promptOriginal != nullptr)
            return;
        const uint32_t slot = (kPromptPressed - un5_btl_base) >> 2;
        if (slot < un5_btl_slots && un5_btl_table[slot] != nullptr)
        {
            g_promptOriginal = un5_btl_table[slot];
            un5_btl_table[slot] = promptPressed;
        }
    }

    void padRead(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t pad = GPR_U32(ctx, 4);
        const uint32_t gfx = read32(kGfxPointer);
        const uint32_t index = (pad - gfx - 0x1Cu) / 0x78u;
        const bool known = index < 2u && pad == gfx + 0x1Cu + index * 0x78u;
        if (known && g_padAdded[index] != 0u)
        {
            const uint32_t held = read32(pad + kPadCurrent) & ~g_padAdded[index];
            std::memcpy(rdram + pad + kPadCurrent, &held, 4);
            g_padAdded[index] = 0u;
        }
        g_padOriginal(rdram, ctx, runtime);
        if (!known || !hl_game::g_specialOn.load(std::memory_order_relaxed))
            return;
        const uint32_t mask = hl_game::g_specialMask.load(std::memory_order_relaxed);
        uint32_t pressed = read32(pad + kPadPressed);
        uint32_t current = read32(pad + kPadCurrent);
        static uint64_t s_once = std::getenv("HL_SP_ONCE") ? std::strtoull(std::getenv("HL_SP_ONCE"), nullptr, 10) : 0u;
        if (s_once != 0u && g_tick >= s_once && index == 0u && g_fighters[0] != 0u && read16(g_fighters[0] + kFighterAction) == 0u)
        {
            s_once = 0u;
            pressed |= mask;
            current |= mask;
            g_padAdded[index] |= mask;
            std::memcpy(rdram + pad + kPadPressed, &pressed, 4);
            std::memcpy(rdram + pad + kPadCurrent, &current, 4);
        }
        if (g_testTap > 0 && index == 0u && g_tick >= 5700u && g_tick % static_cast<uint64_t>(g_testTap) < 2u)
        {
            pressed |= mask;
            current |= mask;
            g_padAdded[index] |= mask;
        }
        const bool tapped = (pressed & mask) != 0u;
        g_padTapped[index] = tapped ? 1u : 0u;
        g_padSpecialDown[index] = tapped ? 1u : 0u;
        g_padSynthetic[index] = 0u;
        if (!ougiOnScreen())
        {
            g_padCredit[index] = 0;
            if (g_testTap > 0 && index == 0u)
            {
                std::memcpy(rdram + pad + kPadPressed, &pressed, 4);
                std::memcpy(rdram + pad + kPadCurrent, &current, 4);
            }
            return;
        }
        if (tapped)
            g_padCredit[index] += hl_game::g_mashPercent.load(std::memory_order_relaxed);
        if (g_padCredit[index] >= 100)
        {
            g_padCredit[index] -= 100;
            const uint32_t answer = attackBit(index);
            g_padAdded[index] |= answer & ~current;
            g_padSynthetic[index] = answer & ~pressed;
            pressed |= answer;
            current |= answer;
            const uint32_t repeat = read32(pad + kPadRepeat) | answer;
            std::memcpy(rdram + pad + kPadRepeat, &repeat, 4);
            static const bool s_log = std::getenv("HL_SP_LOG") != nullptr;
            if (s_log)
                std::fprintf(stderr, "[sp] v%llu pad %u answer (credit %d)\n", static_cast<unsigned long long>(g_tick), index, g_padCredit[index]);
        }
        std::memcpy(rdram + pad + kPadPressed, &pressed, 4);
        std::memcpy(rdram + pad + kPadCurrent, &current, 4);
    }

    void special(uint32_t f)
    {
        static const uint32_t kJutsu[] = {kInChakra, kInChakra, 0u, kInAttack, kInAttack, 0u};
        constexpr int kOugiLength = 9;
        const int slot = fighterSlot(f);
        Sequence &q = g_sequence[slot];
        const uint32_t controller = read32(f + kFighterController);
        bool start = false;
        if (validAddress(controller) && humanFighter(f) && hl_game::g_specialOn.load(std::memory_order_relaxed))
        {
            const uint32_t pad = read32(controller + 0x64u);
            const uint32_t gfx = read32(kGfxPointer);
            const uint32_t index = (pad - gfx - 0x1Cu) / 0x78u;
            if (index < 2u && pad == gfx + 0x1Cu + index * 0x78u && g_padTapped[index] != 0u)
            {
                g_padTapped[index] = 0u;
                const uint32_t mask = hl_game::g_specialMask.load(std::memory_order_relaxed);
                const uint32_t action = read16(f + kFighterAction);
                // (the circle is also the attack: with a direction, or in the middle of anything, it stays the attack)
                const bool plain = mask != kPadCircle || (read32(pad + kPadCurrent) & kPadDirections) == 0u;
                start = q.step < 0 && (action == 0u || (action == 1u && mask != kPadCircle)) && plain && readf(f + kFighterChakra) >= static_cast<float>(kChakraTap);
            }
        }
        else if (g_testCpu > 0 && !humanFighter(f) && q.step < 0 && g_tick >= g_nextCpu && read16(f + kFighterAction) == 0u && readf(f + kFighterChakra) >= 10.0f)
        {
            g_nextCpu = g_tick + g_testCpu;
            start = true;
        }
        uint32_t word = read32(f + kFighterWord);
        if (start)
        {
            q.step = 0;
            q.length = readf(f + kFighterChakra) >= static_cast<float>(2u * kChakraTap) ? kOugiLength : 6;
            q.chakra = readf(f + kFighterChakra);
            static const bool s_log = std::getenv("HL_SP_LOG") != nullptr;
            if (s_log)
                std::fprintf(stderr, "[sp] v%llu fighter %x %s, chakra %.1f\n", static_cast<unsigned long long>(g_tick), f, q.length != 6 ? "ougi" : "jutsu", q.chakra);
        }
        if (q.step < 0)
            return;
        // (the first tap of chakra refused, the game has its reasons: then it was a plain press)
        if (q.step == 2 && readf(f + kFighterChakra) >= q.chakra)
        {
            q.step = -1;
            return;
        }
        const int t = q.length - q.step; // (from the end: attack, attack, nothing)
        const uint32_t bits = q.length == 6 ? kJutsu[q.step] : (q.step < 2 || (t <= 5 && t >= 4)) ? kInChakra : (t <= 3 && t >= 2) ? kInAttack : 0u;
        word = (word & kInDirections) | bits;
        std::memcpy(g_rdram + f + kFighterWord, &word, 4);
        if (++q.step >= q.length)
            q.step = -1;
    }

    // HL_CPU_HP=<vsync>[,<vsync>...] (test routes): at the first input of a CPU fighter on or after each vsync, its health
    // (the float at fighter+0x6C, 1.0 full) becomes 0.0115, so one hit wins the fight. The routes used to POKE a fixed address
    // (d7ba0c, the CPU fighter at 0xd7b9a0 of the stub-libsdr build): with the real libsdr the heap is laid out otherwise and
    // that poke landed in the game's node list (docs/registro.md, 2026-11-04 18:40).
    void cpuHealthRoute(uint32_t f)
    {
        static std::vector<uint64_t> s_at;
        static size_t s_next = 0;
        static bool s_read = false;
        if (!s_read)
        {
            s_read = true;
            if (const char *v = std::getenv("HL_CPU_HP"))
                for (const char *c = v; *c != 0;)
                {
                    char *end = nullptr;
                    const unsigned long long t = std::strtoull(c, &end, 10);
                    if (end == c)
                        break;
                    s_at.push_back(t);
                    c = *end == ',' ? end + 1 : end;
                }
        }
        if (s_next >= s_at.size() || g_tick < s_at[s_next] || (read32(f + kFighterKind) & 0x1E0u) == 0u)
            return;
        const float hp = 0.0115f;
        std::memcpy(g_rdram + f + 0x6Cu, &hp, 4);
        std::fprintf(stderr, "[route] v%llu CPU fighter 0x%x: health %.4f\n", static_cast<unsigned long long>(g_tick), f, hp);
        ++s_next;
    }

    void fighterInput(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t f = GPR_U32(ctx, 4);
        g_inputOriginal(rdram, ctx, runtime);
        g_fighterTick = g_tick;
        special(f);
        cpuHealthRoute(f);
        static const bool s_trace = std::getenv("HL_FTRACE") != nullptr;
        if (!s_trace)
            return;
        static uint32_t s_f[4];
        static uint32_t s_last[4][8];
        int i = 0;
        for (; i < 4 && s_f[i] != f && s_f[i] != 0u; ++i) {}
        if (i == 4)
            return;
        if (s_f[i] == 0u)
        {
            s_f[i] = f;
            std::fprintf(stderr, "[ft] fighter %d @%x f60 %08x moves %x n %d\n", i, f, read32(f + 0x60u), read32(f + 0xA54u), static_cast<int16_t>(read16(f + 0xA38u)));
            const uint32_t t = read32(f + 0xA54u);
            const int n = static_cast<int16_t>(read16(f + 0xA38u));
            for (int m = 0; t != 0u && m < n && m < 64; ++m)
            {
                const uint32_t e = t + m * 0x54u;
                std::fprintf(stderr, "[ft]  move %d fl %08x %08x nx %d %08x cost %.1f\n", m, read32(e + 0x10u), read32(e + 0x14u), static_cast<int8_t>(g_rdram[e + 0x18u]), read32(e + 0x1Cu), readf(e + 0x20u));
            }
        }
        const uint32_t mv = read32(f + 0xA4Cu);
        const uint32_t v[8] = {read16(f + 0x18Eu), read16(f + 0x190u), read32(f + 0x338u) & 0xFFFFFF00u, read32(f + 0xB00u), mv, read16(f + 0xB08u), static_cast<uint32_t>(readf(f + 0x70u) / 5.0f), read32(f + 0x254u)};
        if (std::memcmp(v, s_last[i], sizeof(v)) != 0)
        {
            const uint32_t gfx = read32(kGfxPointer);
            std::fprintf(stderr, "[ft] v%llu f%d act %x %x in %08x b00 %x b08 %x chakra %.1f 254 %x mv %x fl %08x pad %08x %08x hp %d %d\n", static_cast<unsigned long long>(g_tick), i, v[0], v[1], read32(f + 0x338u), v[3], v[5], readf(f + 0x70u), v[7], mv,
                         mv ? read32(mv + 0x10u) : 0u, read32(gfx + 0x80u), read32(gfx + 0x84u), static_cast<int16_t>(read16(f + 0x324u)), static_cast<int16_t>(read16(f + 0x9F6u)));
            std::memcpy(s_last[i], v, sizeof(v));
        }
    }

    // ---- HL_GS_DUMP=scene,scene,... (HL_GS_DUMP_FRAMES=N, 300 by default): a PCSX2 GS dump of N vsyncs at each scene of the
    // automatic route, once per scene, into <game folder>/gsdumps/<device>_<scene>.gs.zst (libbt4gsx records it). A scene
    // with a detector starts where the detector first sees it; the others at a fixed vsync of the route (build/lab: hl_env_v3.txt
    // for the fight route, hl_env_camp.txt for the campaign). "scene@vsync" puts any of them at a vsync of one's own.
    // HL_GS_DUMP_DEVICE names the device (by default ro.product.model).
    // HL_VIF_DUMP=scene,... (HL_VIF_DUMP_FRAMES=N, 300) takes the same scenes and triggers for the VIF1 stream and the GIF the
    // VIF1/VU1 interpreters make of it: <game folder>/vifdumps/<device>_<scene>.vif and .gif (runtime/hl_vif_dump.cpp).
    enum class DumpBy { Vsync, FightStart, OugiHuman, OugiCpu };
    struct DumpScene
    {
        const char *name;
        DumpBy by;
        uint64_t at;      // the route's vsync (DumpBy::Vsync)
        int overlayNeeded; // 0 any, else hl_game::overlay() must be this one at the vsync (a guard for the wrong route)
    };
    // Vsyncs of the route v3 (and of the campaign route), from rec/rp5_v3h and rec/rp5_camp3 (the same on the RP5, the Cube and
    // the Odin 3 in 16:9 and 20:9 up to the first fight's results):
    //   logos 1200: the BANDAI logo fading out and the CRIWARE / CyberConnect2 film (LOGO_C.PSS from ~1405)
    //   filme 2000: the opening (OPENING.PSS from ~1876, skipped by START at 2866)
    //   titulo 3000: the title after the skip (at 2900 it is still black), to the START at 3116
    //   menu 3800: Mode Select (after New Game at 3436 and its NOW LOADING, still on at 3620), Free Battle at 3965
    //   selecao 4700: the character selection
    //   ko 7880: the end of the player's Ougi (from 6524: the route's circle is the special button) and the K.O. at 7936
    //   winner 8540: WINNER with "Naruto Uzumaki" and his picture (at 8050 the fight is still on screen after the K.O.)
    //   resultado 8950: "Ninja Song" (Health, Time, Bonus, Ryo; at 8550 it is still the WINNER)
    //   campanha_vila 8150: walking in the village (the campaign route's LRIGHT from 8100; the ADV detector, the native
    //     renderer on, first sees the cut scene at the gate, v4559, not the walk)
    //   campanha_cortina 9080: the first change of area (the native off at 9112 to 9199: the curtain and the loading)
    const DumpScene kDumpScenes[] = {
        {"logos", DumpBy::Vsync, 1200u, 0},         {"filme", DumpBy::Vsync, 2000u, 0},
        {"titulo", DumpBy::Vsync, 3000u, 0},        {"menu", DumpBy::Vsync, 3800u, 0},
        {"selecao", DumpBy::Vsync, 4700u, 0},       {"luta_inicio", DumpBy::FightStart, 0u, 1},
        {"ougi_jogador", DumpBy::OugiHuman, 0u, 1}, {"ougi_cpu", DumpBy::OugiCpu, 0u, 1},
        {"ko", DumpBy::Vsync, 7880u, 1},            {"winner", DumpBy::Vsync, 8540u, 0},
        {"resultado", DumpBy::Vsync, 8950u, 0},     {"campanha_vila", DumpBy::Vsync, 8150u, 2},
        {"campanha_cortina", DumpBy::Vsync, 9080u, 2},
    };
    constexpr uint32_t kMoveOugi = 0x00F00000u;

    struct DumpWanted
    {
        DumpScene scene;
        bool done;
        bool vif; // HL_VIF_DUMP, not HL_GS_DUMP
    };
    std::vector<DumpWanted> g_dumpWanted;
    int g_dumpFrames = 300, g_vifDumpFrames = 300;
    std::string g_dumpDevice;

    void dumpSetup()
    {
        const char *gsList = std::getenv("HL_GS_DUMP");
        const char *vifList = std::getenv("HL_VIF_DUMP");
        if ((gsList == nullptr || gsList[0] == '\0') && (vifList == nullptr || vifList[0] == '\0'))
            return;
        if (const char *f = std::getenv("HL_GS_DUMP_FRAMES"))
            g_dumpFrames = std::max(1, std::atoi(f));
        if (const char *f = std::getenv("HL_VIF_DUMP_FRAMES"))
            g_vifDumpFrames = std::max(1, std::atoi(f));
        if (const char *d = std::getenv("HL_GS_DUMP_DEVICE"); d != nullptr && d[0] != '\0')
            g_dumpDevice = d;
        else
        {
            char model[PROP_VALUE_MAX] = {};
            __system_property_get("ro.product.model", model);
            for (const char *c = model; *c; ++c)
                g_dumpDevice += std::isalnum(static_cast<unsigned char>(*c)) ? static_cast<char>(std::tolower(static_cast<unsigned char>(*c))) : '_';
            if (g_dumpDevice.empty())
                g_dumpDevice = "android";
        }
        for (int vif = 0; vif < 2; ++vif)
        {
            const char *list = vif ? vifList : gsList;
            if (list == nullptr)
                continue;
            std::string text = list;
            size_t pos = 0;
            while (pos <= text.size())
            {
                const size_t comma = std::min(text.find(',', pos), text.size());
                std::string item = text.substr(pos, comma - pos);
                pos = comma + 1;
                if (item.empty())
                    continue;
                uint64_t at = 0u;
                const size_t atSign = item.find('@');
                if (atSign != std::string::npos)
                {
                    at = std::strtoull(item.c_str() + atSign + 1, nullptr, 10);
                    item.resize(atSign);
                }
                bool known = false;
                for (const DumpScene &s : kDumpScenes)
                    if (item == s.name)
                    {
                        DumpWanted w{s, false, vif != 0};
                        if (at != 0u)
                        {
                            w.scene.by = DumpBy::Vsync;
                            w.scene.at = at;
                        }
                        g_dumpWanted.push_back(w);
                        known = true;
                    }
                if (!known)
                    std::fprintf(stderr, "[gsdump] unknown scene '%s'\n", item.c_str());
            }
        }
        std::fprintf(stderr, "[gsdump] %zu scenes (GS %d vsyncs each into %s, VIF %d into %s), device %s\n", g_dumpWanted.size(), g_dumpFrames,
                     hl_game::g_gsDumpDir.c_str(), g_vifDumpFrames, hl_game::g_vifDumpDir.c_str(), g_dumpDevice.c_str());
    }

    // The Ougi detector (the one of the special button, ougiOnScreen) by who does it: 1 a human fighter, 2 the CPU.
    int ougiBy()
    {
        int who = 0;
        for (int i = 0; i < 4; ++i)
        {
            const uint32_t f = g_fighters[i];
            if (!validAddress(f))
                continue;
            const uint32_t move = read32(f + kFighterMove);
            if (read16(f + kFighterAction) == 8u && validAddress(move) && (read32(move + 0x10u) & kMoveOugi) != 0u)
                who |= humanFighter(f) ? 1 : 2;
        }
        return who;
    }

    void dumpTick(uint64_t tick)
    {
        if (g_dumpWanted.empty())
            return;
        const int mode = hl_game::overlay();
        const int ougi = mode == 1 ? ougiBy() : 0;
        const bool fighting = mode == 1 && g_fighterTick != 0u && tick - g_fighterTick < 2u;
        for (DumpWanted &w : g_dumpWanted)
        {
            if (w.done)
                continue;
            bool now = false;
            switch (w.scene.by)
            {
            case DumpBy::Vsync: now = tick >= w.scene.at; break;
            case DumpBy::FightStart: now = fighting; break;
            case DumpBy::OugiHuman: now = (ougi & 1) != 0; break;
            case DumpBy::OugiCpu: now = (ougi & 2) != 0; break;
            }
            if (!now)
                continue;
            w.done = true;
            if (w.scene.overlayNeeded != 0 && mode != w.scene.overlayNeeded)
            {
                std::fprintf(stderr, "[gsdump] v%llu %s skipped: overlay %d, not %d (another route?)\n", static_cast<unsigned long long>(tick), w.scene.name, mode,
                             w.scene.overlayNeeded);
                continue;
            }
            if (w.vif)
            {
                const std::string base = hl_game::g_vifDumpDir + "/" + g_dumpDevice + "_" + w.scene.name;
                std::fprintf(stderr, "[vifdump] v%llu %s (%s) overlay %d scene %d -> %s.vif/.gif, %d vsyncs\n", static_cast<unsigned long long>(tick),
                             w.scene.name, w.scene.by == DumpBy::Vsync ? "route vsync" : "detector", mode, hl_game::scene(), base.c_str(), g_vifDumpFrames);
                hl_vifdump::start(base.c_str(), w.scene.name, g_dumpDevice.c_str(), tick, g_vifDumpFrames);
                continue;
            }
            const std::string base = hl_game::g_gsDumpDir + "/" + g_dumpDevice + "_" + w.scene.name;
            std::fprintf(stderr, "[gsdump] v%llu %s (%s) overlay %d scene %d -> %s.gs.zst, %d vsyncs\n", static_cast<unsigned long long>(tick), w.scene.name,
                         w.scene.by == DumpBy::Vsync ? "route vsync" : "detector", mode, hl_game::scene(), base.c_str(), g_dumpFrames);
            if (hl_game::g_gsDump != nullptr)
                hl_game::g_gsDump(base.c_str(), g_dumpFrames);
            else
                std::fprintf(stderr, "[gsdump] libbt4gsx has no Bt4Gsx_GsDump: nothing recorded\n");
        }
    }

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_rdram == nullptr)
        {
            dumpSetup();
            g_rdram = runtime->memory().getRDRAM();
#if defined(HL_WATCH_BUILD)
            hl_watch::g_runtime = runtime;
#endif
            g_cameraOriginal = runtime->lookupFunction(kCameraMatrices);
            if (g_cameraOriginal != nullptr)
                runtime->registerFunction(kCameraMatrices, cameraMatrices);
            g_inputOriginal = runtime->lookupFunction(kFighterInput);
            if (g_inputOriginal != nullptr)
                runtime->registerFunction(kFighterInput, fighterInput);
            g_sequenceOriginal = runtime->lookupFunction(kSequenceStep);
            if (g_sequenceOriginal != nullptr)
                runtime->registerFunction(kSequenceStep, sequenceStep);
            hookOverlay();
#if defined(HL_WATCH_BUILD)
            if (const char *f = std::getenv("HL_FREECHECK"))
                g_freeCheck = std::atoi(f);
            const char *listCheck = std::getenv("HL_LISTCHECK");
            if (listCheck != nullptr && listCheck[0] == '2' && (g_listFreeOriginal = runtime->lookupFunction(kListFree)) != nullptr)
            {
                runtime->registerFunction(kListFree, listFree);
                std::fprintf(stderr, "[listfree] checking the lists 0x18DB30 frees\n");
            }
            else if (listCheck != nullptr && (g_appendOriginal = runtime->lookupFunction(kListAppend)) != nullptr)
            {
                runtime->registerFunction(kListAppend, listAppend);
                std::fprintf(stderr, "[list] checking the appends of 0x18D410\n");
            }
            if (g_freeCheck > 0 && (g_freeOriginal = runtime->lookupFunction(kHeapFree)) != nullptr)
            {
                runtime->registerFunction(kHeapFree, heapFree);
                std::fprintf(stderr, "[free] checking the game's frees (0x1181B0)%s\n", g_freeCheck >= 2 ? ", skipping the bad ones" : "");
            }
#endif
            g_padOriginal = runtime->lookupFunction(kPadRead);
            if (g_padOriginal != nullptr)
                runtime->registerFunction(kPadRead, padRead);
            if (const char *t = std::getenv("HL_SP_TAP"))
                g_testTap = std::atoi(t);
            if (const char *t = std::getenv("HL_SP_CPU"))
                g_testCpu = std::atoi(t);
            if (const char *d = std::getenv("HL_GFX_DIV"))
                g_divider = std::atoi(d);
            if (const char *w = std::getenv("HL_CAM_X"))
                hl_game::g_cameraXScale = static_cast<float>(std::atof(w));
        }
        g_tick = tick;
        dumpTick(tick);
        // HL_PRIV_LOG=1: the GS's display registers, a line each time one changes (for screens the display reads wrong)
        static const bool s_privLog = std::getenv("HL_PRIV_LOG") != nullptr;
        if (s_privLog)
        {
            const GSRegisters &r = runtime->memory().gs();
            const uint64_t now[7] = {r.pmode, r.smode2, r.dispfb1, r.display1, r.dispfb2, r.display2, r.bgcolor};
            static uint64_t s_last[7] = {~0ull, ~0ull, ~0ull, ~0ull, ~0ull, ~0ull, ~0ull};
            if (std::memcmp(now, s_last, sizeof(now)) != 0)
            {
                std::fprintf(stderr, "[priv] v%llu pmode %llx smode2 %llx dispfb1 %llx display1 %llx dispfb2 %llx display2 %llx bgcolor %llx\n",
                             static_cast<unsigned long long>(tick), static_cast<unsigned long long>(now[0]), static_cast<unsigned long long>(now[1]),
                             static_cast<unsigned long long>(now[2]), static_cast<unsigned long long>(now[3]), static_cast<unsigned long long>(now[4]),
                             static_cast<unsigned long long>(now[5]), static_cast<unsigned long long>(now[6]));
                std::memcpy(s_last, now, sizeof(now));
            }
        }
        if (hl_game::g_unlockAll.load(std::memory_order_relaxed) && tick % 50u == 0u)
            unlockEverything();
        const uint32_t gfx = read32(kGfxPointer);
        if (gfx != 0u)
        {
            static const uint64_t s_divFrom = std::getenv("HL_GFX_DIV_FROM") ? std::strtoull(std::getenv("HL_GFX_DIV_FROM"), nullptr, 10) : 0u;
            if (g_divider > 0 && tick >= s_divFrom)
                g_rdram[(gfx + 1u) & 0x1FFFFFFu] = static_cast<uint8_t>(g_divider);
            if (tick % 500u == 0u)
                std::fprintf(stderr, "[hl_game] v%llu gfx divider %u overlay %u\n", static_cast<unsigned long long>(tick), g_rdram[(gfx + 1u) & 0x1FFFFFFu], hl_game::overlay());
        }
        // the panel's pause: this thread is the EE's and everything of the game (the IOP too) runs from it
        while (hl_game::g_paused.load(std::memory_order_acquire) != 0)
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    struct Install
    {
        Install() { g_hlVsyncHook = &onVsync; }
    } g_install;
}

int hl_game::overlay()
{
    if (g_rdram == nullptr || std::memcmp(g_rdram + kOverlayBase, "MWo3", 4) != 0)
        return 0;
    const uint32_t id = read32(kOverlayBase + 4u);
    return id == 1u || id == 2u ? static_cast<int>(id) : 3;
}

int hl_game::divider()
{
    if (g_rdram == nullptr)
        return 2;
    const uint32_t gfx = read32(kGfxPointer);
    return gfx != 0u ? g_rdram[(gfx + 1u) & 0x1FFFFFFu] : 2;
}

// A fight and the campaign are what the native renderer recognizes in the GS stream (the copy of the scene's Z); which of the
// two by the mode's code in memory. Anything else (title, menus, films, loading screens) is a menu. An Ougi's film and the
// loading of an area go through the GS for a few seconds in the middle of play: those seconds stay in the scene they are in.
int hl_game::scene()
{
    if (g_rdram == nullptr)
        return 0;
    static int s_scene = 1;
    static uint64_t s_lastOn = 0;
    const uint64_t now = SDL_GetTicks();
    const int mode = overlay();
    if (s_scene == 2 && mode == 1 && ougiOnScreen())
        s_lastOn = now; // (an Ougi's film may take longer than the wait below)
    if (g_nativeOn != nullptr && g_nativeOn() && (mode == 1 || mode == 2))
    {
        s_scene = mode == 1 ? 2 : 3;
        s_lastOn = now;
    }
    else if (s_scene != 1 && (now - s_lastOn > 8000u || (s_scene == 2 ? mode != 1 : mode != 2)))
        s_scene = 1;
    return s_scene;
}
