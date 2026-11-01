// compat: what ARMSX2's SPU2 sources need of the rest of PCSX2. The IOP here is ps2xIOP (see ../hl_spu2.cpp).
#pragma once
#include "common/Pcsx2Types.h"
#include "common/Pcsx2Defs.h"
#include "common/Assertions.h"
static constexpr u32 PSXCLK = 36864000u;
struct HlPsxRegs { u32 cycle = 0; };
extern HlPsxRegs psxRegs;
