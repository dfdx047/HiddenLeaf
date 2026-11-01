#pragma once
#include <atomic>

// The pad of player 1, kept by the host: libpad's bit order, active high; lx | ly << 8 | rx << 16 | ry << 24, 0x80 centred.
extern std::atomic<unsigned> g_hlPadButtons;
extern std::atomic<unsigned> g_hlPadAxes;
