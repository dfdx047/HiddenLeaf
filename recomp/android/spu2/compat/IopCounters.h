// compat: the SPU2 asks to be called at DMAICounter through counter 6; here it is called on every batch of IOP cycles
#pragma once
#include "common/Pcsx2Types.h"
struct HlPsxCounter { u32 startCycle = 0, deltaCycles = 0; };
extern HlPsxCounter psxCounters[8];
extern u32 psxNextDeltaCounter, psxNextStartCounter;
