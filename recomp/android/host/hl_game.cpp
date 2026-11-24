// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself. All addresses are of the European ELF (SLES-55605).
#include "hl_game.h"

#include <SDL3/SDL.h>

#include "ps2_runtime.h"
#include "ps2_runtime_macros.h"
#include "runtime/hl_vif_dump.h"
#include "runtime/ee_scheduler.h"

#include <algorithm>
#include <array>
#include <utility>
#include <cctype>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/stat.h>
#include <iostream>
#include <string>
#if defined(__ANDROID__)
#include <sys/system_properties.h>
#endif
#include <thread>
#include <unordered_map>
#include <vector>

// the runtime's Pad.cpp: when fight N began, for the "@N+vsyncs" steps of HL_PADROUTE
extern uint64_t (*g_hlFightStart)(uint32_t);

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
    std::atomic<uint32_t> g_vsync{0u};
    std::atomic<bool> g_unlockAll{false};
    std::atomic<bool> g_specialOn{false};
    std::atomic<uint32_t> g_specialMask{0x20u};
    std::atomic<int> g_mashPercent{100};
    std::atomic<bool> g_menuOnline{false};
    std::atomic<int> g_onlineMenu{0};
    std::atomic<int> g_p1CharId{0};
    bool (*g_nativeOn)() = nullptr;
    void (*g_liveFight)(bool) = nullptr;
    void (*g_wideCameraOut)(bool) = nullptr;
    void (*g_gsDump)(const char *, int) = nullptr;
    std::string g_gsDumpDir;
    std::string g_vifDumpDir;
}


