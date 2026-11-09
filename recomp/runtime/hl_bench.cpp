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
//   HLB_RNGLOG=F,T     every call of the Mersenne Twister (0x180BA0) and rand (0x17C558) in frames F..T: "rng ..." lines
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

#include <csignal>
#include <mutex>
#include <fstream>
extern std::mutex g_hlGifCapMutex;
extern std::FILE *g_hlGifCap;
extern std::FILE *g_hlVuCap;

void hlProfDump(); // recomp/runtime/hl_prof.cpp

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
    // The game's pool at newlib's address, as hl_game.cpp does on Android: at boot (0x118D34, then 0x118D50) the game asks
    // malloc for the RAM from its ELF's end ($s7) to its stack and keeps its own heap there; newlib gives end + 0x10, the
    // runtime's heap something else (here 0x80 off the PCSX2 side). Asked for 0x100 more, the pool starts at end + 0x10
    // when the runtime's block begins at most 0x100 under it ("pool" line in the text).
    PS2Runtime::RecompiledFunction s_mallocOriginal = nullptr;
    void gameMalloc(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t ra = GPR_U32(ctx, 31);
        if (ra != 0x118D3Cu && ra != 0x118D58u)
        {
            s_mallocOriginal(rdram, ctx, runtime);
            return;
        }
        const uint32_t size = GPR_U32(ctx, 4), want = GPR_U32(ctx, 23) + 0x10u;
        SET_GPR_U32(ctx, 4, size + 0x100u);
        s_mallocOriginal(rdram, ctx, runtime);
        const uint32_t got = GPR_U32(ctx, 2);
        if (got != 0u && want >= got && want - got <= 0x100u)
            SET_GPR_U32(ctx, 2, want);
        if (got != 0u)
            std::fprintf(stderr, "[hlb] pool 0x%x bytes: runtime 0x%x, given 0x%x (newlib end+0x10 = 0x%x)\n", size, got, GPR_U32(ctx, 2), want);
    }
    // HLB_CDBUSY=N (an experiment): sceCdSync(non-blocking) says "busy" for N vsyncs after each sceCdRead, as a disc that
    // takes time to seek and read would
    constexpr uint32_t kCdRead = 0x174308u, kCdSync = 0x1738D0u;
    uint32_t s_cdBusy = 0, s_cdReadVsync = 0;
    bool s_cdLog = false; // HLB_CDLOG=1: "v.. cdread lbn .. sectors .." at each EE sceCdRead, as the PCSX2 side's HLB_CDLOG
    // HLB_FROUTE="[fight.]frame:WORDS,...": buttons by the first fighter's frame (fight 1 by default), as the PCSX2 side's
    struct FStep
    {
        uint32_t fight, frame;
        std::string word;
    };
    std::vector<FStep> s_froute;
    // HLB_OROUTE="ovl.off:WORDS,...": buttons off vsyncs after overlay ovl first appears; HLB_OSHOT=ovl,step,count: a shot
    // (hl_shot) every step vsyncs from it, "v.. oshot N" (the PCSX2 side's)
    std::vector<FStep> s_oroute;
    uint32_t s_oshotOvl = 0, s_oshotStep = 0, s_oshotCount = 0, s_ovlSeen[64] = {};

    // HLB_RNGLOG=F,T: every call of the Mersenne Twister and of rand in frames F..T
    constexpr uint32_t kMtNext = 0x180BA0u, kRand = 0x17C558u;
    // and the game's helpers on top of it: a float in [0, f12) (0x1810D0), an int in [0, a0] (0x181020), a word (0x180FF0)
    constexpr uint32_t kRandHelpers[3] = {0x1810D0u, 0x181020u, 0x180FF0u};
    PS2Runtime::RecompiledFunction s_helperOriginal[3] = {};
    // HLB_SFLOG=1 with HLB_RNGLOG: the soft-float double routines called from the effect loop (0x34BC9C..0x34BCE4), with
    // their 64-bit arguments and result, to check them against exact IEEE arithmetic
    constexpr uint32_t kSoftFloat[5] = {0x172108u /* fptodp */, 0x16F4B0u /* fabs */, 0x172598u /* dpsub */,
                                         0x172600u /* dpmul */, 0x172BF0u /* dptoli */};
    PS2Runtime::RecompiledFunction s_sfOriginal[5] = {};
    uint32_t s_rngFrom = 0, s_rngTo = 0;
    PS2Runtime::RecompiledFunction s_mtOriginal = nullptr, s_randOriginal = nullptr;
    PS2Runtime::RecompiledFunction s_cdReadOriginal = nullptr, s_cdSyncOriginal = nullptr;
    void (*s_prevVsync)(PS2Runtime *, uint64_t) = nullptr;
    // HLB_GIFCAP=F[,N]: the GIF packets of N vsyncs (6) from frame F of the first fighter of fight 1, in <prefix>_gif.cap
    // (the HL_GSCAP format, a 0xFF record at each vsync: tools/gscap.py); the writer is in the runtime's ps2_memory.cpp
    uint32_t s_gifCapAt = 0, s_gifCapN = 6, s_gifCapLeft = 0;
    uint32_t s_vuCapAt = 0, s_vuCapN = 0, s_vuCapLeft = 0; // HLB_VU1CAP=F,N: each VU1 start in N vsyncs from fight 1's frame F
    // HLB_GIFCAPV=V,N: the same from vsync V (for the loading before a fight)
    uint32_t s_gifCapV = 0;
    // HLB_RAMV=V,V,...: the runtime's hl_ram dump (EE RAM, GS VRAM, VU1 data in hl_frames/) at these vsyncs
    std::vector<uint32_t> s_ramV;
    // HLB_SHOTV=V,V,...: the runtime's hl_shot at these vsyncs ("v.. shotv" in the txt; the frame lands in hl_frames/)
    std::vector<uint32_t> s_shotV;
    // HLB_TRAPV=V: raise SIGTRAP at vsync V (to stop under gdb at a known point)
    uint32_t s_trapV = 0;

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
        {
            ++s_frame;
            for (const FStep &st : s_froute)
                if (st.fight == s_fight && st.frame == s_frame)
                {   // the runtime's hl_pad file (patch 0026): each button held 6 vsyncs and let go 6, from the next vsync
                    std::ofstream("hl_pad", std::ios::app) << st.word << ' ';
                    std::fprintf(s_txt, "v%u fight %u frame %u froute %s\n", s_vsync, s_fight, s_frame, st.word.c_str());
                }
        }
        if (s_mark && f == s_first)
            std::fprintf(stderr, "[hlb] v%u fight %u frame %u\n", s_vsync, s_fight, s_frame);
        if (f == s_first && std::find(s_ramAt.begin(), s_ramAt.end(), s_frame) != s_ramAt.end())
        {
            char name[48];
            std::snprintf(name, sizeof(name), "_ram%02u_f%05u.bin", s_fight, s_frame);
            if (FILE *r = std::fopen((s_prefix + name).c_str(), "wb"))
                std::fwrite(s_ram, 1, 0x2000000u, r), std::fclose(r);
            if (FILE *t = std::fopen("hl_ram", "w")) // and the runtime's dump of the EE RAM and the GS VRAM, next vsync
                std::fclose(t);
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
            if (s_vuCapAt != 0 && s_fight == 1 && f == s_first && s_frame == s_vuCapAt && s_vuCapLeft == 0)
            {
                std::lock_guard<std::mutex> lock(g_hlGifCapMutex);
                g_hlVuCap = std::fopen((s_prefix + "_vu1.cap").c_str(), "wb");
                s_vuCapLeft = s_vuCapN;
                s_vuCapAt = 0;
                std::fprintf(s_txt, "v%u vu1cap fight %u frame %u\n", s_vsync, s_fight, s_frame);
            }
            if (s_gifCapAt != 0 && s_fight == 1 && f == s_first && s_frame == s_gifCapAt && s_gifCapLeft == 0)
            {
                std::lock_guard<std::mutex> lock(g_hlGifCapMutex);
                g_hlGifCap = std::fopen((s_prefix + "_gif.cap").c_str(), "wb");
                s_gifCapLeft = s_gifCapN;
                std::fprintf(s_txt, "v%u gifcap fight %u frame %u\n", s_vsync, s_fight, s_frame);
            }
            if (s_shot != 0 && f == s_first && (s_frame % s_shot) == 1u)
            {
                if (FILE *t = std::fopen("hl_shot", "w"))
                    std::fclose(t);
                std::fprintf(s_txt, "v%u shot fight %u frame %u\n", s_vsync, s_fight, s_frame);
            }
        }
        s_inputOriginal(rdram, ctx, runtime);
    }

    // HLB_FNLOG=addr,addr,... (up to 8): "fn v.. addr ra .. a0 .. a1 .." at each entry; the PCSX2 side has the same
    uint32_t s_fnAddr[8] = {}, s_fnCount = 0;
    PS2Runtime::RecompiledFunction s_fnOriginal[8] = {};
    template <int I>
    void fnLog(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        std::fprintf(s_txt, "fn v%u %x ra %x a0 %x a1 %x a2 %x\n", s_vsync, s_fnAddr[I], GPR_U32(ctx, 31), GPR_U32(ctx, 4),
                     GPR_U32(ctx, 5), GPR_U32(ctx, 6));
        s_fnOriginal[I](rdram, ctx, runtime);
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

    void rngNote(R5900Context *ctx, const char *what)
    {
        if (s_fight != 0 && s_frame >= s_rngFrom && s_frame <= s_rngTo)
            std::fprintf(s_txt, "rng v%u frame %u %s ra %x sp %x mti %u\n", s_vsync, s_frame, what, GPR_U32(ctx, 31), GPR_U32(ctx, 29), rd32(kMtIndex));
    }

    void mtNext(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        rngNote(ctx, "mt");
        s_mtOriginal(rdram, ctx, runtime);
    }

    void randNext(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        rngNote(ctx, "rand");
        s_randOriginal(rdram, ctx, runtime);
    }

    template <int I>
    void randHelper(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        if (s_fight != 0 && s_frame >= s_rngFrom && s_frame <= s_rngTo)
        {
            uint32_t f12;
            std::memcpy(&f12, &ctx->f[12], 4);
            // the effect loop at 0x34BB20 compares *(s2 + s1*16 + 0x48) + 0x1E4 with the number drawn
            const uint32_t ra = GPR_U32(ctx, 31);
            uint32_t obj = 0, cnt = 0;
            if (ra == 0x34BC98u || ra == 0x34BE48u)
            {
                obj = rd32(GPR_U32(ctx, 18) + GPR_U32(ctx, 17) * 16u + 0x48u);
                cnt = (obj >= 0x100000u && obj < 0x2000000u) ? rd32(obj + 0x1E4u) : 0xFFFFFFFFu;
            }
            std::fprintf(s_txt, "rng v%u frame %u fn %x ra %x a0 %x f12 %08x mti %u obj %x cnt %x\n", s_vsync, s_frame, kRandHelpers[I],
                         ra, GPR_U32(ctx, 4), f12, rd32(kMtIndex), obj, cnt);
        }
        s_helperOriginal[I](rdram, ctx, runtime);
    }

    template <int I>
    void softFloat(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t ra = GPR_U32(ctx, 31);
        const bool note = s_fight != 0 && s_frame >= s_rngFrom && s_frame <= s_rngTo && ra >= 0x34BC9Cu && ra <= 0x34BCE4u;
        const uint64_t a0 = GPR_U64(ctx, 4), a1 = GPR_U64(ctx, 5);
        uint32_t f12;
        std::memcpy(&f12, &ctx->f[12], 4);
        s_sfOriginal[I](rdram, ctx, runtime);
        if (note)
            std::fprintf(s_txt, "sf v%u frame %u fn %x ra %x a0 %016llx a1 %016llx f12 %08x -> %016llx\n", s_vsync, s_frame,
                         kSoftFloat[I], ra, static_cast<unsigned long long>(a0), static_cast<unsigned long long>(a1), f12,
                         static_cast<unsigned long long>(GPR_U64(ctx, 2)));
    }

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (s_prevVsync)
            s_prevVsync(runtime, tick);
        s_vsync = static_cast<uint32_t>(tick);
        if (s_trapV != 0 && s_vsync == s_trapV)
            std::raise(SIGTRAP);
        if (std::find(s_ramV.begin(), s_ramV.end(), s_vsync) != s_ramV.end())
            if (FILE *t = std::fopen("hl_ram", "w"))
                std::fclose(t);
        if (std::find(s_shotV.begin(), s_shotV.end(), s_vsync) != s_shotV.end())
        {
            if (FILE *t = std::fopen("hl_shot", "w"))
                std::fclose(t);
            std::fprintf(s_txt, "v%u shotv\n", s_vsync);
        }
        if (s_gifCapV != 0 && s_vsync == s_gifCapV && s_gifCapLeft == 0)
        {
            std::lock_guard<std::mutex> lock(g_hlGifCapMutex);
            g_hlGifCap = std::fopen((s_prefix + "_gif.cap").c_str(), "wb");
            s_gifCapLeft = s_gifCapN;
            std::fprintf(s_txt, "v%u gifcap\n", s_vsync);
        }
        if (s_vuCapLeft != 0)
        {
            std::lock_guard<std::mutex> lock(g_hlGifCapMutex);
            const uint32_t h[2] = {0xFFu, s_vsync};
            if (g_hlVuCap != nullptr)
                std::fwrite(h, 4, 2, g_hlVuCap);
            if (--s_vuCapLeft == 0 && g_hlVuCap != nullptr)
            {
                std::fclose(g_hlVuCap);
                g_hlVuCap = nullptr;
            }
        }
        if (s_gifCapLeft != 0)
        {
            std::lock_guard<std::mutex> lock(g_hlGifCapMutex);
            const uint32_t h[2] = {0xFFu, s_vsync};
            if (g_hlGifCap != nullptr)
                std::fwrite(h, 4, 2, g_hlGifCap);
            if (--s_gifCapLeft == 0 && g_hlGifCap != nullptr)
            {
                std::fclose(g_hlGifCap);
                g_hlGifCap = nullptr;
            }
        }
        if (s_ram == nullptr)
        {
            s_ram = runtime->memory().getRDRAM();
            s_inputOriginal = runtime->lookupFunction(kFighterInput);
            if (s_inputOriginal != nullptr)
                runtime->registerFunction(kFighterInput, fighterInput);
            if (std::getenv("HLB_NOPOOL") == nullptr && (s_mallocOriginal = runtime->lookupFunction(0x17A610u)) != nullptr)
                runtime->registerFunction(0x17A610u, gameMalloc);
            if (s_rngTo != 0u)
            {
                s_mtOriginal = runtime->lookupFunction(kMtNext);
                s_randOriginal = runtime->lookupFunction(kRand);
                if (s_mtOriginal != nullptr && s_randOriginal != nullptr)
                {
                    runtime->registerFunction(kMtNext, mtNext);
                    runtime->registerFunction(kRand, randNext);
                }
                if (std::getenv("HLB_SFLOG") != nullptr)
                {
                    const PS2Runtime::RecompiledFunction sf[5] = {softFloat<0>, softFloat<1>, softFloat<2>, softFloat<3>, softFloat<4>};
                    for (int i = 0; i < 5; ++i)
                        if ((s_sfOriginal[i] = runtime->lookupFunction(kSoftFloat[i])) != nullptr)
                            runtime->registerFunction(kSoftFloat[i], sf[i]);
                }
                const PS2Runtime::RecompiledFunction hooks[3] = {randHelper<0>, randHelper<1>, randHelper<2>};
                for (int i = 0; i < 3; ++i)
                    if ((s_helperOriginal[i] = runtime->lookupFunction(kRandHelpers[i])) != nullptr)
                        runtime->registerFunction(kRandHelpers[i], hooks[i]);
                std::fprintf(s_txt, "v%u rng log frames %u..%u: %d\n", s_vsync, s_rngFrom, s_rngTo, s_mtOriginal != nullptr);
            }
            {
                const PS2Runtime::RecompiledFunction fh[8] = {fnLog<0>, fnLog<1>, fnLog<2>, fnLog<3>,
                                                              fnLog<4>, fnLog<5>, fnLog<6>, fnLog<7>};
                for (uint32_t i = 0; i < s_fnCount; ++i)
                    if ((s_fnOriginal[i] = runtime->lookupFunction(s_fnAddr[i])) != nullptr)
                        runtime->registerFunction(s_fnAddr[i], fh[i]);
            }
            if (s_cdLog && s_cdBusy == 0u)
            {
                if ((s_cdReadOriginal = runtime->lookupFunction(kCdRead)) != nullptr)
                    runtime->registerFunction(kCdRead, cdRead);
            }
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
            if (ovl < 64 && s_ovlSeen[ovl] == 0)
                s_ovlSeen[ovl] = s_vsync;
        }
        for (const FStep &st : s_oroute)
            if (st.fight < 64 && s_ovlSeen[st.fight] != 0 && s_vsync - s_ovlSeen[st.fight] == st.frame)
            {
                std::ofstream("hl_pad", std::ios::app) << st.word << ' ';
                std::fprintf(s_txt, "v%u overlay %u +%u oroute %s\n", s_vsync, st.fight, st.frame, st.word.c_str());
            }
        if (s_oshotStep != 0 && s_oshotOvl < 64 && s_ovlSeen[s_oshotOvl] != 0)
        {
            const uint32_t d = s_vsync - s_ovlSeen[s_oshotOvl];
            if (d % s_oshotStep == 0 && d / s_oshotStep < s_oshotCount)
            {
                if (FILE *t = std::fopen("hl_shot", "w"))
                    std::fclose(t);
                std::fprintf(s_txt, "v%u oshot %u\n", s_vsync, d / s_oshotStep);
            }
        }
        if (s_quit != 0 && s_vsync >= s_quit)
        {
            std::fprintf(s_txt, "v%u quit\n", s_vsync);
            std::fclose(s_bin);
            std::fclose(s_txt);
            hlProfDump(); // (HL_PROF: the samples, before the _Exit skips atexit)
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
            if (const char *v = std::getenv("HLB_RNGLOG"))
            {
                char *e;
                s_rngFrom = static_cast<uint32_t>(std::strtoul(v, &e, 0));
                s_rngTo = *e ? static_cast<uint32_t>(std::strtoul(e + 1, nullptr, 0)) : s_rngFrom;
            }
            s_cdLog = std::getenv("HLB_CDLOG") != nullptr;
            if (const char *r = std::getenv("HLB_OROUTE"))
                for (const char *p = r; *p;)
                {
                    char *e;
                    const uint32_t o = static_cast<uint32_t>(std::strtoul(p, &e, 10));
                    if (*e != '.')
                        break;
                    const uint32_t off = static_cast<uint32_t>(std::strtoul(e + 1, &e, 10));
                    if (*e != ':')
                        break;
                    const char *w = e + 1;
                    const char *c = std::strchr(w, ',');
                    s_oroute.push_back({o, off, std::string(w, c ? static_cast<size_t>(c - w) : std::strlen(w))});
                    p = c ? c + 1 : w + std::strlen(w);
                }
            if (const char *v = std::getenv("HLB_OSHOT"))
                std::sscanf(v, "%u,%u,%u", &s_oshotOvl, &s_oshotStep, &s_oshotCount);
            if (const char *r = std::getenv("HLB_FROUTE"))
                for (const char *p = r; *p;)
                {
                    char *e;
                    uint32_t a = static_cast<uint32_t>(std::strtoul(p, &e, 10)), fight = 1;
                    if (*e == '.')
                        fight = a, a = static_cast<uint32_t>(std::strtoul(e + 1, &e, 10));
                    if (*e != ':')
                        break;
                    const char *w = e + 1;
                    const char *c = std::strchr(w, ',');
                    s_froute.push_back({fight, a, std::string(w, c ? static_cast<size_t>(c - w) : std::strlen(w))});
                    p = c ? c + 1 : w + std::strlen(w);
                }
            if (const char *v = std::getenv("HLB_FNLOG"))
                for (const char *p = v; *p && s_fnCount < 8;)
                {
                    char *e;
                    s_fnAddr[s_fnCount++] = static_cast<uint32_t>(std::strtoul(p, &e, 0));
                    p = *e ? e + 1 : e;
                }
            s_cdBusy = std::getenv("HLB_CDBUSY") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_CDBUSY"))) : 0u;
            if (const char *v = std::getenv("HLB_RAMAT"))
                for (const char *p = v; *p;)
                {
                    char *e;
                    s_ramAt.push_back(static_cast<uint32_t>(std::strtoul(p, &e, 0)));
                    p = *e ? e + 1 : e;
                }
            s_p1cpu = std::getenv("HLB_P1CPU") ? static_cast<uint32_t>(std::strtoul(std::getenv("HLB_P1CPU"), nullptr, 0)) & 0xFFu : 0u;
            s_trapV = std::getenv("HLB_TRAPV") ? static_cast<uint32_t>(std::atoi(std::getenv("HLB_TRAPV"))) : 0u;
            if (const char *v = std::getenv("HLB_SHOTV"))
                for (const char *p = v; *p;)
                {
                    char *e;
                    s_shotV.push_back(static_cast<uint32_t>(std::strtoul(p, &e, 0)));
                    p = *e ? e + 1 : e;
                }
            if (const char *v = std::getenv("HLB_RAMV"))
                for (const char *p = v; *p;)
                {
                    char *e;
                    s_ramV.push_back(static_cast<uint32_t>(std::strtoul(p, &e, 0)));
                    p = *e ? e + 1 : e;
                }
            if (const char *v = std::getenv("HLB_VU1CAP"))
                std::sscanf(v, "%u,%u", &s_vuCapAt, &s_vuCapN);
            if (const char *v = std::getenv("HLB_GIFCAPV"))
            {
                char *e;
                s_gifCapV = static_cast<uint32_t>(std::strtoul(v, &e, 0));
                if (*e)
                    s_gifCapN = static_cast<uint32_t>(std::strtoul(e + 1, nullptr, 0));
            }
            if (const char *v = std::getenv("HLB_GIFCAP"))
            {
                char *e;
                s_gifCapAt = static_cast<uint32_t>(std::strtoul(v, &e, 0));
                if (*e)
                    s_gifCapN = static_cast<uint32_t>(std::strtoul(e + 1, nullptr, 0));
            }
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
