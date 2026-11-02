// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself. All addresses are of the European ELF (SLES-55605).
#include "hl_game.h"

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
    bool (*g_nativeOn)() = nullptr;
}

namespace
{
    constexpr uint32_t kOverlayBase = 0x6C6D00u;   // where BTL.BIN, ADV.BIN and ETC.BIN are loaded
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

    void cameraMatrices(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const float scale = hl_game::g_cameraXScale.load(std::memory_order_relaxed);
        if (scale != 1.0f)
        {
            const uint32_t camera = GPR_U32(ctx, 4);
            std::memcpy(rdram + ((camera + kCameraXScale) & 0x1FFFFFFu), &scale, 4);
        }
        g_cameraOriginal(rdram, ctx, runtime);
    }

    void onVsync(PS2Runtime *runtime, uint64_t tick)
    {
        if (g_rdram == nullptr)
        {
            g_rdram = runtime->memory().getRDRAM();
            g_cameraOriginal = runtime->lookupFunction(kCameraMatrices);
            if (g_cameraOriginal != nullptr)
                runtime->registerFunction(kCameraMatrices, cameraMatrices);
            if (const char *d = std::getenv("HL_GFX_DIV"))
                g_divider = std::atoi(d);
            if (const char *w = std::getenv("HL_CAM_X"))
                hl_game::g_cameraXScale = static_cast<float>(std::atof(w));
        }
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
// two by the mode's code in memory. Anything else (title, menus, films, loading screens) is a menu.
int hl_game::scene()
{
    if (g_rdram == nullptr)
        return 0;
    if (g_nativeOn == nullptr || !g_nativeOn())
        return 1;
    const int mode = overlay();
    return mode == 1 ? 2 : mode == 2 ? 3 : 1;
}
