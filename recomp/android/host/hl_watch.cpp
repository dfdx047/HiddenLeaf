// Hidden Leaf (Android, lab builds only: HL_WATCH_BUILD=1 recomp/android/build.sh): HL_WATCH=<hex address> in hl_env.txt
// watches the 8 bytes there (patch 0061):
//   - [watch] the guest writes that go through ps2TraceGuestWrite (the runtime's memcpy/memset and slow stores), with the
//     writer's pc and ra;
//   - [watch-fast] the generated code's fast stores (Ps2FastWrite*), with the host address of the store (offset in libun5.so:
//     llvm-addr2line -f -e libun5.so <offset> names the recompiled function);
//   - [watch-poll] any change of the 8 bytes, whoever made it (DMA, the IOP, host code), seen at the next guest call or jump
//     (PS2Runtime::dispatchGuestBranch), with that branch's source and target.
// setup() runs from un5_main.cpp after hl_env.txt is loaded (a static initializer would run before it, at dlopen).
// HL_WATCH_PTRS=1 (for a heap list head): a write or change is logged only when a word of the watched 8 bytes stops looking
// like a pointer of that list (0, or 16-aligned in 0x100000-0x2000000), and then with the last 16 events before it; without
// it, the first 400 of each kind are logged.
#if defined(HL_WATCH_BUILD)
#include <dlfcn.h>
#include <atomic>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

namespace hl_watch
{
    uint32_t g_addr = 0xFFFFFFF0u; // nothing matches until setup() reads HL_WATCH

    namespace
    {
        std::atomic<int> s_hits{0}, s_fast{0}, s_polls{0};
        uint8_t s_last[8] = {};
        bool s_haveLast = false;
        constexpr int kMax = 400;
        bool s_ptrs = false;
        // the last events, printed before an implausible one (HL_WATCH_PTRS)
        constexpr int kRing = 16;
        char s_ring[kRing][200];
        unsigned s_ringPos = 0;
        std::atomic<int> s_bad{0};

        bool plausible(uint32_t v) { return v == 0u || ((v & 15u) == 0u && v >= 0x100000u && v < 0x02000000u); }

        void report(bool bad, const char *line)
        {
            if (!s_ptrs)
            {
                std::fputs(line, stderr);
                return;
            }
            if (bad && s_bad.fetch_add(1) < 40)
            {
                std::fprintf(stderr, "[watch-bad] the last %d events before it:\n", kRing);
                for (int i = 0; i < kRing; ++i)
                {
                    const char *e = s_ring[(s_ringPos + i) % kRing];
                    if (e[0] != 0)
                        std::fprintf(stderr, "  %s", e);
                }
                std::fputs(line, stderr);
            }
            std::snprintf(s_ring[s_ringPos % kRing], sizeof(s_ring[0]), "%s", line);
            ++s_ringPos;
        }

        uintptr_t libOffset(const void *p, const char **sym)
        {
            Dl_info info{};
            if (dladdr(p, &info) == 0 || info.dli_fbase == nullptr)
            {
                *sym = "?";
                return reinterpret_cast<uintptr_t>(p);
            }
            *sym = info.dli_sname != nullptr ? info.dli_sname : "-";
            return reinterpret_cast<uintptr_t>(p) - reinterpret_cast<uintptr_t>(info.dli_fbase);
        }
    }

    void setup()
    {
        const char *v = std::getenv("HL_WATCH");
        if (v == nullptr)
            return;
        g_addr = static_cast<uint32_t>(std::strtoul(v, nullptr, 16)) & ~3u;
        const char *ptrs = std::getenv("HL_WATCH_PTRS");
        s_ptrs = ptrs != nullptr && ptrs[0] == '1';
        std::fprintf(stderr, "[watch] watching 0x%08x+8%s\n", g_addr, s_ptrs ? ", logging only what is not a list pointer" : "");
    }

    void hit(uint32_t addr, uint32_t size, uint64_t lo, uint64_t hi, const char *op, uint32_t pc, uint32_t ra)
    {
        if (!s_ptrs && s_hits.fetch_add(1) >= kMax)
            return;
        // the words of the watched 8 bytes this write sets (a range write, from the runtime's memcpy/memset, has no value)
        bool bad = false;
        if (std::strncmp(op, "WRITE", 5) == 0)
            for (uint32_t w = g_addr; w < g_addr + 8u; w += 4u)
                if (w >= addr && w + 4u <= addr + size)
                {
                    const uint32_t off = w - addr;
                    const uint64_t half = off < 8u ? lo : hi;
                    bad |= !plausible(static_cast<uint32_t>(half >> ((off & 7u) * 8u)));
                }
        char line[200];
        std::snprintf(line, sizeof(line), "[watch] %s 0x%08x+%u = %016llx %016llx pc 0x%x ra 0x%x\n", op, addr, size,
                      static_cast<unsigned long long>(hi), static_cast<unsigned long long>(lo), pc, ra);
        report(bad, line);
    }

    __attribute__((noinline)) void hitFast(uint32_t addr, uint32_t size, uint64_t value)
    {
        if (!s_ptrs && s_fast.fetch_add(1) >= kMax)
            return;
        bool bad = false;
        for (uint32_t w = g_addr; w < g_addr + 8u; w += 4u)
            if (w >= addr && w + 4u <= addr + (size > 8u ? 8u : size))
                bad |= !plausible(static_cast<uint32_t>(value >> ((w - addr) * 8u)));
        const char *sym = nullptr;
        const uintptr_t off = libOffset(__builtin_return_address(0), &sym);
        char line[200];
        std::snprintf(line, sizeof(line), "[watch-fast] 0x%08x+%u = %016llx from libun5+0x%llx (%.60s)\n", addr, size,
                      static_cast<unsigned long long>(value), static_cast<unsigned long long>(off), sym);
        report(bad, line);
    }

    void poll(const uint8_t *rdram, uint32_t sourcePc, uint32_t targetPc, uint32_t ra)
    {
        if (g_addr >= 0x02000000u)
            return;
        const uint8_t *p = rdram + g_addr;
        if (s_haveLast && std::memcmp(p, s_last, 8) == 0)
            return;
        if (s_haveLast && (s_ptrs || s_polls.fetch_add(1) < kMax))
        {
            uint32_t was[2], now[2];
            std::memcpy(was, s_last, 8);
            std::memcpy(now, p, 8);
            char line[200];
            std::snprintf(line, sizeof(line), "[watch-poll] 0x%08x: %08x %08x -> %08x %08x, seen at branch 0x%x -> 0x%x (ra 0x%x)\n",
                          g_addr, was[0], was[1], now[0], now[1], sourcePc, targetPc, ra);
            report(!plausible(now[0]) || !plausible(now[1]), line);
        }
        std::memcpy(s_last, p, 8);
        s_haveLast = true;
    }
}
#endif
