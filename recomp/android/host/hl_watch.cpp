// Hidden Leaf (Android, lab builds only: HL_WATCH_BUILD=1 recomp/android/build.sh): HL_WATCH=<hex address> in hl_env.txt logs
// each guest write that touches the 8 bytes there (the generated code's stores, the runtime's memcpy/memset, the IOP's copies
// into the EE), with the writer's pc and ra. The hook is ps2TraceGuestWrite in runtime/ps2_runtime.h (patch 0061).
#if defined(HL_WATCH_BUILD)
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <atomic>

namespace hl_watch
{
    uint32_t g_addr = []
    {
        const char *v = std::getenv("HL_WATCH");
        return v != nullptr ? static_cast<uint32_t>(std::strtoul(v, nullptr, 16)) : 0xFFFFFFF0u;
    }();

    void hit(uint32_t addr, uint32_t size, uint64_t lo, uint64_t hi, const char *op, uint32_t pc, uint32_t ra)
    {
        static std::atomic<int> s_count{0};
        if (s_count.fetch_add(1) >= 400)
            return;
        std::fprintf(stderr, "[watch] %s 0x%08x+%u = %016llx %016llx pc 0x%x ra 0x%x\n", op, addr, size,
                     static_cast<unsigned long long>(hi), static_cast<unsigned long long>(lo), pc, ra);
    }
}
#endif
