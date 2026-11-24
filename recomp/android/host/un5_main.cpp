// SDL_main of the Hidden Leaf engine (libun5.so). GameActivity passes the game folder, the app's native libraries, the GPU
// driver (folder and file, "" for the system's) and the base ISO as /proc/self/fd/N.
//
// Three threads: the game (the runtime's main(), with no window: its GS stream goes into a ring, hl_gs_live.cpp), PCSX2's GS
// (libbt4gsx.so, reading the ring) and this one, which keeps the window, its events and the pad.
#include "hl_game.h"
#include "hl_host_pad.h"
#include "online/hl_online.h"
#include "ps2_live_ring.h"
#include "runtime/gs/hl_gs_live.h"
#include "runtime/hl_perf.h"

#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <pthread.h>
#include <signal.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <ucontext.h>
#include <unistd.h>

#include <atomic>
#include <cerrno>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <thread>

int ps2x_runtime_main(int argc, char *argv[]); // the runtime's main() (src/main.cpp, renamed by the build)
#if defined(HL_WATCH_BUILD)
namespace hl_watch
{
    void setup();
}
#endif
namespace hl_nominum_vu1
{
    void setup(); // NOMINUM_VU1=1: Nominum's VIF1 and VU1 (hl_nominum_vu1.cpp)
}
void hlProfStart();                            // recomp/runtime/hl_prof.cpp
void hl_spu2_install();                        // the sound (recomp/android/spu2/hl_spu2.cpp)
void hl_spu2_pause(bool paused);

namespace
{
    // True when the ELF at mod has the base's program headers and the same bytes where the base has code (tools/un5.py
    // CODE_RANGES): a mod that changed only the program's data (the Alpha 1.1X: tables), which this engine can run with its
    // own program. Anything else needs the mod's code pack.
    bool sameCodeAsBase(const std::string &base, const std::string &mod)
    {
        auto readAll = [](const std::string &path, std::string &out) {
            FILE *f = std::fopen(path.c_str(), "rb");
            if (f == nullptr)
                return false;
            char buf[1 << 16];
            size_t n;
            while ((n = std::fread(buf, 1, sizeof(buf), f)) > 0)
                out.append(buf, n);
            std::fclose(f);
            return true;
        };
        std::string a, b;
        if (!readAll(base, a) || !readAll(mod, b) || a.size() < 0x34 || a.size() != b.size())
            return false;
        uint32_t phoff = 0;
        uint16_t phnum = 0;
        std::memcpy(&phoff, a.data() + 0x1c, 4);
        std::memcpy(&phnum, a.data() + 0x2c, 2);
        if (std::memcmp(a.data(), b.data(), 0x34) != 0 || phoff + phnum * 32u > a.size() ||
            std::memcmp(a.data() + phoff, b.data() + phoff, phnum * 32u) != 0)
            return false;
        uint32_t off = 0, vaddr = 0, filesz = 0;
        std::memcpy(&off, a.data() + phoff + 4, 4);
        std::memcpy(&vaddr, a.data() + phoff + 8, 4);
        std::memcpy(&filesz, a.data() + phoff + 16, 4);
        static const uint32_t kCode[][2] = {{0x100000u, 0x3d5760u}, {0x5e3a00u, 0x5e7208u}};
        for (const auto &r : kCode)
        {
            if (r[0] < vaddr || r[1] > vaddr + filesz || off + (r[1] - vaddr) > a.size())
                return false;
            if (std::memcmp(a.data() + off + (r[0] - vaddr), b.data() + off + (r[0] - vaddr), r[1] - r[0]) != 0)
                return false;
        }
        return true;
    }
}

namespace
{
    const char TAG[] = "HiddenLeaf";
    std::atomic<bool> g_quit{false};
    std::atomic<unsigned> g_touchButtons{0};
    std::atomic<unsigned> g_touchAxes{0x80808080u};

    // ---- everything the engine prints: logcat, and un5_log.txt in the game folder (the two before it are kept)
    int g_pipe[2];
    FILE *g_log = nullptr;

    void *logThread(void *)
    {
        char buf[2048];
        size_t used = 0;
        for (;;)
        {
            const ssize_t n = read(g_pipe[0], buf + used, sizeof(buf) - 1 - used);
            if (n <= 0)
            {
                if (n < 0 && errno == EINTR)
                    continue;
                break;
            }
            used += static_cast<size_t>(n);
            for (;;)
            {
                char *nl = static_cast<char *>(std::memchr(buf, '\n', used));
                size_t len = nl ? static_cast<size_t>(nl - buf) : used == sizeof(buf) - 1 ? used : 0;
                if (len == 0 && !nl)
                    break;
                buf[len] = '\0';
                __android_log_write(ANDROID_LOG_INFO, TAG, buf);
                if (g_log)
                {
                    std::fprintf(g_log, "%s\n", buf);
                    std::fflush(g_log);
                }
                len += nl != nullptr;
                std::memmove(buf, buf + len, used - len);
                used -= len;
            }
        }
        return nullptr;
    }

