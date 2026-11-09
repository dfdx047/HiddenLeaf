// Hidden Leaf on the PC with the Android's drawing: the headless runtime (PS2X_HEADLESS_HOST, its GS stream into a ring,
// hl_gs_live.cpp) and libbt4gsx.so built for Linux (recomp/pc/hlgsx_pc.cmake: ARMSX2's GS on Vulkan plus the native fight
// renderer, hl_native.cpp), as recomp/android/host/un5_main.cpp does on the devices, without its window, touch and JNI.
// For tests: the same picture as the devices, compared with PCSX2 in 4:3, 16:9 and 20:9.
//
//   un5pc <elf>   (run in build/disc, as the PC runner)
//   HL_GSX_LIB    libbt4gsx.so (default: build/gsx-pc/bin/libbt4gsx.so next to build/disc)
//   HL_GSX_ROOT   the GS's folder: resources/ (shaders) and its settings (default: the library's folder)
//   HL_SCREEN     the screen the picture is for, WxH (default 1280x960: 4:3); 1920x1080 is 16:9, 2400x1080 is 20:9
//   HL_ASPECT     the launcher's shape (aspect_milli): 0 the screen's, 1333 the game's
//   HL_GS_SCALE   internal resolution (1, 2, ...); HL_FPS_MODE 50/60/120; HL_FILTER 0-3; HL_GS_API vulkan or gl
//   HL_GSX_ARGS   more of the GS runner's arguments: "-surfaceless -dumpdir <dir>" saves every frame as PNG; without
//                 -surfaceless the runner opens an X11 window (add -window)
//   HL_UNLOCK_ALL, HL_SPECIAL_ONE, HL_SPECIAL_BUTTON, HL_MASH_PERCENT: the launcher's game options
#include "hl_game.h"
#include "hl_host_pad.h"
#include "ps2_live_ring.h"
#include "runtime/gs/hl_gs_live.h"

#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <pthread.h>
#include <string>
#include <sys/stat.h>
#include <thread>

int ps2x_runtime_main(int argc, char *argv[]); // the runtime's main() (src/main.cpp, renamed by the build)

namespace
{
    // as un5_main.cpp
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
    struct GsxVideo
    {
        float upscale;
        int filter;
        float aspect;
        float hudScale;
        int fpsMode;
    };

    int envInt(const char *name, int fallback)
    {
        const char *v = std::getenv(name);
        return v && *v ? std::atoi(v) : fallback;
    }

