// Hidden Leaf: the online match in the engine (hl_online.cpp). The app's Online screen writes <root>/online/session.txt and starts
// the game; this reads it, makes the line to the other phone and keeps the two games in step, once per vsync.
#pragma once
#include <string>

namespace hl_online
{
    // Called by SDL_main before the game starts (un5_main.cpp): true when this start is an online match. It then sets what must be
    // the same on both phones before the game runs (a fresh memory card, a fixed clock in UTC, the launcher's assists off, every
    // character open) and puts in the vsync hook and the pad hook.
    bool setup(const std::string &root);
    bool active();
}
