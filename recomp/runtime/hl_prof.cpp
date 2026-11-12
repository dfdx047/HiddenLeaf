// Hidden Leaf: a sampling profiler inside the runtime (HL_PROF=1), for the PC (where gdb cannot attach: ptrace_scope 1, no
// perf) and for the devices. A CPU-time timer (ITIMER_PROF, HL_PROF_US microseconds, default 1000) interrupts whichever
// thread is running; the handler keeps the interrupted instruction address and the thread id. hlProfDump() (at exit, and
// by the bench before its _Exit) writes hl_prof.txt: "module offset tid count" per sampled address, for
// tools/hlprof.py (addr2line on the module) to turn into functions. HL_PROF_EVERY=<s> also writes it while running.
#if defined(__linux__) || defined(__ANDROID__)
#include <atomic>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <map>
#include <string>
#include <tuple>
#include <thread>
#include <chrono>
#include <sys/syscall.h>
#include <sys/time.h>
#include <ucontext.h>
#include <unistd.h>
#include <utility>

void hlProfDump();

namespace hl_prof_detail
{
    void write(bool final);
}

namespace hl_prof_detail
{
    constexpr uint32_t kMax = 1u << 22; // samples kept (4M: over an hour at 1 kHz)
    uintptr_t *g_pc = nullptr;
    uintptr_t *g_lr = nullptr; // (the link register on arm64: the caller of a leaf function, for hlprof.py --callers)
    int32_t *g_tid = nullptr;
    std::atomic<uint32_t> g_count{0};
    std::atomic<bool> g_dumped{false};

    void onSample(int, siginfo_t *, void *uc)
    {
        const uint32_t i = g_count.fetch_add(1, std::memory_order_relaxed);
        if (i >= kMax)
            return;
        const ucontext_t *u = static_cast<const ucontext_t *>(uc);
#if defined(__x86_64__)
        g_pc[i] = static_cast<uintptr_t>(u->uc_mcontext.gregs[REG_RIP]);
#elif defined(__aarch64__)
        g_pc[i] = static_cast<uintptr_t>(u->uc_mcontext.pc);
        g_lr[i] = static_cast<uintptr_t>(u->uc_mcontext.regs[30]);
#else
        g_pc[i] = 0;
#endif
        g_tid[i] = static_cast<int32_t>(syscall(SYS_gettid));
    }

    void start()
    {
        {
            static std::atomic<bool> s_started{false};
            const char *on = std::getenv("HL_PROF");
            if (on == nullptr || on[0] == '\0' || on[0] == '0' || s_started.exchange(true))
                return;
            g_pc = static_cast<uintptr_t *>(std::calloc(kMax, sizeof(uintptr_t)));
            g_lr = static_cast<uintptr_t *>(std::calloc(kMax, sizeof(uintptr_t)));
            g_tid = static_cast<int32_t *>(std::calloc(kMax, sizeof(int32_t)));
            if (g_pc == nullptr || g_lr == nullptr || g_tid == nullptr)
                return;
            struct sigaction sa;
            std::memset(&sa, 0, sizeof(sa));
            sa.sa_sigaction = &onSample;
            sa.sa_flags = SA_SIGINFO | SA_RESTART;
            sigemptyset(&sa.sa_mask);
            sigaction(SIGPROF, &sa, nullptr);
            const long us = std::getenv("HL_PROF_US") ? std::atol(std::getenv("HL_PROF_US")) : 1000L;
            itimerval t;
            t.it_interval.tv_sec = us / 1000000L;
            t.it_interval.tv_usec = us % 1000000L;
            t.it_value = t.it_interval;
            setitimer(ITIMER_PROF, &t, nullptr);
            std::atexit(&hlProfDump);
            // HL_PROF_EVERY=<seconds>: also written while running (the devices: the app is killed, atexit never runs)
            if (const char *every = std::getenv("HL_PROF_EVERY"))
            {
                const int seconds = std::atoi(every);
                if (seconds > 0)
                    std::thread([seconds] {
                        for (;;)
                        {
                            std::this_thread::sleep_for(std::chrono::seconds(seconds));
                            write(false);
                        }
                    }).detach();
            }
            std::fprintf(stderr, "[prof] sampling every %ld us (HL_PROF)\n", us);
        }
    }

    struct ProfInstall
    {
        ProfInstall() { start(); }
    } profInstall;
}

// The Android host sets the variables of hl_env.txt after the libraries are loaded: it calls this then.
void hlProfStart() { hl_prof_detail::start(); }

void hlProfDump()
{
    using namespace hl_prof_detail;
    if (g_pc == nullptr || g_dumped.exchange(true))
        return;
    itimerval off{};
    setitimer(ITIMER_PROF, &off, nullptr);
    write(true);
}

void hl_prof_detail::write(bool final)
{
    using namespace hl_prof_detail;
    if (g_pc == nullptr)
        return;
    const uint32_t n = std::min(g_count.load(), kMax);
    std::map<std::pair<uintptr_t, int32_t>, uint32_t> counts;
    for (uint32_t i = 0; i < n; ++i)
        ++counts[{g_pc[i], g_tid[i]}];
    const char *path = std::getenv("HL_PROF_OUT") ? std::getenv("HL_PROF_OUT") : "hl_prof.txt";
    FILE *f = std::fopen(path, "w");
    if (f == nullptr)
        return;
    std::fprintf(f, "# samples %u\n", n);
    for (const auto &c : counts)
    {
        Dl_info info{};
        const char *module = "?";
        uintptr_t offset = c.first.first;
        if (dladdr(reinterpret_cast<void *>(c.first.first), &info) != 0 && info.dli_fname != nullptr)
        {
            module = info.dli_fname;
            offset = c.first.first - reinterpret_cast<uintptr_t>(info.dli_fbase);
        }
        std::fprintf(f, "%s 0x%zx %d %u\n", module, static_cast<size_t>(offset), c.first.second, c.second);
    }
    std::fclose(f);
#if defined(__aarch64__)
    // <path>.lr: "pcmodule pcoffset lrmodule lroffset tid count", who called the sampled function
    {
        const auto where = [](uintptr_t a, const char *&module, uintptr_t &offset) {
            Dl_info info{};
            module = "?";
            offset = a;
            if (dladdr(reinterpret_cast<void *>(a), &info) != 0 && info.dli_fname != nullptr)
            {
                module = info.dli_fname;
                offset = a - reinterpret_cast<uintptr_t>(info.dli_fbase);
            }
        };
        std::map<std::tuple<uintptr_t, uintptr_t, int32_t>, uint32_t> pairs;
        for (uint32_t i = 0; i < n; ++i)
            ++pairs[{g_pc[i], g_lr[i], g_tid[i]}];
        const std::string lrPath = std::string(path) + ".lr";
        if (FILE *g = std::fopen(lrPath.c_str(), "w"))
        {
            for (const auto &c : pairs)
            {
                const char *pm, *lm;
                uintptr_t po, lo;
                where(std::get<0>(c.first), pm, po);
                where(std::get<1>(c.first), lm, lo);
                std::fprintf(g, "%s 0x%zx %s 0x%zx %d %u\n", pm, static_cast<size_t>(po), lm, static_cast<size_t>(lo),
                             std::get<2>(c.first), c.second);
            }
            std::fclose(g);
        }
    }
#endif
    std::fprintf(stderr, "[prof] %u samples in %s%s\n", n, path, final ? "" : " (so far)");
}
#else
void hlProfStart() {}
void hlProfDump() {}
#endif