    void redirectOutput(const std::string &dir)
    {
        const std::string path = dir + "/un5_log.txt", old = dir + "/un5_log.1.txt", older = dir + "/un5_log.2.txt";
        std::rename(old.c_str(), older.c_str());
        std::rename(path.c_str(), old.c_str());
        g_log = std::fopen(path.c_str(), "w");
        setvbuf(stdout, nullptr, _IOLBF, 0);
        setvbuf(stderr, nullptr, _IONBF, 0);
        if (pipe(g_pipe) == 0)
        {
            dup2(g_pipe[1], 1);
            dup2(g_pipe[1], 2);
            pthread_t th;
            pthread_create(&th, nullptr, logThread, nullptr);
            pthread_detach(th);
        }
    }

    // ---- libbt4gsx.so (recomp/android/README.md)
    struct GsxArgs
    {
        void *ring;
        void *window;
        int width, height;
        const char *root;
        const char *ini;
        const char *renderer;
        float upscale;
        const char *driverDir;
        const char *driverLib;
        const char *hookDir;
    };

    // The launcher's settings (un5_settings.txt: name=number lines). See android/.../data/EngineSettings.kt.
    struct Settings
    {
        int scale4 = 4;      // internal resolution in quarters: 4 = 1x
        int filter = 0;      // on the way to the screen: 0 bilinear, 1 sharp bilinear, 2 FXAA, 3 CAS
        int aspectMilli = 0; // the shape of the picture: 0 the screen's, 1333 the game's, or width/height*10000
        int fpsMode = 50;    // 30, 50 (the game's), 60 or 120 (the last two by interpolation)
    };
    Settings g_settings;
    // The launcher's "Modo leve" (light_mode=1, on by default since 0107/0108 fixed the DIRECT-PACKED bug):
    // Nominum's VIF1 and VU1 (zero-copy) and, on weak/mid 4-big-core SoCs, the GS in two threads.
    // NOMINUM_VU1 and HL_GS_SPLIT in hl_env.txt still decide over it.
    bool g_lightMode = true;

    void loadSettings(const std::string &path)
    {
        FILE *fp = std::fopen(path.c_str(), "r");
        if (!fp)
            return;
        char line[128];
        bool skipIntro = true, nativeRender = false;
        int gpuApi = -1; // the launcher's renderer (Renderer.kt): 0 Vulkan, 1 OpenGL ES
        while (std::fgets(line, sizeof(line), fp))
        {
            char *eq = std::strchr(line, '=');
            if (!eq)
                continue;
            *eq = '\0';
            const int v = std::atoi(eq + 1);
            if (!std::strcmp(line, "scale4"))
                g_settings.scale4 = v;
            else if (!std::strcmp(line, "filter"))
                g_settings.filter = v;
            else if (!std::strcmp(line, "aspect_milli"))
                g_settings.aspectMilli = v;
            else if (!std::strcmp(line, "fps_mode"))
                g_settings.fpsMode = v;
            else if (!std::strcmp(line, "dr_unlock_all"))
                hl_game::g_unlockAll = v != 0;
            else if (!std::strcmp(line, "special_one"))
                hl_game::g_specialOn = v != 0;
            else if (!std::strcmp(line, "special_button"))
                hl_game::g_specialMask = static_cast<uint32_t>(v);
            else if (!std::strcmp(line, "mash_percent"))
                hl_game::g_mashPercent = v;
            else if (!std::strcmp(line, "skip_intro"))
                skipIntro = v != 0;
            else if (!std::strcmp(line, "native_render"))
                nativeRender = v != 0;
            else if (!std::strcmp(line, "light_mode"))
                g_lightMode = v != 0;
            else if (!std::strcmp(line, "gpu_api"))
                gpuApi = v;
        }
        std::fclose(fp);
        // the logos and the opening movies skipped by START (Pad.cpp, HL_SKIP_INTRO): on unless the launcher says off, and not
        // under a test route (its buttons are by vsync) or a value of hl_env.txt
        if (skipIntro && std::getenv("HL_PADROUTE") == nullptr)
            setenv("HL_SKIP_INTRO", "1", 0);
        // the renderer: PCSX2's GS draws everything unless the player turns the native one on (a value of hl_env.txt wins).
        // 2026-11-12 on the Mi A3 at 1x the GS gave 42/36 fps against the native's 44/38 and draws every effect.
        if (!nativeRender)
            setenv("HL_NATIVE", "0", 0);
        // PCSX2's GS on Vulkan or OpenGL ES, as chosen in the launcher (a value of hl_env.txt wins; without a line, Vulkan)
        if (gpuApi >= 0)
            setenv("HL_GS_API", gpuApi == 1 ? "gl" : "vulkan", 0);
    }

