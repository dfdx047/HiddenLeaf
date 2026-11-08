// Comparison bench against PCSX2 (PC only; docs/plano.md "Bancada contra o PCSX2"). The same records as the PCSX2 bench
// hook (~/build/pcsx2-bench/src/pcsx2/HLBench.cpp, kept out of this repository), read by tools/hlbench.py.
//
//   HLB_OUT=<prefix>   <prefix>.bin (records) and <prefix>.txt (events); nothing is hooked without it
//   HLB_RNG=1          at the start of each fight the Mersenne Twister's index (gp-0x7FA8 = 0x60FF48) becomes 0x271, so its
//                      next number reseeds it with the game's default seed: the same state in both
//   HLB_SHOT=N         a frame dump (the runtime's hl_shot trigger) every N fight frames
//   HLB_FRAMES=N       records per fight at most
//   HLB_QUIT=<vsync>   exit at this vsync
//   HLB_RAM=1          the EE RAM at each fight's first call (before HLB_RNG), in <prefix>_ramNN.bin
//   HLB_RAMAT=1,30,50  the EE RAM at these frames of the first fighter, in <prefix>_ramNN_fFFFFF.bin
//   HLB_MARK=1         "[hlb] v.. fight .. frame .." on stderr at each frame of the first fighter (to date other logs)
//   HLB_CDBUSY=N       experiment: sceCdSync(1) answers busy for N vsyncs after each sceCdRead, once a fight began
//   HLB_P1CPU=0x23     the first fighter's kind (low byte of fighter+0x60) becomes this at its first frame of each fight,
//                      as the second (CPU) fighter has it: CPU against CPU
// The buttons come from the runtime's HL_PADROUTE (the same syntax as HLB_ROUTE on the PCSX2 side).
//
// A record is written at the entry of 0x21E660, the per-frame input of each fighter (a0 = the fighter): the fighter's
// 0xC00 bytes, the first camera's 0x280 (*(0x617ACC)), the twister's index and a hash of its 624 64-bit words (0x627D40).

#if !defined(__ANDROID__)

#include "ps2_runtime.h"
#include "ps2_runtime_macros.h"

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

namespace
{
    constexpr uint32_t kFighterInput = 0x21E660u;
    constexpr uint32_t kFighterBytes = 0xC00u;
    constexpr uint32_t kCameraBytes = 0x280u;
    constexpr uint32_t kCameraList = 0x617ACCu;
    constexpr uint32_t kMtIndex = 0x60FF48u;
    constexpr uint32_t kMtState = 0x627D40u;
    constexpr uint32_t kMtBytes = 624u * 8u;
    constexpr uint32_t kImpure = 0x412DD8u; // newlib _impure_ptr; rand()'s 64-bit state at +0xA8
    constexpr uint32_t kOverlayBase = 0x6C6D00u;

    FILE *s_bin = nullptr;
    FILE *s_txt = nullptr;
    std::string s_prefix;
    uint8_t *s_ram = nullptr;
    uint32_t s_vsync = 0, s_lastHook = 0, s_fight = 0, s_call = 0, s_frame = 0, s_first = 0;
    uint32_t s_shot = 0, s_maxCalls = 0, s_quit = 0, s_lastOverlay = 0xFFFFFFFFu;
    bool s_rng = false;
    uint32_t s_p1cpu = 0;
    bool s_ramDump = false;
    bool s_mark = false;
    std::vector<uint32_t> s_ramAt;
    PS2Runtime::RecompiledFunction s_inputOriginal = nullptr;
    // HLB_CDBUSY=N (an experiment): sceCdSync(non-blocking) says "busy" for N vsyncs after each sceCdRead, as a disc that
    // takes time to seek and read would
    constexpr uint32_t kCdRead = 0x174308u, kCdSync = 0x1738D0u;
    uint32_t s_cdBusy = 0, s_cdReadVsync = 0;
    PS2Runtime::RecompiledFunction s_cdReadOriginal = nullptr, s_cdSyncOriginal = nullptr;
    void (*s_prevVsync)(PS2Runtime *, uint64_t) = nullptr;

    inline uint32_t rd32(uint32_t a)
    {
        uint32_t v;
        std::memcpy(&v, s_ram + (a & 0x1FFFFFFu), 4);
        return v;
    }
    inline void wr32(uint32_t a, uint32_t v) { std::memcpy(s_ram + (a & 0x1FFFFFFu), &v, 4); }