extern PS2Runtime::RecompiledFunction un5_btl_table[] __attribute__((weak));
extern const uint32_t un5_btl_base __attribute__((weak));
extern const uint32_t un5_btl_slots __attribute__((weak));
extern PS2Runtime::RecompiledFunction un5_adv_table[] __attribute__((weak));
extern const uint32_t un5_adv_base __attribute__((weak));
extern const uint32_t un5_adv_slots __attribute__((weak));

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
    constexpr uint32_t kRoundEndPanel = 0x617D3Cu; // gp-0x1B4: the round's end picture (the art panel after the KO), 0 otherwise

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
    uint64_t g_ougiTick = 0u;      // the last vsync an Ougi's sequence or prompt read the pad (0x36E690, 0x6C99B0)

    // The vsync each fight began: a fighter reads its input after more than HL_FIGHT_GAP vsyncs (1500) with no fighter and
    // no Ougi reading the pad. An Ougi's film stops the fighters for ~1000 vsyncs; the result, the menus and the load
    // between two fights take ~3800 (Odin 2 Mini, 2026-11-09). For the "@N+vsyncs" routes (HL_PADROUTE, HL_CPU_HP).
    std::vector<uint64_t> g_fightStarts;
    uint64_t fightStart(uint32_t n)
    {
        return n >= 1u && n <= g_fightStarts.size() ? g_fightStarts[n - 1u] : 0u;
    }
    void noteFightStart()
    {
        static const uint64_t s_gap = std::getenv("HL_FIGHT_GAP") ? std::strtoull(std::getenv("HL_FIGHT_GAP"), nullptr, 10) : 1500u;
        const uint64_t live = g_fighterTick > g_ougiTick ? g_fighterTick : g_ougiTick;
        if (live != 0u && g_tick - live <= s_gap)
            return;
        g_fightStarts.push_back(g_tick);
        std::fprintf(stderr, "[route] v%llu fight %zu begins\n", static_cast<unsigned long long>(g_tick), g_fightStarts.size());
    }

    // The wide projection is for the 3D of a fight (while the fighters read their input, and an Ougi's film) and of the
    // campaign. Everything else drawn through a camera (the result screen's picture, menus) is 2D made for 4:3: squeezed, it
    // leaves the sides of the screen with nothing drawn on them. The result screen builds its matrices once, in the vsyncs
    // right after the fight: hence the short wait (6 vsyncs, the fighters read their input every 2).
    // The round's end picture (the art panel after the KO and the victory pose) is a full-screen plane drawn through the
    // fight's camera while the fighters still read their input: made wide, it came out at 60% of the width with black
    // bars on the sides in 16:9 and 20:9 (a few frames after the first round, ~80 after the last; docs/registro.md
    // 2026-11-13 15:40). Its object at gp-0x1B4 is set 5 vsyncs before it shows and cleared when it ends.
    bool ougiOnScreen();
    bool wideCamera()
    {
        const int mode = hl_game::overlay();
        if (mode == 1 && read32(kRoundEndPanel) != 0u)
            return false;
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
        static const bool s_camLog = std::getenv("HL_CAM_LOG") != nullptr;
        if (s_camLog)
        {
            static int s_n = 0;
            float before;
            std::memcpy(&before, rdram + ((camera + kCameraXScale) & 0x1FFFFFFu), 4);
            if (s_n < 40 && before != scale)
            {
                ++s_n;
                std::fprintf(stderr, "[cam] v%llu camera 0x%x +0x274 was %g, now %g\n", static_cast<unsigned long long>(g_tick), camera, before, scale);
            }
        }
        // Outside the wide scenes the game's own scale stays: 1.0 in the base game, 0.75 in the UN6 A35 (its anamorphic 16:9,
        // kept by its code table). Writing 1.0 there gave the A35's title a background 4/3 wider than on PCSX2
        // (docs/registro.md, 2026-11-09). Only the cameras made wide get their own value back, below.
        // (the cameras the fight made wide, with the game's value: a picture may be built later from one of them without
        // coming back here)
        static uint32_t s_cameras[16];
        static float s_own[16];
        static uint32_t s_count = 0u;
        if (scale != 1.0f)
        {
            bool known = false;
            for (uint32_t i = 0; i < s_count; ++i)
                known = known || s_cameras[i] == camera;
            if (!known && s_count < 16u)
            {
                // A camera the game made during the fight may not have its scale yet (0, or what the block held before:
                // 1.7e-38, 5e+33...): given back at the end, a scale of 0 left the victory screen black in 16:9 and
                // 20:9 (only the 2D name drawn; docs/registro.md, 2026-11-10). What is not a scale is the base game's 1.0.
                float own;
                std::memcpy(&own, rdram + ((camera + kCameraXScale) & 0x1FFFFFFu), 4);
                if (!(own >= 0.25f && own <= 4.0f))
                    own = 1.0f;
                s_own[s_count] = own;
                s_cameras[s_count++] = camera;
                if (s_camLog)
                    std::fprintf(stderr, "[cam] v%llu camera 0x%x made wide, its own scale %g\n", static_cast<unsigned long long>(g_tick), camera, own);
            }
            std::memcpy(rdram + ((camera + kCameraXScale) & 0x1FFFFFFu), &scale, 4);
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
                    std::memcpy(field, &s_own[i], 4);
                    ++back;
                }
            }
            std::fprintf(stderr, "[cam] v%llu %u of %u cameras back to the game's scale (%u cameras live)\n", static_cast<unsigned long long>(g_tick), back,
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

    // HL_CAM_LOG: who sets a camera's horizontal scale (0x10EED0: +0x274 = f12), with the caller
    PS2Runtime::RecompiledFunction g_camSetOriginal = nullptr;
    void cameraSetScale(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        static int s_n = 0;
        if (s_n < 60)
        {
            ++s_n;
            std::fprintf(stderr, "[camset] v%llu camera 0x%x scale %g ra 0x%x, init called from 0x%x\n", static_cast<unsigned long long>(g_tick),
                         GPR_U32(ctx, 4), ctx->f[12], GPR_U32(ctx, 31), read32(GPR_U32(ctx, 29) + 0x40u));
        }
        g_camSetOriginal(rdram, ctx, runtime);
    }

    // The game's pool, at the address newlib gives on the PS2. At boot (0x118CA0) the game asks malloc for all the RAM
    // from its ELF's end to its stack (0x118D34, then 0x100 less each time until it gets it: 0x118D50) and keeps its own heap
    // in it, bottom up. newlib gives end + 0x10 (PCSX2: 0x928910 for the base, 0xA28910 for the UN6 A35); the runtime's
    // heap gives end, so every object of the game sat 0x10 lower than on the PS2. The UN6 A35's code table writes absolute
    // addresses (the main camera's x scale at 0xC3EF14) and hit the wrong field (docs/registro.md, 2026-11-09). The pool
    // is 0x10 further up, as on the PS2. (It is still ~1 MB smaller: the runtime keeps 0x1F00000 and up for its callback
    // stacks.) HL_CAM_LOG prints the mallocs.
    PS2Runtime::RecompiledFunction g_mallocOriginal = nullptr;
    void gameMalloc(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t size = GPR_U32(ctx, 4), ra = GPR_U32(ctx, 31);
        const bool pool = ra == 0x118D3Cu || ra == 0x118D58u;
        if (pool)
            SET_GPR_U32(ctx, 4, size + 0x10u);
        g_mallocOriginal(rdram, ctx, runtime);
        if (pool && GPR_U32(ctx, 2) != 0u)
            SET_GPR_U32(ctx, 2, GPR_U32(ctx, 2) + 0x10u);
        static const bool s_log = std::getenv("HL_CAM_LOG") != nullptr;
        static int s_n = 0;
        if (s_log && (s_n++ < 4 || GPR_U32(ctx, 2) != 0u))
            std::fprintf(stderr, "[malloc] v%llu size 0x%x ra 0x%x -> 0x%x\n", static_cast<unsigned long long>(g_tick), size, ra, GPR_U32(ctx, 2));
    }

    // A mod's code table in sceSifSendCmd. The UN6 A35 calls 0x17B1E4 at 0x161E10 in place of _sceSifSendCmd: a small engine
    // (lui/ori $t4 = its header) that writes a table of (address << 7 | kind, value) at each SIF command, then goes on to
    // _sceSifSendCmd. The header is (table << 7 | entries - 1); the kind is how far back from the header its store is
    // (sb, sh or sw, alone or in a branch's delay slot). Its one entry keeps the main camera's x scale at 0.75 (0xC3EF14),
    // after 0x1843D0 sets 1.0. Here sceSifSendCmd runs as the mod's code (tools/un5.py stubfix), but the RPCs are the
    // runtime's and never reach it: the table is written before each sceSifCallRpc and sceSifBindRpc, as on the PS2.
    constexpr uint32_t kSifSendCmdCall = 0x161E10u, kSifSendCmdOriginal = 0x161CB0u;
    uint32_t g_codeHeader = 0u; // 0: not looked at yet; 1: none
    void modCodeTable()
    {
        if (g_codeHeader == 0u)
        {
            g_codeHeader = 1u;
            const uint32_t jal = read32(kSifSendCmdCall);
            const uint32_t target = (jal & 0x03FFFFFFu) << 2;
            if ((jal >> 26) == 3u && target != kSifSendCmdOriginal && (read32(target) & 0xFFFF0000u) == 0x3C0C0000u &&
                (read32(target + 4u) & 0xFFFF0000u) == 0x358C0000u)
            {
                g_codeHeader = (read32(target) << 16) | (read32(target + 4u) & 0xFFFFu);
                std::fprintf(stderr, "[codes] the mod's code table at 0x%x (engine 0x%x)\n", g_codeHeader, target);
            }
        }
        if (g_codeHeader == 1u)
            return;
        const uint32_t header = read32(g_codeHeader);
        uint32_t entry = (header >> 7) & 0x1FFFFFFu;
        for (uint32_t n = (header & 0x7Fu) + 1u; n != 0u; --n, entry += 8u)
        {
            const uint32_t where = read32(entry), value = read32(entry + 4u);
            uint32_t store = read32(g_codeHeader - (where & 0x7Fu));
            if ((store & 0xFFFF0000u) == 0x10000000u) // b: the store is in its delay slot
                store = read32(g_codeHeader - (where & 0x7Fu) + 4u);
            uint8_t *p = g_rdram + ((where >> 7) & 0x1FFFFFFu);
            switch (store >> 26)
            {
            case 0x28: *p = static_cast<uint8_t>(value); break;
            case 0x29: { const uint16_t v = static_cast<uint16_t>(value); std::memcpy(p, &v, 2); break; }
            case 0x2B: std::memcpy(p, &value, 4); break;
            default: break;
            }
        }
    }
    PS2Runtime::RecompiledFunction g_callRpcOriginal = nullptr, g_bindRpcOriginal = nullptr;
    void sifCallRpc(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        modCodeTable();
        g_callRpcOriginal(rdram, ctx, runtime);
    }
    void sifBindRpc(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        modCodeTable();
        g_bindRpcOriginal(rdram, ctx, runtime);
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
        // 0x909..0x967: characters (3 = unlocked + seen); 0x968..0x96F (save+8+0x960): 64-bit starting transformations / awakenings bitset!
        std::memset(state + kSaveRoster, 3, 0x968u - kSaveRoster);
        std::memset(state + 0x968u, 0xFF, 8u);
        // 0x970..0x98F (save+8+0x968): stages & extras
        std::memset(state + 0x970u, 3, 0x20u);
        for (uint32_t i = kSaveTables; i < kSaveTablesEnd; ++i)
            if (state[i] < 3u)
                state[i] = 3u;
        // 0xDF4..0xDF7 (save+8+0xDEC): 32-bit extra unlocks bitset; 0x130C..0x138B (save+8+0x1304): jutsus/items
        std::memset(state + 0xDF4u, 0xFF, 4u);
        for (uint32_t i = 0x130Cu; i < 0x138Cu; ++i)
            if (state[i] < 3u)
                state[i] = 3u;
        // 0x34: max Ryo (99,999,999 = 0x05F5E0FF)
        const uint32_t maxRyo = 99999999u;
        std::memcpy(state + 0x34u, &maxRyo, 4);
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
    constexpr uint32_t kMoveOugi = 0x00F00000u; // (0xF0000 the jutsu, 0xF00000 the ougi)
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
    uint64_t g_padSpecialTick[2] = {}; // vsync of the last latch of g_padSpecialDown
    uint64_t g_padDirTick[2] = {};     // vsync of the last directional input (avoids hijacking ↑↑○ / ↓↓○)
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
        for (int i = 0; i < 4; ++i)
            g_sequence[i] = Sequence{0u, -1, 0, 0.0f};
        g_fighters[0] = f;
        g_sequence[0] = Sequence{f, -1, 0, 0.0f};
        return 0;
    }

    inline bool validAddress(uint32_t a) { return a >= 0x100000u && a < 0x1F00000u; }

    inline bool humanFighter(uint32_t f) { return ((read16(f + kFighterKind) >> 5) & 0xFu) == 0u; }

    // An Ougi on the screen, or a dispute: from then on the taps answer the game's prompts. Only while the fight lives
    // (a fighter read its input or an Ougi's prompt read the pad in the last 0.5 s) and not on the round-end panel.
    bool ougiOnScreen()
    {
        if (hl_game::overlay() != 1)
            return false;
        if (read32(kRoundEndPanel) != 0u)
            return false;
        const uint64_t live = g_fighterTick > g_ougiTick ? g_fighterTick : g_ougiTick;
        if (live == 0u || g_tick - live > 25u)
            return false;
        for (int i = 0; i < 4; ++i)
        {
            const uint32_t f = g_fighters[i];
            if (!validAddress(f))
                continue;
            if ((read32(f + kFighterDispute) & 0xFF00u) != 0u)
                return true;
            const uint32_t move = read32(f + kFighterMove);
            if (read16(f + kFighterAction) == 8u && validAddress(move) && (read32(move + 0x10u) & kMoveOugi) != 0u)
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
        g_ougiTick = g_tick;
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
        // HL_SP_LOG: the button the sequence wants now from each pad, what the screen shows (for a test player that reads it)
        static const bool s_seqLog = std::getenv("HL_SP_LOG") != nullptr;
        static int s_lastWant[2] = {-2, -2};
        for (uint32_t p = 0; s_seqLog && p < 2u; ++p)
        {
            const int16_t at = static_cast<int16_t>(read16(obj + 0x312u + p * 2u));
            const int code = at >= 0 && at < 0x100 ? static_cast<int8_t>(rdram[(obj + 0x112u + (p << 8) + static_cast<uint32_t>(at)) & 0x1FFFFFFu]) : -1;
            const int want = rdram[(obj + 0x1Au + p) & 0x1FFFFFFu] == 0u ? (at << 4) | (code & 15) : -1;
            if (want != s_lastWant[p])
            {
                s_lastWant[p] = want;
                if (want >= 0 && code >= 0 && code < 8)
                    std::fprintf(stderr, "[sp] v%llu pad %u step %d wants %d\n", static_cast<unsigned long long>(g_tick), p, static_cast<int>(at), code);
            }
        }
        for (uint32_t p = 0; on && p < 2u; ++p)
            std::memcpy(rdram + gfx + 0x1Cu + p * 0x78u + kPadPressed, &saved[p], 4);
    }

    PS2Runtime::RecompiledFunction g_promptOriginal = nullptr;
    void promptPressed(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t obj = GPR_U32(ctx, 4);
        g_ougiTick = g_tick;
        g_promptOriginal(rdram, ctx, runtime);
        const uint32_t pad = read16(obj + 0x28u);
        if (GPR_U32(ctx, 2) == 0u && pad < 2u && g_padSpecialDown[pad] != 0u && rdram[(obj + 0x2Eu) & 0x1FFFFFFu] != 1u && hl_game::g_specialOn.load(std::memory_order_relaxed))
            SET_GPR_U32(ctx, 2, 1u);
    }

    // HL_FNCOUNT=ADDR,... or all (hex, ADV.BIN's functions, debugging): how many times each ran, a line every 50 vsyncs
    // ("[fn]"), for finding which of them run in one state of the game and not in another (docs/registro.md 2026-11-09).
    // HL_FNCOUNT_WIN=A0:A1,B0:B1 (vsyncs) instead: the functions that ran only in window A or only in window B, at B1.
    // HL_FNCOUNT=all:btl or all:elf counts BTL.BIN's functions or the ELF's instead (the menus run with BTL.BIN in memory,
    // docs/menu-online.md). HL_FNCHG=ADDR (hex): the function that changed this 32-bit word ("[fnchg]", the innermost of the
    // counted ones: it returns first), a write watch without gdb.
    constexpr int kCountMax = 16384;
    PS2Runtime::RecompiledFunction g_countOriginal[kCountMax] = {};
    uint32_t g_countAddress[kCountMax] = {}, g_countCalls[kCountMax] = {};
    int g_countUsed = 0;
    uint32_t g_chgAddress = 0u, g_chgLast = 0u;
    template <int I>
    void countCall(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        ++g_countCalls[I];
        const uint32_t ra = GPR_U32(ctx, 31);
        g_countOriginal[I](rdram, ctx, runtime);
        if (g_chgAddress != 0u)
        {
            uint32_t now;
            std::memcpy(&now, rdram + g_chgAddress, 4);
            if (now != g_chgLast)
            {
                std::fprintf(stderr, "[fnchg] v%llu fn %x (ra %x): %08x -> %08x\n", static_cast<unsigned long long>(g_tick), g_countAddress[I], ra, g_chgLast, now);
                g_chgLast = now;
            }
        }
    }
    template <int... I>
    constexpr std::array<PS2Runtime::RecompiledFunction, sizeof...(I)> countCalls(std::integer_sequence<int, I...>) { return {&countCall<I>...}; }
    void hookCounts()
    {
        static bool s_done = false;
        const char *list = std::getenv("HL_FNCOUNT");
        if (s_done || list == nullptr || &un5_adv_table == nullptr)
            return;
        s_done = true;
        static constexpr auto kCalls = countCalls(std::make_integer_sequence<int, kCountMax>{});
        if (const char *c = std::getenv("HL_FNCHG"))
            g_chgAddress = static_cast<uint32_t>(std::strtoul(c, nullptr, 16)) & 0x1FFFFFFu;
        const bool btl = std::strstr(list, "btl") != nullptr && &un5_btl_table != nullptr;
        const bool elf = std::strstr(list, "elf") != nullptr;
        if (btl || elf)
        {
            // (a function's every entry point shares one slot each: one counter per function, by its pointer)
            std::unordered_map<PS2Runtime::RecompiledFunction, int> seen;
            auto wrap = [&](PS2Runtime::RecompiledFunction *table, uint32_t base, uint32_t slots) {
                for (uint32_t slot = 0; slot < slots; ++slot)
                    if (table[slot] != nullptr)
                    {
                        auto [it, fresh] = seen.try_emplace(table[slot], g_countUsed);
                        if (fresh)
                        {
                            if (g_countUsed >= kCountMax)
                                break;
                            g_countOriginal[g_countUsed] = table[slot];
                            g_countAddress[g_countUsed] = base + slot * 4u;
                            ++g_countUsed;
                        }
                        table[slot] = kCalls[it->second];
                    }
            };
            if (btl)
                wrap(un5_btl_table, un5_btl_base, un5_btl_slots);
            if (elf)
                wrap(g_ps2RecompiledFunctionTable, g_ps2RecompiledFunctionTableBase, g_ps2RecompiledFunctionTableSlotCount);
            std::fprintf(stderr, "[fn] counting %d functions (%s%s)\n", g_countUsed, btl ? "BTL " : "", elf ? "ELF" : "");
            return;
        }
        if (std::strcmp(list, "all") == 0)
        {
            for (uint32_t slot = 0; slot < un5_adv_slots && g_countUsed < kCountMax; ++slot)
                if (un5_adv_table[slot] != nullptr)
                {
                    g_countOriginal[g_countUsed] = un5_adv_table[slot];
                    g_countAddress[g_countUsed] = un5_adv_base + slot * 4u;
                    un5_adv_table[slot] = kCalls[g_countUsed];
                    ++g_countUsed;
                }
            list = "";
        }
        for (const char *p = list; *p && g_countUsed < kCountMax;)
        {
            char *e;
            const uint32_t addr = static_cast<uint32_t>(std::strtoul(p, &e, 16));
            p = *e ? e + 1 : e;
            const uint32_t slot = (addr - un5_adv_base) >> 2;
            if (addr < un5_adv_base || slot >= un5_adv_slots || un5_adv_table[slot] == nullptr)
            {
                std::fprintf(stderr, "[fn] %x is not an ADV function\n", addr);
                continue;
            }
            g_countOriginal[g_countUsed] = un5_adv_table[slot];
            g_countAddress[g_countUsed] = addr;
            un5_adv_table[slot] = kCalls[g_countUsed];
            ++g_countUsed;
        }
        std::fprintf(stderr, "[fn] counting %d ADV functions\n", g_countUsed);
    }
    void logCounts(uint64_t tick)
    {
        if (g_countUsed == 0)
            return;
        static uint64_t s_win[4] = {};
        static const bool s_windows = [] {
            const char *w = std::getenv("HL_FNCOUNT_WIN");
            unsigned long long v[4];
            if (w == nullptr || std::sscanf(w, "%llu:%llu,%llu:%llu", &v[0], &v[1], &v[2], &v[3]) != 4)
                return false;
            for (int i = 0; i < 4; ++i)
                s_win[i] = v[i];
            return true;
        }();
        if (s_windows)
        {
            static uint32_t s_a[kCountMax];
            if (tick == s_win[0] || tick == s_win[2])
                std::fill(g_countCalls, g_countCalls + g_countUsed, 0u);
            else if (tick == s_win[1])
                std::copy(g_countCalls, g_countCalls + g_countUsed, s_a);
            else if (tick == s_win[3])
            {
                for (int i = 0; i < g_countUsed; ++i)
                    if ((s_a[i] == 0u) != (g_countCalls[i] == 0u))
                        std::fprintf(stderr, "[fn] only %s %x: %u calls\n", s_a[i] != 0u ? "A" : "B", g_countAddress[i], s_a[i] + g_countCalls[i]);
                std::fprintf(stderr, "[fn] windows done\n");
            }
            return;
        }
        if (tick % 50u != 0u)
            return;
        std::string line;
        for (int i = 0; i < g_countUsed; ++i)
            if (g_countCalls[i] != 0u)
            {
                char item[32];
                std::snprintf(item, sizeof(item), " %x:%u", g_countAddress[i], g_countCalls[i]);
                line += item;
                g_countCalls[i] = 0u;
            }
        std::fprintf(stderr, "[fn] v%llu%s\n", static_cast<unsigned long long>(tick), line.c_str());
    }

    // ADV.BIN (the campaign): 0x723970 runs once a frame of the game's logic while the player can move (standing or walking)
    // and never while a conversation, its tutorials or a cut scene hold the player; ten more do the same (0x723C20, 0x755A20,
    // 0x7B9630...). Found with HL_FNCOUNT_WIN over every ADV function: dialog v6800-7600 against standing (v8300-8900) and
    // walking (v8200-8800) on the campaign route (docs/registro.md 2026-11-09).
    constexpr uint32_t kAdvFree = 0x723970u;
    // It does not run in every area: in the Hokage's room (after "Sakura has joined your team") the player walks and it never
    // runs, so the room's controls stayed the menu's and the player could not move (docs/registro.md 2026-11-11). The area is
    // counted by the native renderer's scene going on (each loading of an area goes through the GS and turns it off), and the
    // conversation's controls only come in an area where the function has run since.
    PS2Runtime::RecompiledFunction g_advFreeOriginal = nullptr;
    uint64_t g_advFreeTick = 0;
    std::atomic<uint32_t> g_area{0u}, g_advFreeArea{~0u};
    void advFree(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        g_advFreeTick = g_tick;
        g_advFreeArea.store(g_area.load(std::memory_order_relaxed), std::memory_order_relaxed);
        g_advFreeOriginal(rdram, ctx, runtime);
    }
    // The other side: 0x7F3F60 runs once a frame of the game's logic while a conversation's box is up and never while the
    // player can move ("only A", 400 calls in 800 vsyncs of dialog, in both 11-09 runs). It catches the conversations at the
    // start of an area, before 0x723970 has run there (Tsunade's, as one enters her room). The same code in the A35 and the
    // Alpha 1.1X (whose ADV.BIN is the base's).
    constexpr uint32_t kAdvTalk = 0x7F3F60u;
    PS2Runtime::RecompiledFunction g_advTalkOriginal = nullptr;
    uint64_t g_advTalkTick = 0;
    void advTalk(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        g_advTalkTick = g_tick;
        g_advTalkOriginal(rdram, ctx, runtime);
    }

    void hookAdv(uint32_t address, PS2Runtime::RecompiledFunction &original, PS2Runtime::RecompiledFunction hook)
    {
        if (&un5_adv_table == nullptr || original != nullptr || address < un5_adv_base)
            return;
        const uint32_t slot = (address - un5_adv_base) >> 2;
        if (slot < un5_adv_slots && un5_adv_table[slot] != nullptr)
        {
            original = un5_adv_table[slot];
            un5_adv_table[slot] = hook;
        }
    }

    // BTL.BIN's functions are in its own table (recomp/overlays.py), which answers only while it is the one in memory.
    void hookOverlay()
    {
        hookAdv(kAdvFree, g_advFreeOriginal, advFree);
        hookAdv(kAdvTalk, g_advTalkOriginal, advTalk);
        if (&un5_btl_table == nullptr || g_promptOriginal != nullptr)
            return;
        const uint32_t slot = (kPromptPressed - un5_btl_base) >> 2;
        if (slot < un5_btl_slots && un5_btl_table[slot] != nullptr)
        {
            g_promptOriginal = un5_btl_table[slot];
            un5_btl_table[slot] = promptPressed;
        }
    }

    // "Naruto Online" in the game's Mode Select (docs/menu-online.md). The menu is the ELF's (0x3953B0 builds it, 0x396B90
    // runs it, 0x3972E0 draws it), with an object whose +0x04 is the count of entries, +0x08 their mode ids (up to 7), +0x24
    // the cursor. 0x395CE0 fills the ids with every mode whose word in 0x5DC300 is not negative, in id order: 0 Master Mode,
    // 1 Free Battle, 2 Practice, 4 Shop, 5 Collection, 6 Options. Mode 3 is the Mini-Games of the original, cut from the
    // European game (its word is -1) but with all its pieces still in the menu's file: a board (with no name on it), its
    // diorama and its line of text. We put it right under Free Battle and give it our own text; choosing it is the game's own
    // path for a mode that is not there (0x395F20, state 5, the message box with message 4, empty in this version), where
    // we write our message and tell the launcher (hl_game::g_onlineMenu).
    constexpr uint32_t kMsBuildList = 0x395CE0u, kMsUnavailable = 0x395F20u, kModeText = 0x3D1850u, kMenuText = 0x3D18A0u;
    constexpr uint32_t kPlaySound = 0x1DD470u;
    constexpr uint32_t kMsCount = 0x04u, kMsIds = 0x08u, kMsCursor = 0x24u, kMsMax = 7u;
    constexpr uint32_t kOnlineMode = 3u, kUnavailableText = 4u;
    constexpr uint32_t kSoundRefused = 0x3Fu, kSoundChosen = 0x58u;
    constexpr uint32_t kTextOverlay = 0x8F3D00u; // TEXT*.BIN: id 4 English ... 8 Spanish (+4 of the MWo3 header)
    // Our strings, in RAM no one uses: under the ELF (0x100000), above the runtime's pools (0xA0000-0xD1000), all zero the
    // first time (checked)
    constexpr uint32_t kOnlineStrings = 0xFE000u, kOnlineStringMax = 0x100u;
    std::atomic<bool> g_menuOnlineEnv{std::getenv("HL_MENU_ONLINE") != nullptr && std::getenv("HL_MENU_ONLINE")[0] == '1'};
    PS2Runtime::RecompiledFunction g_msBuildOriginal = nullptr, g_msUnavailableOriginal = nullptr, g_modeTextOriginal = nullptr,
                                   g_menuTextOriginal = nullptr, g_playSoundOriginal = nullptr;
    bool g_onlineSound = false;
    uint64_t g_onlineSoundTick = 0u;
    int g_onlineStringsOk = -1;
    std::atomic<uint32_t> g_modeSelectObj{0u};
    std::atomic<uint64_t> g_modeSelectTick{0u};

    bool menuOnline()
    {
        return g_menuOnlineEnv.load(std::memory_order_relaxed) || hl_game::g_menuOnline.load(std::memory_order_relaxed);
    }
    int textLanguage()
    {
        if (std::memcmp(g_rdram + kTextOverlay, "MWo3", 4) != 0)
            return 0;
        const uint32_t id = read32(kTextOverlay + 4u);
        return id >= 4u && id <= 8u ? static_cast<int>(id - 4u) : 0;
    }
    // (ASCII only: the game's markup, <colorRRGGBB> <WHITE> <BLACK> <br>, and nothing else)
    const char *const kOnlineDescription[5] = {
        "<color00FFFF>Naruto Online<WHITE>: fight other players over the internet.",
        "<color00FFFF>Naruto Online<WHITE> : combattez d'autres joueurs sur Internet.",
        "<color00FFFF>Naruto Online<WHITE>: kaempfe gegen andere Spieler im Internet.",
        "<color00FFFF>Naruto Online<WHITE>: sfida altri giocatori su Internet.",
        "<color00FFFF>Naruto Online<WHITE>: lucha contra otros jugadores por Internet.",
    };
    const char *const kOnlineMessage[5] = {
        "<color080860>Naruto Online<BLACK><br>Quick Match, Create Room or Join Room.",
        "<color080860>Naruto Online<BLACK><br>Partie rapide, creer ou rejoindre une salle.",
        "<color080860>Naruto Online<BLACK><br>Schnelles Spiel, Raum erstellen oder beitreten.",
        "<color080860>Naruto Online<BLACK><br>Partita rapida, crea o entra in una stanza.",
        "<color080860>Naruto Online<BLACK><br>Partida rapida, crear o unirse a una sala.",
    };
    // writes one of our strings into slot 0 or 1 of kOnlineStrings; 0 if that RAM was not ours to take
    uint32_t onlineString(int slot, const char *text)
    {
        const uint32_t at = kOnlineStrings + static_cast<uint32_t>(slot) * kOnlineStringMax;
        if (g_onlineStringsOk < 0)
        {
            const uint8_t *p = g_rdram + kOnlineStrings;
            g_onlineStringsOk = std::all_of(p, p + 2u * kOnlineStringMax, [](uint8_t b) { return b == 0u; }) ? 1 : 0;
            std::fprintf(stderr, "[online] strings at %x: %s\n", kOnlineStrings, g_onlineStringsOk ? "free" : "in use, menu text left alone");
        }
        if (g_onlineStringsOk == 0)
            return 0u;
        const size_t n = std::min(std::strlen(text), static_cast<size_t>(kOnlineStringMax - 1u));
        std::memcpy(g_rdram + at, text, n);
        std::memset(g_rdram + at + n, 0, kOnlineStringMax - n);
        return at;
    }
    uint32_t msEntry(uint32_t menu, uint32_t i) { return read32(menu + kMsIds + i * 4u); }

    // The board's name: the boards' names are one texture of the menu's file (TEX_modesel02, 256x512, 8 bits, upside down:
    // raw row 0 is the bottom on screen), one band of 48 rows each, from the bottom: Master Mode, Free Battle, Practice,
    // (Mini-Games: empty in this version), Shop, Collection, Options. "Online" goes into the empty band, made of the letters
    // of the other names (the game's own lettering, cut and put together here; nothing of it is in our code): O of Options,
    // n l i e of Collection and Free Battle. Each piece: its band, its columns [a, b) at the band's raw row 0 (they move right
    // by 0.15 per row up, as the lettering leans), its raw rows [r0, r1). Tuned on the PC (build/lab/menu, docs/menu-online.md).
    struct LabelPiece
    {
        uint16_t band, a, b, r0, r1;
    };
    constexpr uint32_t kLabelOptions = 176u, kLabelCollection = 224u, kLabelEmpty = 320u, kLabelFree = 416u, kLabelRows = 48u;
    constexpr uint32_t kLabelWidth = 256u, kLabelBytes = 256u * 512u;
    constexpr LabelPiece kOnlineLabel[][2] = {
        {{kLabelOptions, 50, 88, 0, 48}, {}},                              // O
        {{kLabelCollection, 200, 232, 0, 48}, {}},                         // n
        {{kLabelCollection, 86, 100, 0, 48}, {}},                          // l
        {{kLabelCollection, 86, 100, 0, 27}, {kLabelCollection, 170, 184, 33, 46}}, // i: the l's stroke under the x-height, the i's dot
        {{kLabelCollection, 200, 232, 0, 48}, {}},                         // n
        {{kLabelFree, 217, 248, 0, 48}, {}},                               // e
    };
    // the texture's pixels: its name in the file's table of names, +0x2C its object, +0x28 the upload's block, +4 the pixels,
    // +8 their size in quadwords
    uint32_t labelTexture()
    {
        static const char kName[] = "TEX_modesel02";
        const uint8_t *ram = g_rdram;
        for (const uint8_t *p = ram + 0x100000u; (p = static_cast<const uint8_t *>(memmem(p, (ram + (32u << 20)) - p, kName, sizeof(kName)))) != nullptr; ++p)
        {
            const uint32_t entry = static_cast<uint32_t>(p - ram);
            if ((entry & 7u) != 0u)
                continue;
            const uint32_t object = read32(entry + 0x2Cu);
            if (object < 0x100000u || object >= (32u << 20) || read32(object) != entry)
                continue;
            const uint32_t upload = read32(object + 0x28u);
            if (upload < 0x100000u || upload >= (32u << 20))
                continue;
            const uint32_t pixels = read32(upload + 4u);
            if (read32(upload + 8u) == kLabelBytes / 16u && pixels >= 0x100000u && pixels + kLabelBytes <= (32u << 20))
                return pixels;
        }
        return 0u;
    }
    void writeOnlineLabel()
    {
        const uint32_t pixels = labelTexture();
        if (pixels == 0u)
        {
            std::fprintf(stderr, "[online] the boards' names (TEX_modesel02) not found: the board stays empty\n");
            return;
        }
        uint8_t *tex = g_rdram + pixels;
        uint8_t *empty = tex + kLabelEmpty * kLabelWidth;
        if (!std::all_of(empty, empty + kLabelRows * kLabelWidth, [](uint8_t b) { return b == 0u; }))
            return; // (already written, or a version with its own name there)
        auto shadow = [](uint8_t v) { return v >= 28u && v <= 33u; }; // (the indices of the dark outline around the letters)
        int width = 0;
        for (const auto &letter : kOnlineLabel)
            width += letter[0].b - letter[0].a - 2;
        int x = (static_cast<int>(kLabelWidth) - width) / 2;
        for (const auto &letter : kOnlineLabel)
        {
            for (const LabelPiece &piece : letter)
                for (uint32_t row = piece.r0; row < piece.r1; ++row)
                {
                    const int shift = static_cast<int>(0.15f * static_cast<float>(row) + 0.5f);
                    const uint8_t *src = tex + (piece.band + row) * kLabelWidth;
                    uint8_t *dst = empty + row * kLabelWidth;
                    for (int c = piece.a; c < piece.b; ++c)
                    {
                        const int sx = c + shift, tx = x + (c - piece.a) + shift;
                        if (sx >= static_cast<int>(kLabelWidth) || tx < 0 || tx >= static_cast<int>(kLabelWidth) || src[sx] == 0u)
                            continue;
                        if (dst[tx] == 0u || (shadow(dst[tx]) && !shadow(src[sx])))
                            dst[tx] = src[sx];
                    }
                }
            x += letter[0].b - letter[0].a - 2;
        }
        std::fprintf(stderr, "[online] board name written into %x\n", pixels);
    }

    void msBuildList(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t menu = GPR_U32(ctx, 4);
        g_msBuildOriginal(rdram, ctx, runtime);
        g_modeSelectObj.store(menu, std::memory_order_relaxed);
        g_modeSelectTick.store(g_tick, std::memory_order_relaxed);
        if (!menuOnline())
            return;
        const uint32_t count = read32(menu + kMsCount);
        if (count >= kMsMax)
            return;
        uint32_t at = count; // right under Free Battle (mode 1); at the end if it is not there
        for (uint32_t i = 0; i < count; ++i)
        {
            if (msEntry(menu, i) == kOnlineMode)
                return;
            if (msEntry(menu, i) == 1u)
                at = i + 1u;
        }
        for (uint32_t i = count; i > at; --i)
        {
            const uint32_t v = msEntry(menu, i - 1u);
            std::memcpy(rdram + ((menu + kMsIds + i * 4u) & 0x1FFFFFFu), &v, 4);
        }
        std::memcpy(rdram + ((menu + kMsIds + at * 4u) & 0x1FFFFFFu), &kOnlineMode, 4);
        const uint32_t n = count + 1u;
        std::memcpy(rdram + ((menu + kMsCount) & 0x1FFFFFFu), &n, 4);
        std::fprintf(stderr, "[online] Mode Select at %x: Naruto Online is entry %u of %u\n", menu, at, n);
        writeOnlineLabel();
    }
    // 0x395F20(menu, 1): the mode under the cursor is not there; with ours under the cursor, it is "Naruto Online chosen"
    void msUnavailable(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t menu = GPR_U32(ctx, 4);
        const uint32_t cursor = read32(menu + kMsCursor);
        const bool ours = menuOnline() && GPR_U32(ctx, 5) == 1u && cursor < kMsMax && msEntry(menu, cursor) == kOnlineMode;
        if (ours)
        {
            std::fprintf(stderr, "[online] menu chosen (v%llu)\n", static_cast<unsigned long long>(g_tick));
            hl_game::g_onlineMenu.fetch_add(1, std::memory_order_relaxed);
            if (hl_game::g_menuOnline.load(std::memory_order_relaxed))
            {   // (the developer, 2026-11-14: on Android the app's online panel opens over the Mode Select, and the game's message
                // box showed through it. No box there: the Mode Select stays as it is behind the panel, with the game's "chosen"
                // sound, the one argument of 0x1DD470; the EE's registers as they were for the caller)
                const R5900Context saved = *ctx;
                SET_GPR_U32(ctx, 4, kSoundChosen);
                g_playSoundOriginal(rdram, ctx, runtime);
                *ctx = saved;
                return;
            }
            g_onlineSound = true; // the game's "chosen" sound instead of its "refused" one
            g_onlineSoundTick = g_tick;
        }
        g_msUnavailableOriginal(rdram, ctx, runtime);
        g_onlineSound = false;
    }
    void playSound(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        if (g_onlineSound && g_onlineSoundTick == g_tick && GPR_U32(ctx, 4) == kSoundRefused)
            SET_GPR_U32(ctx, 4, kSoundChosen);
        g_playSoundOriginal(rdram, ctx, runtime);
    }
    // the line of text under the boards (by mode id) and the text of the menu's message box (by message)
    void modeText(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        if (menuOnline() && GPR_U32(ctx, 4) == kOnlineMode)
            if (const uint32_t at = onlineString(0, kOnlineDescription[textLanguage()]))
            {
                SET_GPR_U32(ctx, 2, at);
                ctx->pc = GPR_U32(ctx, 31);
                return;
            }
        g_modeTextOriginal(rdram, ctx, runtime);
    }
    void menuText(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        if (menuOnline() && GPR_U32(ctx, 4) == kUnavailableText)
            if (const uint32_t at = onlineString(1, kOnlineMessage[textLanguage()]))
            {
                SET_GPR_U32(ctx, 2, at);
                ctx->pc = GPR_U32(ctx, 31);
                return;
            }
        g_menuTextOriginal(rdram, ctx, runtime);
    }
    void hookModeSelect(PS2Runtime *runtime)
    {
        // only the European ELF's (and the Alpha 1.1X's, which runs it): an instruction of each, the A35's code is elsewhere
        const std::pair<uint32_t, uint32_t> sign[] = {{0x395CE0u, 0xAC800004u}, {0x395D04u, 0x2463C300u}, {0x395F30u, 0x24020005u},
                                                      {0x3D1870u, 0x2442A810u}, {0x3D18C0u, 0x2442A830u}, {0x1DD470u, 0x27BDFFD0u}};
        for (const auto &[address, word] : sign)
            if (read32(address) != word)
            {
                std::fprintf(stderr, "[online] not the European Mode Select (%x): no Naruto Online in the menu\n", address);
                return;
            }
        const std::pair<uint32_t, std::pair<PS2Runtime::RecompiledFunction *, PS2Runtime::RecompiledFunction>> hooks[] = {
            {kMsBuildList, {&g_msBuildOriginal, msBuildList}}, {kMsUnavailable, {&g_msUnavailableOriginal, msUnavailable}},
            {kModeText, {&g_modeTextOriginal, modeText}},      {kMenuText, {&g_menuTextOriginal, menuText}},
            {kPlaySound, {&g_playSoundOriginal, playSound}}};
        for (const auto &[address, hook] : hooks)
            if ((*hook.first = runtime->lookupFunction(address)) != nullptr)
                runtime->registerFunction(address, hook.second);
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
        if ((current & kPadDirections) != 0u)
            g_padDirTick[index] = g_tick;
        const bool tapped = (pressed & mask) != 0u;
        if (tapped)
        {
            g_padTapped[index] = 1u;
            g_padSpecialDown[index] = 1u;
            g_padSpecialTick[index] = g_tick;
        }
        else if (g_padSpecialTick[index] != g_tick)
        {
            g_padTapped[index] = 0u;
            g_padSpecialDown[index] = 0u;
        }
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
                // (the circle is also the attack: with a direction, or right after a directional macro like ↑↑○/↓↓○, stay attack)
                const bool recentDir = g_padDirTick[index] != 0u && (g_tick - g_padDirTick[index] <= 10u);
                const bool plain = mask != kPadCircle || ((read32(pad + kPadCurrent) & kPadDirections) == 0u && !recentDir);
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
        // (abort if hit/interrupted or if the first tap of chakra was refused)
        const uint32_t curAction = read16(f + kFighterAction);
        if ((q.step > 0 && curAction != 0u && curAction != 1u && curAction != 8u) ||
            (q.step == 2 && readf(f + kFighterChakra) >= q.chakra))
        {
            q.step = -1;
            return;
        }
        const int t = q.length - q.step; // (from the end: attack, attack, nothing)
        const uint32_t bits = q.length == 6 ? kJutsu[q.step] : (q.step < 2 || (t <= 5 && t >= 4)) ? kInChakra : (t <= 3 && t >= 2) ? kInAttack : 0u;
        word |= bits;
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
        // entries: a vsync, or "@N+vsyncs" counted from the start of fight N (as in HL_PADROUTE)
        struct At
        {
            uint64_t at;
            uint32_t fight;
        };
        static std::vector<At> s_at;
        static size_t s_next = 0;
        static bool s_read = false;
        if (!s_read)
        {
            s_read = true;
            if (const char *v = std::getenv("HL_CPU_HP"))
                for (const char *c = v; *c != 0;)
                {
                    At a{0u, 0u};
                    char *end = nullptr;
                    if (*c == '@')
                    {
                        a.fight = static_cast<uint32_t>(std::strtoul(c + 1, &end, 10));
                        if (*end == '+')
                            a.at = std::strtoull(end + 1, &end, 10);
                    }
                    else
                    {
                        a.at = std::strtoull(c, &end, 10);
                        if (end == c)
                            break;
                    }
                    s_at.push_back(a);
                    c = *end == ',' ? end + 1 : end;
                }
        }
        // (a step of a fight that is over already, the next one having begun, is dropped)
        while (s_next < s_at.size() && s_at[s_next].fight != 0u && fightStart(s_at[s_next].fight + 1u) != 0u)
            ++s_next;
        if (s_next >= s_at.size() || (read32(f + kFighterKind) & 0x1E0u) == 0u)
            return;
        uint64_t due = s_at[s_next].at;
        if (s_at[s_next].fight != 0u)
        {
            const uint64_t start = fightStart(s_at[s_next].fight);
            if (start == 0u)
                return;
            due += start;
        }
        if (g_tick < due)
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
        noteFightStart();
        // Track Player 1's active character ID (fighter+0x68, verified in sub_002146A0) for automatic portrait export & per-character mastery
        if (humanFighter(f) && (read16(f + kFighterKind) & 1u) == 0u)
        {
            const uint32_t cid = read32(f + 0x68u);
            if (cid > 0u && cid < 256u)
                hl_game::g_p1CharId.store(static_cast<int>(cid), std::memory_order_relaxed);
        }
        {
            // HL_SP_LOG: which fighter is the human one, its chakra and action, twice a second
            static const bool s_spLog = std::getenv("HL_SP_LOG") != nullptr;
            if (s_spLog && humanFighter(f) && g_tick % 25u < 2u && g_tick != g_fighterTick)
                std::fprintf(stderr, "[sp] v%llu human fighter 0x%x chakra %.1f action %u move 0x%x\n", static_cast<unsigned long long>(g_tick), f,
                             static_cast<double>(readf(f + kFighterChakra)), read16(f + kFighterAction), read32(f + kFighterMove));
        }
        g_fighterTick = g_tick;
        special(f);
        cpuHealthRoute(f);
        // HL_P1_HP=<vsync> (debug only, to reach a scene): at the first input of the human fighter on or after it, its health
        // becomes 0.12 (Naruto's low-health state, the Kyubi aura); the first two fighters seen, so the 2P too.
        static const uint64_t s_p1At = std::getenv("HL_P1_HP") ? std::strtoull(std::getenv("HL_P1_HP"), nullptr, 10) : 0u;
        static uint32_t s_p1Done[4] = {};
        if (s_p1At != 0u && g_tick >= s_p1At && s_p1Done[0] != f && s_p1Done[1] != f && s_p1Done[3] == 0u)
        {
            for (uint32_t &d : s_p1Done)
                if (d == 0u) { d = f; break; }
            const float hp = std::getenv("HL_P1_HP_VAL") ? static_cast<float>(std::atof(std::getenv("HL_P1_HP_VAL"))) : 0.12f;
            std::memcpy(g_rdram + f + 0x6Cu, &hp, 4);
            std::fprintf(stderr, "[route] v%llu fighter 0x%x (kind %x): health %.2f\n", static_cast<unsigned long long>(g_tick), f, read32(f + kFighterKind), hp);
        }
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
    // (kMoveOugi defined above at line 506)

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
#if defined(__ANDROID__)
            char model[PROP_VALUE_MAX] = {};
            __system_property_get("ro.product.model", model);
#else
            const char model[] = "pc"; // (recomp/pc)
#endif
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


    // HL_DMAH_LOG=1: the game's VIF1 packet chain (kick 0x107A20, DMAC handler 0x108D10 -> 0x108F00) and the byte it
    // waits on (gp-0x3F0) at each step, a few hundred lines. Neji's Ougi left the main loop spinning on it (2026-11-12).
    PS2Runtime::RecompiledFunction g_dmaKickOriginal = nullptr, g_dmaHandlerOriginal = nullptr, g_dmaPacketOriginal = nullptr;
    int g_dmahLines = 0;
    uint64_t g_dmahFrom = 0; // (HL_DMAH_LOG=<vsync>: from there)
    bool dmahLog() { return g_tick >= g_dmahFrom && g_dmahLines++ < 400; }
    uint32_t dmahFlag(R5900Context *ctx) { return g_rdram[(GPR_U32(ctx, 28) - 0x3F0u) & 0x1FFFFFFu]; }
    void dmaKick(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t obj = GPR_U32(ctx, 4);
        g_dmaKickOriginal(rdram, ctx, runtime);
        if (dmahLog())
            std::fprintf(stderr, "[dmah] v%llu kick obj %x list %x flag %u CHCR %08x MADR %08x TADR %08x\n", static_cast<unsigned long long>(g_tick), obj,
                         read32(obj + 0x1D0u), dmahFlag(ctx), runtime->memory().readIORegister(0x10009000u), runtime->memory().readIORegister(0x10009010u),
                         runtime->memory().readIORegister(0x10009030u));
    }
    void dmaHandler(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        auto &m = runtime->memory();
        const uint32_t chcr = m.readIORegister(0x10009000u), madr = m.readIORegister(0x10009010u), tadr = m.readIORegister(0x10009030u),
                       asr0 = m.readIORegister(0x10009040u), stat = m.readIORegister(0x1000E010u);
        g_dmaHandlerOriginal(rdram, ctx, runtime);
        if (dmahLog())
            std::fprintf(stderr, "[dmah] v%llu handler CHCR %08x MADR %08x TADR %08x ASR0 %08x STAT %08x -> v0 %d flag %u, now CHCR %08x\n",
                         static_cast<unsigned long long>(g_tick), chcr, madr, tadr, asr0, stat, static_cast<int>(GPR_U32(ctx, 2)), dmahFlag(ctx),
                         m.readIORegister(0x10009000u));
    }
    void dmaPacket(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t pkt = GPR_U32(ctx, 4), asp = GPR_U32(ctx, 6);
        const uint32_t w0 = read32(pkt), w1 = read32(pkt + 4u), w2 = read32(pkt + 8u), w3 = read32(pkt + 12u), cb = read32(pkt + 0x18u);
        g_dmaPacketOriginal(rdram, ctx, runtime);
        if (dmahLog())
            std::fprintf(stderr, "[dmah] v%llu packet %x tag %08x %08x %08x %08x cb %x asp %u -> v0 %d\n", static_cast<unsigned long long>(g_tick), pkt, w0, w1,
                         w2, w3, cb, asp, static_cast<int>(GPR_U32(ctx, 2)));
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
            if (std::getenv("HL_CAM_LOG") != nullptr && (g_camSetOriginal = runtime->lookupFunction(0x10EED0u)) != nullptr)
                runtime->registerFunction(0x10EED0u, cameraSetScale);
            if (const char *dmah = std::getenv("HL_DMAH_LOG"))
            {
                g_dmahFrom = std::strtoull(dmah, nullptr, 10);
                if ((g_dmaKickOriginal = runtime->lookupFunction(0x107A20u)) != nullptr)
                    runtime->registerFunction(0x107A20u, dmaKick);
                if ((g_dmaHandlerOriginal = runtime->lookupFunction(0x108D10u)) != nullptr)
                    runtime->registerFunction(0x108D10u, dmaHandler);
                if ((g_dmaPacketOriginal = runtime->lookupFunction(0x108F00u)) != nullptr)
                    runtime->registerFunction(0x108F00u, dmaPacket);
            }
            if ((g_mallocOriginal = runtime->lookupFunction(0x17A610u)) != nullptr)
                runtime->registerFunction(0x17A610u, gameMalloc);
            if ((g_callRpcOriginal = runtime->lookupFunction(0x1629E8u)) != nullptr)
                runtime->registerFunction(0x1629E8u, sifCallRpc);
            if ((g_bindRpcOriginal = runtime->lookupFunction(0x162808u)) != nullptr)
                runtime->registerFunction(0x162808u, sifBindRpc);
            g_inputOriginal = runtime->lookupFunction(kFighterInput);
            ::g_hlFightStart = &fightStart;
            if (g_inputOriginal != nullptr)
                runtime->registerFunction(kFighterInput, fighterInput);
            g_sequenceOriginal = runtime->lookupFunction(kSequenceStep);
            if (g_sequenceOriginal != nullptr)
                runtime->registerFunction(kSequenceStep, sequenceStep);
            hookModeSelect(runtime);
            hookOverlay();
            hookCounts();
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
        hl_game::g_vsync.store(static_cast<uint32_t>(tick), std::memory_order_relaxed);
        {
            static bool s_nativeWas = false;
            const bool on = hl_game::g_nativeOn != nullptr && hl_game::g_nativeOn();
            if (on && !s_nativeWas)
                g_area.fetch_add(1u, std::memory_order_relaxed);
            s_nativeWas = on;
        }
        logCounts(tick);
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
        {
            // HL_RAM_DUMP=V,V,... (debugging): the 32 MB of EE RAM at these vsyncs, into HL_RAM_DUMP_DIR (the app's files
            // folder by default)/ramdumps/ram_<V>.bin, for comparing the game's state between scenes (build/lab/ramflag.py)
            static std::vector<uint64_t> s_ramAt = [] {
                std::vector<uint64_t> at;
                if (const char *v = std::getenv("HL_RAM_DUMP"))
                    for (const char *p = v; *p;)
                    {
                        char *e;
                        at.push_back(std::strtoull(p, &e, 10));
                        p = *e ? e + 1 : e;
                    }
                return at;
            }();
            // HL_WATCH=ADDR,ADDR,... (hex, debugging): a line whenever one of these 32-bit words changes
            static std::vector<std::pair<uint32_t, uint32_t>> s_watch = [] {
                std::vector<std::pair<uint32_t, uint32_t>> w;
                if (const char *v = std::getenv("HL_WATCH"))
                    for (const char *p = v; *p;)
                    {
                        char *e;
                        w.emplace_back(static_cast<uint32_t>(std::strtoul(p, &e, 16)), 0xDEADBEEFu);
                        p = *e ? e + 1 : e;
                    }
                return w;
            }();
            // HL_POKE=ADDR=VALUE,... (hex, debugging): these 32-bit words written at every vsync
            static std::vector<std::pair<uint32_t, uint32_t>> s_poke = [] {
                std::vector<std::pair<uint32_t, uint32_t>> w;
                if (const char *v = std::getenv("HL_POKE"))
                    for (const char *p = v; *p;)
                    {
                        char *e;
                        const uint32_t a = static_cast<uint32_t>(std::strtoul(p, &e, 16));
                        const uint32_t x = *e == '=' ? static_cast<uint32_t>(std::strtoul(e + 1, &e, 16)) : 0u;
                        w.emplace_back(a & 0x1FFFFFCu, x);
                        p = *e ? e + 1 : e;
                    }
                return w;
            }();
            for (const auto &[addr, value] : s_poke)
                std::memcpy(g_rdram + addr, &value, 4);
            for (auto &[addr, last] : s_watch)
                if (const uint32_t now = read32(addr); now != last)
                {
                    std::fprintf(stderr, "[watch] v%llu %07x %08x -> %08x (overlay %d)\n", static_cast<unsigned long long>(tick), addr, last, now, hl_game::overlay());
                    last = now;
                }
            for (uint64_t &at : s_ramAt)
                if (at != 0u && tick >= at)
                {
                    const char *dir = std::getenv("HL_RAM_DUMP_DIR");
                    const std::string base = std::string(dir ? dir : "/sdcard/Android/data/com.dfdx047.hiddenleaf/files") + "/ramdumps";
                    ::mkdir(base.c_str(), 0775);
                    const std::string name = base + "/ram_" + std::to_string(tick) + ".bin";
                    if (FILE *f = std::fopen(name.c_str(), "wb"))
                    {
                        std::fwrite(g_rdram, 1, 32u << 20, f);
                        std::fclose(f);
                    }
                    std::fprintf(stderr, "[ram] v%llu %s\n", static_cast<unsigned long long>(tick), name.c_str());
                    at = 0u;
                }
        }
        // (for the native renderer's fight held without its detector: hl_native.cpp, s_held)
        if (hl_game::g_wideCameraOut != nullptr)
            hl_game::g_wideCameraOut(wideCamera());
        if (hl_game::g_liveFight != nullptr)
            hl_game::g_liveFight(hl_game::overlay() == 1 && g_fighterTick != 0u && tick - g_fighterTick <= 30u && !ougiOnScreen()); // (30: in a live fight the age reaches 11 to 21 now and then)
        static const bool s_spLog = std::getenv("HL_SP_LOG") != nullptr;
        if (s_spLog && tick % 50u == 0u && hl_game::overlay() == 1)
            std::fprintf(stderr, "[sp] v%llu ages: fighter %lld ougi %lld, on screen %d, scene %d\n", static_cast<unsigned long long>(tick),
                         g_fighterTick ? static_cast<long long>(tick - g_fighterTick) : -1ll, g_ougiTick ? static_cast<long long>(tick - g_ougiTick) : -1ll,
                         ougiOnScreen() ? 1 : 0, hl_game::scene());
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

    // (chained: the PC's bench, recomp/runtime/hl_bench.cpp, keeps its own on the same hook, and the order of the two
    // static constructors is not fixed)
    void (*g_prevVsync)(PS2Runtime *, uint64_t) = nullptr;
    void onVsyncChained(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_prevVsync)
            g_prevVsync(runtime, tick);
        onVsync(runtime, tick);
    }

    struct Install
    {
        Install()
        {
            g_prevVsync = g_hlVsyncHook;
            g_hlVsyncHook = &onVsyncChained;
        }
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
// A fight is also what the game itself says: the fighters reading their input, or an Ougi's sequence or prompt reading the
// pad, in the last 3 s (150 vsyncs). That holds the fight's controls while the renderer has let go: an Ougi drawn by the GS
// went on longer than 8 s and its buttons became the menu's in the middle of the sequence (docs/registro.md, 2026-11-05).
// With that, a fight's controls go 2 s after the last of these (the character select after a fight kept them 10 s); the
// campaign keeps 8 s for the loading of an area.
int hl_game::scene()
{
    if (g_rdram == nullptr)
        return 0;
    static int s_scene = 1;
    static uint64_t s_lastOn = 0;
    const uint64_t now = SDL_GetTicks();
    const int mode = overlay();
    const uint64_t tick = g_tick;
    const bool fighting = mode == 1 && ((g_fighterTick != 0u && tick - g_fighterTick < 150u) || (g_ougiTick != 0u && tick - g_ougiTick < 150u) || ougiOnScreen());
    if (fighting)
    {
        s_scene = 2;
        s_lastOn = now;
    }
    else if (g_nativeOn != nullptr && g_nativeOn() && (mode == 1 || mode == 2))
    {
        s_scene = mode == 1 ? 2 : 3;
        s_lastOn = now;
    }
    else if (s_scene != 1 && (now - s_lastOn > (s_scene == 2 ? 2000u : 8000u) || (s_scene == 2 ? mode != 1 : mode != 2)))
        s_scene = 1;
    // In the campaign, while the player cannot move (a conversation, a tutorial's box, a cut scene) the touch controls are the
    // menu's: the fight's buttons leave the text alone and a tap is ✕, which moves the conversation on. From the first
    // conversation: the hook is only in when this library's ADV (the one the overlay resolver found in memory) has that
    // function at 0x723970 (the base's, the Alpha 1.1X's, the A35's); a library without it keeps the campaign's controls.
    // HL_TALK_OFF=1 keeps them too. A conversation's box seen in the last 15 vsyncs (0x7F3F60) is one at once, in any area;
    // without the box, 0x723970 not running counts only in an area where it has run (it never runs in some rooms, where the
    // player would be left without the stick).
    static const bool s_talkOff = std::getenv("HL_TALK_OFF") != nullptr;
    if (s_scene == 3 && mode == 2 && !s_talkOff)
    {
        if (g_advTalkOriginal != nullptr && g_advTalkTick != 0u && tick - g_advTalkTick <= 15u)
            return 4; // (the launcher takes it as the menu's controls, at once: no wait for a loading screen's flicker)
        if (g_advFreeOriginal != nullptr && tick - g_advFreeTick > 15u &&
            g_advFreeArea.load(std::memory_order_relaxed) == g_area.load(std::memory_order_relaxed))
            return 4;
    }
    return s_scene;
}

int hl_game::menuTap(float nx, float ny)
{
    if (overlay() != 0 || g_modeSelectObj.load(std::memory_order_relaxed) == 0u)
        return 0;
    const float xScale = g_cameraXScale.load(std::memory_order_relaxed);
    const float x43 = xScale > 0.01f && xScale < 0.999f ? 0.5f + (nx - 0.5f) / xScale : nx;
    if (x43 >= 0.08f && x43 <= 0.58f)
    {
        if (ny >= 0.16f && ny < 0.37f) return 1; // UP (top board)
        if (ny >= 0.37f && ny <= 0.64f) return 2; // CROSS (center board)
        if (ny > 0.64f && ny <= 0.86f) return 3; // DOWN (bottom board)
    }
    return 0;
}
