// compat: the DMA registers of channels 4 and 7 and the IOP's memory
#pragma once
#include "common/Pcsx2Types.h"
extern u32 HW_DMA4_MADR, HW_DMA4_BCR, HW_DMA4_CHCR, HW_DMA4_TADR;
extern u32 HW_DMA7_MADR, HW_DMA7_BCR, HW_DMA7_CHCR, HW_DMA7_TADR;
u8 *iopPhysMem(u32 address);