    uint32_t fnv(const uint8_t *p, uint32_t n)
    {
        uint32_t h = 2166136261u;
        for (uint32_t i = 0; i < n; ++i)
            h = (h ^ p[i]) * 16777619u;
        return h;
    }

    uint32_t rngHash()
    {
        uint32_t h = fnv(s_ram + kMtState, kMtBytes);
        const uint32_t r = rd32(kImpure);
        if (r >= 0x100000u && r < 0x2000000u)
            h = h * 31u + fnv(s_ram + r + 0xA8u, 8);
        return h;
    }

    void fighterInput(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t f = GPR_U32(ctx, 4);
        const uint32_t ra = GPR_U32(ctx, 31);
        if (s_lastHook == 0 || s_vsync - s_lastHook > 100u)
        {
            ++s_fight;
            s_call = 0;
            s_frame = 0;
            s_first = f;
            std::fprintf(s_txt, "v%u fight %u start: fighter %08x mti %u mt %08x\n", s_vsync, s_fight, f, rd32(kMtIndex), rngHash());
            if (s_ramDump)
            {
                char name[32];
                std::snprintf(name, sizeof(name), "_ram%02u.bin", s_fight);
                if (FILE *r = std::fopen((s_prefix + name).c_str(), "wb"))
                    std::fwrite(s_ram, 1, 0x2000000u, r), std::fclose(r);
            }
            if (s_rng)
            {
                wr32(kMtIndex, 0x271u);
                const uint32_t r = rd32(kImpure);
                if (r >= 0x100000u && r < 0x2000000u)
                    wr32(r + 0xA8u, 1u), wr32(r + 0xACu, 0u);
                std::fprintf(s_txt, "v%u fight %u rng: mti 0x271, rand state 1\n", s_vsync, s_fight);
            }
        }
        s_lastHook = s_vsync;
        if (f == s_first)
            ++s_frame;
        if (s_mark && f == s_first)
            std::fprintf(stderr, "[hlb] v%u fight %u frame %u\n", s_vsync, s_fight, s_frame);
        if (f == s_first && std::find(s_ramAt.begin(), s_ramAt.end(), s_frame) != s_ramAt.end())
        {
            char name[48];
            std::snprintf(name, sizeof(name), "_ram%02u_f%05u.bin", s_fight, s_frame);
            if (FILE *r = std::fopen((s_prefix + name).c_str(), "wb"))
                std::fwrite(s_ram, 1, 0x2000000u, r), std::fclose(r);
        }
        if (s_p1cpu != 0 && s_frame == 1 && f == s_first)
        {
            const uint32_t kind = rd32(f + 0x60);
            wr32(f + 0x60, (kind & ~0xFFu) | s_p1cpu);
            std::fprintf(s_txt, "v%u fighter %08x kind %08x -> %08x\n", s_vsync, f, kind, rd32(f + 0x60));
        }
        ++s_call;
        if (s_maxCalls == 0 || s_call <= s_maxCalls)
        {
            const uint32_t cam = rd32(kCameraList);
            const uint32_t rec[9] = {0x63655246u, s_vsync, s_fight, s_call, f, ra, rd32(kMtIndex), rngHash(), cam};
            std::fwrite(rec, 4, 9, s_bin);
            std::fwrite(s_ram + (f & 0x1FFFFFFu), 1, kFighterBytes, s_bin);
            static uint8_t zero[kCameraBytes];
            std::fwrite(cam >= 0x100000u && cam < 0x2000000u ? s_ram + cam : zero, 1, kCameraBytes, s_bin);
            if (s_shot != 0 && f == s_first && (s_frame % s_shot) == 1u)
            {
                if (FILE *t = std::fopen("hl_shot", "w"))
                    std::fclose(t);
                std::fprintf(s_txt, "v%u shot fight %u frame %u\n", s_vsync, s_fight, s_frame);
            }
        }
        s_inputOriginal(rdram, ctx, runtime);
    }

    void cdRead(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        s_cdReadVsync = s_vsync;
        std::fprintf(s_txt, "v%u cdread lbn %x sectors %x\n", s_vsync, GPR_U32(ctx, 4), GPR_U32(ctx, 5));
        s_cdReadOriginal(rdram, ctx, runtime);
    }

