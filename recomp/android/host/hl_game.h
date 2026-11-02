// Hidden Leaf: what the Android host knows and changes of Ultimate Ninja 5 itself (recomp/android/host/hl_game.cpp).
#pragma once
#include <atomic>
#include <cstdint>

namespace hl_game
{
    extern std::atomic<int> g_paused;          // the game frozen: EE, IOP and sound stay where they are
    extern std::atomic<float> g_cameraXScale;  // the 3D's horizontal scale for a wider screen, 1 = the game's 4:3

    extern bool (*g_nativeOn)();               // libbt4gsx: its native renderer is drawing (a fight or the campaign)
    int divider();                             // vsyncs a game frame takes (the game's own: 2)

    int scene();                               // what the game shows: 0 unknown, 1 menu, 2 fight, 3 campaign
    int overlay();                             // the mode's code in memory: 0 none yet, 1 BTL (fight), 2 ADV (campaign), 3 other
}
