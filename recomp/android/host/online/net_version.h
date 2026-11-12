#pragma once
#include <cstdint>

// The network version of Hidden Leaf's online match. The same number is in three places, and the three must change together:
//   - here (the engine: what goes over the line between the two phones, link.cpp and session.cpp);
//   - the app's Online.NET_VERSION (android/.../data/Online.kt: what it tells the rooms service);
//   - the rooms service's ALLOWED_VERSIONS (online/worker/wrangler.example.jsonc).
// It goes up when anything that decides a match changes: the packets, the lockstep, the pad hook, or the game's code in a way
// that two builds would no longer stay the same. Two games of different versions are never put in the same room.
constexpr std::uint32_t HL_NET_VERSION = 1;

// The game, as the rooms service knows it (Ultimate Ninja 5, European disc SLES-55605).
constexpr const char* HL_NET_GAME = "un5";