    // What libbt4gsx changes while the game runs (Bt4Gsx_SetVideo).
    struct GsxVideo
    {
        float upscale;
        int filter;
        float aspect;   // the fight's and the campaign's picture, width/height; 0 stretched to the screen
        float hudScale; // what undoes the stretch on the HUD
        int fpsMode;
    };
    void (*g_gsxSetVideo)(const GsxVideo *) = nullptr;
    int (*g_gsxPresentRate)() = nullptr;
    int g_windowW = 0, g_windowH = 0;

    // The shape asked for against the screen: a wider picture is the 3D's horizontal scale, 0.75 at 16:9, as the
    // community's patch for this game does it. On a screen no wider than 4:3 (the RG Cube) nothing changes.
    void applyVideo()
    {
        const float screen = g_windowH > 0 ? static_cast<float>(g_windowW) / static_cast<float>(g_windowH) : 4.0f / 3.0f;
        float shape = g_settings.aspectMilli > 0 ? static_cast<float>(g_settings.aspectMilli) / 1000.0f : screen;
        if (shape > screen) // (the screen's width is all there is)
            shape = screen;
        const float game = 4.0f / 3.0f;
        // (online too, each phone with its own screen's camera: the developer, 2026-11-14, wants the picture whole and filling the
        // screen on both phones. The scale at camera+0x274 is read by the projection only (0x10DD00, 0x10F420, copied by
        // 0x1C01B0); a fight with one phone at 20:9 and the other at 4:3 kept the same memory, docs/registro.md 2026-11-14)
        const float x = shape > game + 0.01f ? game / shape : 1.0f;
        hl_game::g_cameraXScale.store(x, std::memory_order_relaxed);
        if (g_gsxSetVideo)
        {
            const GsxVideo v{static_cast<float>(g_settings.scale4) / 4.0f, g_settings.filter, x < 1.0f ? shape : game, x, g_settings.fpsMode};
            g_gsxSetVideo(&v);
        }
    }

    // The GS settings: nothing of the game's own, only what suits a phone. HL_GS_SCALE: the internal resolution.
    void writeIni(const std::string &path, float upscale)
    {
        FILE *f = std::fopen(path.c_str(), "w");
        if (!f)
            return;
        std::fprintf(f,
                     "; Ultimate Ninja 5 on PCSX2's GS: written by the game at every start\n"
                     "[EmuCore/GS]\n"
                     "upscale_multiplier = %g\n"
                     "MaxAnisotropy = 0\n"
                     "filter = 2\n"
                     "fxaa = %s\n"
                     "CASMode = %d\n"
                     "linear_present_mode = %d\n"
                     "AspectRatio = 4:3\n"
                     "OsdShowFPS = %s\n"
                     "VsyncEnable = 0\n"
                     "pcrtc_antiblur = false\n"
                     "UserHacks = false\n"
                     // Mali without dual-source blending: blending by fetch instead of a barrier per primitive (Mali-G57, RG Cube:
                     // barriers 1878 -> 0, passes 519 -> 22, GPU 75% -> 13%, right image; ARMSX2's list denies it on this part).
                     // Adreno ignores it.
                     "ForceMaliFramebufferFetch = true\n"
                     "DumpReplaceableTextures = false\n",
                     static_cast<double>(upscale), g_settings.filter == 2 ? "true" : "false", g_settings.filter == 3 ? 1 : 0, g_settings.filter == 1 ? 2 : 1,
                     std::getenv("HL_PERF") ? "true" : "false");
        // PCSX2's GS in two threads (GSBackThreadMode: one parses the GIF, "GS Back" draws) where the GS is the limit of a
        // device with four fast cores: on the Mi A3 (Snapdragon 665, "trinket") the GS thread of a heavy fight went from 22-27
        // to 12-16 ms (docs/registro.md, 2026-11-13). Only with the GS on Vulkan and the native renderer off (hl_native's
        // fight path works on the renderer object from the MTGS thread). The IOP then goes to the slow cores: five busy
        // threads on four fast ones cost 2-3 fps. HL_GS_SPLIT=0/1 decides on any device.
        {
            const char *split = std::getenv("HL_GS_SPLIT");
            const char *nat = std::getenv("HL_NATIVE");
            const char *api = std::getenv("HL_GS_API");
            char platform[PROP_VALUE_MAX] = {};
            __system_property_get("ro.board.platform", platform);
            const bool gsOnly = nat != nullptr && nat[0] == '0' && (api == nullptr || std::strcmp(api, "gl") != 0);
            const bool splitPlatform = std::strcmp(platform, "trinket") == 0 || std::strcmp(platform, "bengal") == 0 ||
                                       std::strcmp(platform, "khaje") == 0 || std::strcmp(platform, "ums9620") == 0 ||
                                       std::strncmp(platform, "mt676", 5) == 0 || std::strncmp(platform, "mt678", 5) == 0;
            const bool on = split != nullptr ? split[0] == '1' : g_lightMode && gsOnly && splitPlatform;
            if (on)
            {
                std::fprintf(f, "GSBackThreadMode = 1\n");
                setenv("HL_PIN_IOP", "rest", 0);
            }
            std::fprintf(stderr, "un5: GS in %s (platform %s)\n", on ? "two threads" : "one thread", platform);
        }
        // HL_GS_EXTRA="key=value;key=value": GS settings to try (bench only)
        if (const char *extra = std::getenv("HL_GS_EXTRA"))
        {
            std::string text = extra;
            for (char &c : text)
                if (c == ';')
                    c = '\n';
            std::fprintf(f, "%s\n", text.c_str());
        }
        std::fclose(f);
    }