    void cdSync(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t mode = GPR_U32(ctx, 4);
        if (mode != 0u && s_fight != 0u && s_vsync - s_cdReadVsync < s_cdBusy)
        {
            SET_GPR_S32(ctx, 2, 1);
            ctx->pc = GPR_U32(ctx, 31);
            return;
        }
        if (mode == 0u && s_fight != 0u && s_vsync - s_cdReadVsync < s_cdBusy)
            std::fprintf(s_txt, "v%u cdsync blocking while busy\n", s_vsync);
        s_cdSyncOriginal(rdram, ctx, runtime);
    }

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (s_prevVsync)
            s_prevVsync(runtime, tick);
        s_vsync = static_cast<uint32_t>(tick);
        if (s_ram == nullptr)
        {
            s_ram = runtime->memory().getRDRAM();
            s_inputOriginal = runtime->lookupFunction(kFighterInput);
            if (s_inputOriginal != nullptr)
                runtime->registerFunction(kFighterInput, fighterInput);
            if (s_cdBusy != 0u)
            {
                s_cdReadOriginal = runtime->lookupFunction(kCdRead);
                s_cdSyncOriginal = runtime->lookupFunction(kCdSync);
                if (s_cdReadOriginal != nullptr && s_cdSyncOriginal != nullptr)
                {
                    runtime->registerFunction(kCdRead, cdRead);
                    runtime->registerFunction(kCdSync, cdSync);
                }
                std::fprintf(s_txt, "v%u cd busy %u vsyncs: %d\n", s_vsync, s_cdBusy, s_cdSyncOriginal != nullptr);
            }
            std::fprintf(s_txt, "v%u hooked 0x%x: %d\n", s_vsync, kFighterInput, s_inputOriginal != nullptr);
        }
        const uint32_t ovl = std::memcmp(s_ram + kOverlayBase, "MWo3", 4) == 0 ? rd32(kOverlayBase + 4) : 0u;
        if (ovl != s_lastOverlay)
        {
            std::fprintf(s_txt, "v%u overlay %u\n", s_vsync, ovl);
            s_lastOverlay = ovl;
        }
        if (s_quit != 0 && s_vsync >= s_quit)
        {
            std::fprintf(s_txt, "v%u quit\n", s_vsync);
            std::fclose(s_bin);
            std::fclose(s_txt);
            std::_Exit(0);
        }
    }

    struct Install
    {
        Install()
        {
            const char *out = std::getenv("HLB_OUT");
            if (out == nullptr)
                return;
            s_prefix = out;
            s_bin = std::fopen((s_prefix + ".bin").c_str(), "wb");
            s_txt = std::fopen((s_prefix + ".txt").c_str(), "w");
            if (s_bin == nullptr || s_txt == nullptr)
                return;
            setvbuf(s_txt, nullptr, _IOLBF, 0);
            const uint32_t header[4] = {0x31424C48u, 1u, kFighterBytes, kCameraBytes};
            std::fwrite(header, 4, 4, s_bin);
            s_rng = std::getenv("HLB_RNG") != nullptr;
            s_ramDump = std::getenv("HLB_RAM") != nullptr;
            s_mark = std::getenv("HLB_MARK") != nullptr;
            s_cdBusy = std::getenv("HLB_CDBUSY") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_CDBUSY"))) : 0u;
            if (const char *v = std::getenv("HLB_RAMAT"))
                for (const char *p = v; *p;)
                {
                    char *e;
                    s_ramAt.push_back(static_cast<uint32_t>(std::strtoul(p, &e, 0)));
                    p = *e ? e + 1 : e;
                }
            s_p1cpu = std::getenv("HLB_P1CPU") ? static_cast<uint32_t>(std::strtoul(std::getenv("HLB_P1CPU"), nullptr, 0)) & 0xFFu : 0u;
            s_shot = std::getenv("HLB_SHOT") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_SHOT"))) : 0u;
            s_maxCalls = std::getenv("HLB_FRAMES") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_FRAMES"))) : 0u;
            s_quit = std::getenv("HLB_QUIT") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_QUIT"))) : 0u;
            std::fprintf(s_txt, "recomp hlbench: rng %d shot %u\n", s_rng, s_shot);
            s_prevVsync = g_hlVsyncHook;
            g_hlVsyncHook = onVsync;
        }
    } s_install;
} // namespace

#endif
