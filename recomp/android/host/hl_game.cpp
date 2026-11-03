// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself. All addresses are of the European ELF (SLES-55605).
#include "hl_game.h"

#include <SDL3/SDL.h>

#include "ps2_runtime.h"
#include "ps2_runtime_macros.h"

#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <thread>

namespace hl_game
{
    std::atomic<int> g_paused{0};
    std::atomic<float> g_cameraXScale{1.0f};
    std::atomic<bool> g_unlockAll{false};
    std::atomic<bool> g_specialOn{true};
    std::atomic<uint32_t> g_specialMask{0x20u};
    std::atomic<int> g_mashPercent{100};
    bool (*g_nativeOn)() = nullptr;
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
            for (uint32_t i = 0; i < s_count; ++i)
                std::memcpy(rdram + ((s_cameras[i] + kCameraXScale) & 0x1FFFFFFu), &scale, 4);
            std::fprintf(stderr, "[cam] v%llu %u cameras back to 1\n", static_cast<unsigned long long>(g_tick), s_count);
            s_count = 0u;
        }
    }

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

    void fighterInput(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t f = GPR_U32(ctx, 4);
        g_inputOriginal(rdram, ctx, runtime);
        g_fighterTick = g_tick;
        special(f);
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

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_rdram == nullptr)
        {
            g_rdram = runtime->memory().getRDRAM();
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
