// compat: the one setting the mixer reads
#pragma once
#include "R3000A.h"
struct HlEmuConfig { struct { bool LightweightMode = false; } SPU2; };
extern HlEmuConfig EmuConfig;