    // un5_main.cpp's writeIni, the same settings
    void writeIni(const std::string &path, float upscale, int filter)
    {
        FILE *f = std::fopen(path.c_str(), "w");
        if (!f)
            return;
        std::fprintf(f,
                     "; Ultimate Ninja 5 on PCSX2's GS (PC test host, recomp/pc/hl_pc_main.cpp)\n"
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
                     "ForceMaliFramebufferFetch = true\n"
                     "DumpReplaceableTextures = false\n",
                     static_cast<double>(upscale), filter == 2 ? "true" : "false", filter == 3 ? 1 : 0, filter == 1 ? 2 : 1,
                     std::getenv("HL_PERF") ? "true" : "false");
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
}

int main(int argc, char *argv[])
{
    if (argc < 2)
    {
        std::fprintf(stderr, "usage: %s <elf>\n", argv[0]);
        return 2;
    }
    const std::string lib = std::getenv("HL_GSX_LIB") ? std::getenv("HL_GSX_LIB") : "../gsx-pc/bin/libbt4gsx.so";
    const std::string root = std::getenv("HL_GSX_ROOT") ? std::getenv("HL_GSX_ROOT") : lib.substr(0, lib.rfind('/'));
    int screenW = 1280, screenH = 960;
    if (const char *s = std::getenv("HL_SCREEN"))
        std::sscanf(s, "%dx%d", &screenW, &screenH);
    const int scale4 = static_cast<int>(std::atof(std::getenv("HL_GS_SCALE") ? std::getenv("HL_GS_SCALE") : "1") * 4.0 + 0.5);
    const int aspectMilli = envInt("HL_ASPECT", 0), filter = envInt("HL_FILTER", 0), fpsMode = envInt("HL_FPS_MODE", 50);
    hl_game::g_unlockAll = envInt("HL_UNLOCK_ALL", 0) != 0;
    hl_game::g_specialOn = envInt("HL_SPECIAL_ONE", 0) != 0;
    hl_game::g_specialMask = static_cast<uint32_t>(envInt("HL_SPECIAL_BUTTON", 0));
    hl_game::g_mashPercent = envInt("HL_MASH_PERCENT", 100);

    void *gsx = dlopen(lib.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!gsx)
    {
        std::fprintf(stderr, "un5pc: %s\n", dlerror());
        return 1;
    }
    const auto gsxRun = reinterpret_cast<int (*)(const GsxArgs *)>(dlsym(gsx, "Bt4Gsx_Run"));
    const auto gsxSetVideo = reinterpret_cast<void (*)(const GsxVideo *)>(dlsym(gsx, "Bt4Gsx_SetVideo"));
    hl_game::g_nativeOn = reinterpret_cast<bool (*)()>(dlsym(gsx, "Bt4Gsx_NativeOn"));
    hl_game::g_liveFight = reinterpret_cast<void (*)(bool)>(dlsym(gsx, "Bt4Gsx_LiveFight"));
    hl_game::g_wideCameraOut = reinterpret_cast<void (*)(bool)>(dlsym(gsx, "Bt4Gsx_WideCamera"));
    hl_game::g_gsDump = reinterpret_cast<void (*)(const char *, int)>(dlsym(gsx, "Bt4Gsx_GsDump"));
    if (!gsxRun || !gsxSetVideo)
    {
        std::fprintf(stderr, "un5pc: %s is not the expected library\n", lib.c_str());
        return 1;
    }

    // un5_main.cpp's applyVideo: the 3D's horizontal scale for the screen's shape
    {
        const float screen = static_cast<float>(screenW) / static_cast<float>(screenH);
        float shape = aspectMilli > 0 ? static_cast<float>(aspectMilli) / 1000.0f : screen;
        if (shape > screen)
            shape = screen;
        const float game = 4.0f / 3.0f;
        const float x = shape > game + 0.01f ? game / shape : 1.0f;
        hl_game::g_cameraXScale.store(x, std::memory_order_relaxed);
        const GsxVideo v{static_cast<float>(scale4) / 4.0f, filter, x < 1.0f ? shape : game, x, fpsMode};
        gsxSetVideo(&v);
        std::fprintf(stderr, "un5pc: screen %dx%d, picture %.3f, camera x %.3f\n", screenW, screenH, static_cast<double>(x < 1.0f ? shape : game),
                     static_cast<double>(x));
    }

    static std::string elf = argv[1];
    static std::atomic<bool> quit{false};
    std::thread([] {
        static char arg0[] = "ps2EntryRunner";
        static char *args[] = {arg0, elf.data(), nullptr};
        const int code = ps2x_runtime_main(2, args);
        std::fprintf(stderr, "un5pc: the game ended (%d)\n", code);
        quit = true;
    }).detach();

    ps2live::RingHeader *ring = nullptr;
    while (!quit && !(ring = static_cast<ps2live::RingHeader *>(hl_gslive::ring())))
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    if (!ring)
        return 0;

    const float upscale = static_cast<float>(scale4) / 4.0f;
    static const std::string ini = root + "/un5.ini";
    writeIni(ini, upscale, filter);
    const char *api = std::getenv("HL_GS_API");
    static GsxArgs a;
    a = {ring, nullptr, screenW, screenH, root.c_str(), ini.c_str(), api ? api : "vulkan", upscale, "", "", ""};
    std::fprintf(stderr, "un5pc: PCSX2's GS, %s, %gx\n", a.renderer, static_cast<double>(upscale));
    // (the GS runs on this thread; ARMSX2's VMManager and HL_PERF know it by this name, as on Android)
    pthread_setname_np(pthread_self(), "CPU Thread");
    const int code = gsxRun(&a);
    std::fprintf(stderr, "un5pc: the GS ended (%d)\n", code);
    std::fflush(nullptr);
    _exit(0);
}