    void *windowHandle(SDL_Window *w, int *pw, int *ph)
    {
        SDL_GetWindowSizeInPixels(w, pw, ph);
        return SDL_GetPointerProperty(SDL_GetWindowProperties(w), SDL_PROP_WINDOW_ANDROID_WINDOW_POINTER, nullptr);
    }

    // Player 1: the touch controller and the first gamepad, together.
    SDL_Gamepad *g_pad = nullptr;

    std::atomic<bool> g_padHeld{false};

    void updatePad()
    {
        unsigned buttons = g_touchButtons.load(std::memory_order_relaxed);
        unsigned axes = g_touchAxes.load(std::memory_order_relaxed);
        // (a controller gone in the middle of a game: let go, and the next one connected is opened; the developer, 2026-11-14)
        if (g_pad && !SDL_GamepadConnected(g_pad))
        {
            SDL_CloseGamepad(g_pad);
            g_pad = nullptr;
        }
        if (!g_pad)
        {
            int count = 0;
            if (SDL_JoystickID *ids = SDL_GetGamepads(&count))
            {
                if (count > 0)
                    g_pad = SDL_OpenGamepad(ids[0]);
                SDL_free(ids);
            }
        }
        if (g_pad)
        {
            static const struct { SDL_GamepadButton b; unsigned bit; } kMap[] = {
                {SDL_GAMEPAD_BUTTON_BACK, 0x0001u}, {SDL_GAMEPAD_BUTTON_LEFT_STICK, 0x0002u}, {SDL_GAMEPAD_BUTTON_RIGHT_STICK, 0x0004u},
                {SDL_GAMEPAD_BUTTON_START, 0x0008u}, {SDL_GAMEPAD_BUTTON_DPAD_UP, 0x0010u}, {SDL_GAMEPAD_BUTTON_DPAD_RIGHT, 0x0020u},
                {SDL_GAMEPAD_BUTTON_DPAD_DOWN, 0x0040u}, {SDL_GAMEPAD_BUTTON_DPAD_LEFT, 0x0080u}, {SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, 0x0400u},
                {SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, 0x0800u}, {SDL_GAMEPAD_BUTTON_NORTH, 0x1000u}, {SDL_GAMEPAD_BUTTON_EAST, 0x2000u},
                {SDL_GAMEPAD_BUTTON_SOUTH, 0x4000u}, {SDL_GAMEPAD_BUTTON_WEST, 0x8000u}};
            for (const auto &m : kMap)
                if (SDL_GetGamepadButton(g_pad, m.b))
                    buttons |= m.bit;
            if (SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_LEFT_TRIGGER) > 8000)
                buttons |= 0x0100u;
            if (SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) > 8000)
                buttons |= 0x0200u;
            if ((axes & 0xFFFFu) == 0x8080u)
            {   // (the touch stick is at rest: the gamepad's)
                const auto axis = [](int v) { return static_cast<unsigned>((v + 32768) >> 8) & 0xFFu; };
                const unsigned lx = axis(SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_LEFTX));
                const unsigned ly = axis(SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_LEFTY));
                const unsigned rx = axis(SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_RIGHTX));
                const unsigned ry = axis(SDL_GetGamepadAxis(g_pad, SDL_GAMEPAD_AXIS_RIGHTY));
                axes = lx | ly << 8 | rx << 16 | ry << 24;
            }
        }
        if (g_padHeld.load(std::memory_order_relaxed))
        {   // (the online menu over the Mode Select: the game goes on, its music and Naruto too, but the buttons are the menu's)
            buttons = 0u;
            axes = 0x80808080u;
        }
        g_hlPadButtons.store(buttons, std::memory_order_relaxed);
        g_hlPadAxes.store(axes, std::memory_order_relaxed);
    }

    // NAME=value lines in hl_env.txt in the game folder: the switches for testing (HL_AUTOPRESS, HL_PADROUTE, HL_PERF...)
    void loadEnvFile(const std::string &path)
    {
        FILE *fp = std::fopen(path.c_str(), "r");
        if (!fp)
            return;
        // (lines of any length: a long HL_PADROUTE was cut at 2048 bytes and the route stopped halfway, with no message)
        char *line = nullptr;
        size_t capacity = 0u;
        while (getline(&line, &capacity, fp) != -1)
        {
            line[std::strcspn(line, "\r\n")] = '\0';
            char *eq = std::strchr(line, '=');
            if (line[0] != '#' && eq)
            {
                *eq = '\0';
                setenv(line, eq + 1, 1);
                std::fprintf(stderr, "un5: env %s=%s\n", line, eq + 1);
            }
        }
        std::free(line);
        std::fclose(fp);
    }

    // A fault in any thread: where, with the registers. (PCSX2's handler says only "Aborting application".)
    void faultHandler(int sig, siginfo_t *si, void *ctx)
    {
        const mcontext_t &m = static_cast<ucontext_t *>(ctx)->uc_mcontext;
        Dl_info info{};
        dladdr(reinterpret_cast<void *>(m.pc), &info);
        std::fprintf(stderr, "un5: signal %d at address %p, pc %p (%s+0x%lx, %s)\n", sig, si->si_addr, reinterpret_cast<void *>(m.pc),
                     info.dli_fname ? info.dli_fname : "?", static_cast<unsigned long>(m.pc - reinterpret_cast<uintptr_t>(info.dli_fbase)),
                     info.dli_sname ? info.dli_sname : "?");
        for (int i = 0; i < 31; i += 4)
            std::fprintf(stderr, "un5:  x%-2d %016llx %016llx %016llx %016llx\n", i, static_cast<unsigned long long>(m.regs[i]),
                         static_cast<unsigned long long>(i + 1 < 31 ? m.regs[i + 1] : 0), static_cast<unsigned long long>(i + 2 < 31 ? m.regs[i + 2] : 0),
                         static_cast<unsigned long long>(i + 3 < 31 ? m.regs[i + 3] : 0));
        std::fprintf(stderr, "un5:  sp %016llx\n", static_cast<unsigned long long>(m.sp));
        usleep(200000); // (the log's thread)
        signal(sig, SIG_DFL);
        raise(sig);
    }

    void installFaultHandler()
    {
        struct sigaction sa{};
        sa.sa_sigaction = faultHandler;
        sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
        sigemptyset(&sa.sa_mask);
        sigaction(SIGSEGV, &sa, nullptr);
        sigaction(SIGBUS, &sa, nullptr);
    }

    void fail(const char *message)
    {
        std::fprintf(stderr, "un5: %s\n", message);
        SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "Hidden Leaf", message, nullptr);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetPad(JNIEnv *, jclass, jint buttons, jint lx, jint ly,
                                                                                              jint rx, jint ry)
{
    // HL_PAD_LOG=1: each change of the touch buttons with the vsync and the time, to check the one-touch sequences (Jutsu,
    // Ougi, ↑↑○, ↓↓○) as the game gets them
    static const bool s_padLog = std::getenv("HL_PAD_LOG") != nullptr;
    if (s_padLog && static_cast<unsigned>(buttons) != g_touchButtons.load(std::memory_order_relaxed))
        std::fprintf(stderr, "[pad] v%u %llu ms touch %04x\n", hl_game::g_vsync.load(std::memory_order_relaxed),
                     static_cast<unsigned long long>(SDL_GetTicks()), static_cast<unsigned>(buttons));
    g_touchButtons.store(static_cast<unsigned>(buttons), std::memory_order_relaxed);
    g_touchAxes.store(static_cast<unsigned>(lx & 0xFF) | static_cast<unsigned>(ly & 0xFF) << 8 | static_cast<unsigned>(rx & 0xFF) << 16 |
                          static_cast<unsigned>(ry & 0xFF) << 24,
                      std::memory_order_relaxed);
}
extern "C" JNIEXPORT jboolean JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeMenuOpen(JNIEnv *, jclass) { return JNI_FALSE; }
extern "C" JNIEXPORT jint JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeScene(JNIEnv *, jclass) { return hl_game::scene(); }
extern "C" JNIEXPORT jint JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeDivider(JNIEnv *, jclass) { return hl_game::divider(); }
extern "C" JNIEXPORT jint JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativePresentRate(JNIEnv *, jclass) { return g_gsxPresentRate ? g_gsxPresentRate() : 0; }
// The panel: the game frozen (EE, IOP and sound together) while it is open
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetPaused(JNIEnv *, jclass, jboolean paused)
{
    hl_game::g_paused.store(paused ? 1 : 0, std::memory_order_release);
    hl_spu2_pause(paused);
}
// The panel: resolution, filter, shape and frame rate, in effect from the next frame
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetVideo(JNIEnv *, jclass, jint scale4, jint filter, jint aspectMilli, jint fpsMode)
{
    g_settings = {scale4, filter, aspectMilli, fpsMode};
    applyVideo();
}
// The game menu's Controls tab: the one-tap special, its button, and what a tap is worth where the game asks to press repeatedly, at once
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetAssist(JNIEnv *, jclass, jint specialOne, jint specialButton, jint mashPercent)
{
    if (hl_online::active()) // (an online match plays by the game's own rules on both phones: hl_online.cpp)
        return;
    hl_game::g_specialOn.store(specialOne != 0, std::memory_order_relaxed);
    if (specialButton != 0)
        hl_game::g_specialMask.store(static_cast<uint32_t>(specialButton), std::memory_order_relaxed);
    hl_game::g_mashPercent.store(mashPercent, std::memory_order_relaxed);
}
// "Naruto Online" in the game's Mode Select (hl_game.cpp, docs/menu-online.md): the entry on or off, and how many times the
// player chose it (the launcher polls it and opens its lobby when it goes up)
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetMenuOnline(JNIEnv *, jclass, jboolean on)
{
    // (an online match boots both games in lockstep: the menu must be the same on both, so the game's own)
    hl_game::g_menuOnline.store(on != JNI_FALSE && !hl_online::active(), std::memory_order_relaxed);
}
extern "C" JNIEXPORT jint JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeOnlineMenu(JNIEnv *, jclass) { return hl_game::g_onlineMenu.load(std::memory_order_relaxed); }
extern "C" JNIEXPORT jint JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeMenuTap(JNIEnv *, jclass, jfloat nx, jfloat ny) { return hl_game::menuTap(nx, ny); }
// The online menu open over the game (OnlineMenu.kt): the controller and the touch pad move the menu, not the game behind it
extern "C" JNIEXPORT void JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativeSetPadHeld(JNIEnv *, jclass, jboolean held)
{
    g_padHeld.store(held != JNI_FALSE, std::memory_order_relaxed);
}
// HL_PERF's line of the last second: "" when it is off
extern "C" JNIEXPORT jstring JNICALL Java_com_dfdx047_hiddenleaf_engine_TouchBridge_nativePerfLine(JNIEnv *env, jclass) { return env->NewStringUTF(hl_perf::line()); }

