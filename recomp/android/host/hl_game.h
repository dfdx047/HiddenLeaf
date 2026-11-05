// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself (recomp/android/host/hl_game.cpp).
#pragma once
#include <atomic>
#include <cstdint>
#include <string>

namespace hl_game
{
    extern std::atomic<int> g_paused;          // the game frozen: EE, IOP and sound stay where they are
    extern std::atomic<float> g_cameraXScale;  // the 3D's horizontal scale for a wider screen, 1 = the game's 4:3
    extern std::atomic<uint32_t> g_vsync;      // the vsync now (for other threads' logs: the SPU2's HL_SND_ONSET)

    extern bool (*g_nativeOn)();               // libbt4gsx: its native renderer is drawing (a fight or the campaign)
    int divider();                             // vsyncs a game frame takes (the game's own: 2)

    int scene();                               // what the game shows: 0 unknown, 1 menu, 2 fight, 3 campaign
    extern std::atomic<bool> g_unlockAll;      // the launcher's "unlock everything"
    extern std::atomic<bool> g_specialOn;      // the launcher's "special with one button"
    extern std::atomic<uint32_t> g_specialMask; // its button, in the game's bits of the pad
    extern std::atomic<int> g_mashPercent;     // what a tap is worth in the steps of pressing repeatedly: 100, 125, 150, 165, 200
    int overlay();                             // the mode's code in memory: 0 none yet, 1 BTL (fight), 2 ADV (campaign), 3 other

    // HL_GS_DUMP: libbt4gsx's Bt4Gsx_GsDump (a PCSX2 GS dump of the next vsyncs into <base>.gs.zst) and the folder
    extern void (*g_gsDump)(const char *base, int frames);
    extern std::string g_gsDumpDir;
    // HL_VIF_DUMP: the same scenes, the VIF1 stream and the GIF out of the VIF1/VU1 interpreters (runtime/hl_vif_dump.h), here
    extern std::string g_vifDumpDir;
}