extern "C" __attribute__((visibility("default"))) int SDL_main(int argc, char *argv[])
{
    const std::string root = argc > 1 ? argv[1] : ".";
    const std::string libDir = argc > 2 ? argv[2] : "";
    const std::string driverDir = argc > 3 ? argv[3] : "";
    const std::string driverLib = argc > 4 ? argv[4] : "";
    const std::string iso = argc > 5 ? argv[5] : "";
    const std::string data = root + "/runtime/data";
    redirectOutput(root);
    std::fprintf(stderr, "un5: Hidden Leaf, Ultimate Ninja 5 engine\n");
    loadEnvFile(root + "/hl_env.txt");
    hlProfStart(); // (HL_PROF: the sampling profiler, recomp/runtime/hl_prof.cpp, with the variables just read)
#if defined(HL_WATCH_BUILD)
    hl_watch::setup();
#endif
    hl_game::g_gsDumpDir = root + "/gsdumps";
    if (std::getenv("HL_GS_DUMP"))
        mkdir(hl_game::g_gsDumpDir.c_str(), 0775);
    hl_game::g_vifDumpDir = root + "/vifdumps";
    if (std::getenv("HL_VIF_DUMP"))
        mkdir(hl_game::g_vifDumpDir.c_str(), 0775);
    loadSettings(root + "/un5_settings.txt");
    hl_online::setup(root); // (the app's Online screen wrote a session file: this start is an online match)
    // Nominum's VIF1 and VU1 (its recompiled microprograms) by default: every route of the week ran with them; the
    // runtime's VU1 gave on the Mi A3 35 fps in the fight's start (48.9 with Nominum) and 25.5 in the campaign (35.2),
    // with the same picture (2026-11-12). NOMINUM_VU1=0 in hl_env.txt for the runtime's.
    // (since 2026-11-13 only in the light mode, g_lightMode above)
    setenv("NOMINUM_VU1", g_lightMode ? "1" : "0", 0);
    std::fprintf(stderr, "un5: light mode %s\n", g_lightMode ? "on" : "off");
    hl_nominum_vu1::setup();
    setenv("HL_METER", "1", 0);
    if (iso.empty())
    {
        fail("The game's disc image cannot be read. Import it again in the launcher.");
        return 1;
    }
    if (chdir(data.c_str()) != 0)
    {
        fail("The game's files are not there. Import the game again in the launcher.");
        return 1;
    }
    setenv("HL_CD_IMAGE", iso.c_str(), 1);
    // The mods switched on (the launcher's un5_mods.txt, "modlib/<id>/disc" in order): their files over the disc's
    // (hl_image.cpp). A mod's own program runs only from its code pack, the libun5_<id>.so built for it (HL_PACK_ID).
    static std::string modElf;
    {
        std::string dirs;
        if (FILE *f = std::fopen((root + "/un5_mods.txt").c_str(), "r"))
        {
            char line[512];
            while (std::fgets(line, sizeof(line), f))
            {
                std::string dir = line;
                while (!dir.empty() && (dir.back() == '\n' || dir.back() == '\r' || dir.back() == ' '))
                    dir.pop_back();
                if (dir.empty())
                    continue;
                dirs += root + "/" + dir + "\n";
#ifndef HL_PACK_ID
                // the base engine: a mod with the base's boot program changed only in its data runs with that program (last mod wins)
                if (access((root + "/" + dir + "/SLES_556.05").c_str(), R_OK) == 0)
                {
                    const std::string candidate = root + "/" + dir + "/SLES_556.05";
                    if (sameCodeAsBase(data + "/SLES_556.05", candidate))
                    {
                        modElf = candidate;
                        std::fprintf(stderr, "un5: mod program %s (the base's code, its own data)\n", candidate.c_str());
                    }
                    else
                        std::fprintf(stderr, "un5: mod program %s has other code: it needs its code pack; the base program runs\n", candidate.c_str());
                }
#endif
#ifdef HL_PACK_ID
                const std::string id = dir.size() > 12 && dir.compare(0, 7, "modlib/") == 0 ? dir.substr(7, dir.find('/', 7) - 7) : "";
                if (id == HL_PACK_ID)
                {
                    if (FILE *p = std::fopen((root + "/modlib/" + id + "/mod.properties").c_str(), "r"))
                    {
                        char prop[256];
                        while (std::fgets(prop, sizeof(prop), p))
                            if (std::strncmp(prop, "boot=", 5) == 0)
                            {
                                std::string boot = prop + 5;
                                while (!boot.empty() && (boot.back() == '\n' || boot.back() == '\r'))
                                    boot.pop_back();
                                if (access((root + "/" + dir + "/" + boot).c_str(), R_OK) == 0)
                                    modElf = root + "/" + dir + "/" + boot;
                                else if (access((data + "/" + boot).c_str(), R_OK) == 0)
                                    modElf = data + "/" + boot;
                            }
                        std::fclose(p);
                    }
                    if (modElf.empty() && access((data + "/SLES_556.05").c_str(), R_OK) == 0)
                        modElf = data + "/SLES_556.05";
                }
#endif
            }
            std::fclose(f);
        }
        if (!dirs.empty())
            setenv("HL_MODS", dirs.c_str(), 1);
#ifdef HL_PACK_ID
        if (modElf.empty())
        {
            fail("This is the code pack of a mod (" HL_PACK_ID "), and the mod is not switched on.");
            return 1;
        }
        std::fprintf(stderr, "un5: mod %s, program %s\n", HL_PACK_ID, modElf.c_str());
        // the IOP in batches of 1024 EE cycles, as before 0091: with 4096 the A35's IOP spun at 83 ms a frame from its
        // first logos and the game ran at 12 fps on the Mi A3 (2026-11-11); the base and the Alpha 1.1X keep 4096
        setenv("HL_IOP_BATCH", "1024", 0);
#endif
    }
    static std::string saves = root + "/saves";
    setenv("HL_MC_ROOT", saves.c_str(), 0);
    // the IOP and the SPU2 on a thread of their own (patch 0100; HL_IOP_THREAD=0 in hl_env.txt for the old way). On the Mi A3
    // the fights went from 39-41 to 43-44 fps after the lives drop (2026-11-12). The A35's code pack too since 2026-11-12: on
    // the RG Cube 55.0 -> 58.7 fps (NTSC), the same sound; on the Mi A3 the same as without (41.3 / 40.4).
    setenv("HL_IOP_THREAD", "1", 0);

    if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_GAMEPAD))
    {
        fail(SDL_GetError());
        return 1;
    }
    hl_spu2_install();
    SDL_Window *window = SDL_CreateWindow("Hidden Leaf", 0, 0, SDL_WINDOW_FULLSCREEN | SDL_WINDOW_HIGH_PIXEL_DENSITY);
    int width = 0, height = 0;
    void *native = window ? windowHandle(window, &width, &height) : nullptr;
    for (int i = 0; !native && window && i < 200; ++i)
    {   // (the surface comes a little after the window)
        SDL_PumpEvents();
        SDL_Delay(10);
        native = windowHandle(window, &width, &height);
    }
    if (!native)
    {
        fail("No window for the game.");
        return 1;
    }

    const std::string gsxLib = libDir + "/libbt4gsx.so";
    void *gsx = dlopen(gsxLib.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!gsx)
    {
        std::fprintf(stderr, "un5: %s\n", dlerror());
        fail("The renderer (libbt4gsx.so) cannot be opened.");
        return 1;
    }
    const auto gsxRun = reinterpret_cast<int (*)(const GsxArgs *)>(dlsym(gsx, "Bt4Gsx_Run"));
    const auto gsxSurfaceChanged = reinterpret_cast<void (*)(void *, int, int)>(dlsym(gsx, "Bt4Gsx_SurfaceChanged"));
    const auto gsxSurfaceDestroyed = reinterpret_cast<void (*)(void)>(dlsym(gsx, "Bt4Gsx_SurfaceDestroyed"));
    const auto gsxStop = reinterpret_cast<void (*)(void)>(dlsym(gsx, "Bt4Gsx_Stop"));
    g_gsxSetVideo = reinterpret_cast<void (*)(const GsxVideo *)>(dlsym(gsx, "Bt4Gsx_SetVideo"));
    g_gsxPresentRate = reinterpret_cast<int (*)()>(dlsym(gsx, "Bt4Gsx_PresentRate"));
    hl_game::g_nativeOn = reinterpret_cast<bool (*)()>(dlsym(gsx, "Bt4Gsx_NativeOn"));
    hl_game::g_liveFight = reinterpret_cast<void (*)(bool)>(dlsym(gsx, "Bt4Gsx_LiveFight"));
    hl_game::g_wideCameraOut = reinterpret_cast<void (*)(bool)>(dlsym(gsx, "Bt4Gsx_WideCamera"));
    hl_game::g_gsDump = reinterpret_cast<void (*)(const char *, int)>(dlsym(gsx, "Bt4Gsx_GsDump"));
    g_windowW = width;
    g_windowH = height;
    applyVideo();
    if (!gsxRun || !gsxSurfaceChanged || !gsxSurfaceDestroyed || !gsxStop)
    {
        fail("The renderer (libbt4gsx.so) is not the expected one.");
        return 1;
    }

    // the game, with its ELF from the boot files the launcher copied
    static std::string elf = modElf.empty() ? data + "/SLES_556.05" : modElf;
    std::thread([] {
        static char arg0[] = "ps2EntryRunner";
        static char *args[] = {arg0, elf.data(), nullptr};
        const int code = ps2x_runtime_main(2, args);
        std::fprintf(stderr, "un5: the game ended (%d)\n", code);
        g_quit = true;
    }).detach();

    // the ring, once the game has sent its first GS packet
    ps2live::RingHeader *ring = nullptr;
    while (!g_quit && !(ring = static_cast<ps2live::RingHeader *>(hl_gslive::ring())))
    {
        SDL_PumpEvents();
        updatePad();
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    if (!ring)
        return 0;

    const char *scale = std::getenv("HL_GS_SCALE");
    if (scale)
        g_settings.scale4 = static_cast<int>(std::atof(scale) * 4.0 + 0.5);
    if (const char *wide = std::getenv("HL_ASPECT"))
        g_settings.aspectMilli = std::atoi(wide);
    if (const char *f = std::getenv("HL_FPS_MODE"))
        g_settings.fpsMode = std::atoi(f);
    if (const char *f = std::getenv("HL_FILTER")) // (0 bilinear, 1 sharp bilinear, 2 FXAA, 3 CAS: the measurement matrix)
        g_settings.filter = std::atoi(f);
    const float upscale = static_cast<float>(g_settings.scale4) / 4.0f;
    static const std::string gsxRoot = root + "/gsx", ini = gsxRoot + "/un5.ini";
    writeIni(ini, upscale);
    const char *api = std::getenv("HL_GS_API");
    static GsxArgs a;
    a = {ring, native, width, height, gsxRoot.c_str(), ini.c_str(), api ? api : "vulkan", upscale,
         driverDir.c_str(), driverLib.c_str(), libDir.c_str()};
    std::fprintf(stderr, "un5: PCSX2's GS, %s, %gx, window %dx%d\n", a.renderer, static_cast<double>(upscale), width, height);
    std::atomic<bool> ended{false};
    std::thread gs([&] {
        // (ARMSX2's VMManager names this thread "CPU Thread" itself; Nominum's own backend runs the stream here without it,
        // and HL_PERF counts the GS by that name)
        pthread_setname_np(pthread_self(), "CPU Thread");
        const int code = gsxRun(&a);
        std::fprintf(stderr, "un5: the GS ended (%d)\n", code);
        // A custom GPU driver the GS could not open a device with (one made for another Adreno, a Vulkan too old): the
        // launcher reads this, goes back to the system's driver and says so (otherwise the game just closed, 2026-11-12)
        if (code != 0 && !driverLib.empty())
            if (FILE *f = std::fopen((root + "/gs_failed.txt").c_str(), "w"))
            {
                std::fprintf(f, "%s\n", driverDir.c_str());
                std::fclose(f);
            }
        ended = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(300)); // (after PCSX2's handler went in, in its own start)
    installFaultHandler();
    while (!g_quit && !ended)
    {
        SDL_Event ev;
        while (SDL_PollEvent(&ev))
        {
            if (ev.type == SDL_EVENT_QUIT)
            {
                gsxStop();
                std::fflush(nullptr);
                _exit(0);
            }
            if (ev.type == SDL_EVENT_WILL_ENTER_BACKGROUND)
                gsxSurfaceDestroyed(); // (the surface goes: no presenting into it)
            else if (ev.type == SDL_EVENT_DID_ENTER_FOREGROUND || ev.type == SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED)
            {
                int nw = 0, nh = 0;
                if (void *n = windowHandle(window, &nw, &nh))
                {
                    gsxSurfaceChanged(n, nw, nh);
                    g_windowW = nw;
                    g_windowH = nh;
                    applyVideo();
                }
            }
        }
        updatePad();
        std::this_thread::sleep_for(std::chrono::milliseconds(4));
    }
    gsxStop();
    if (gs.joinable())
        gs.join();
    std::fprintf(stderr, "un5: leaving\n");
    std::fflush(nullptr);
    _exit(0);
}
