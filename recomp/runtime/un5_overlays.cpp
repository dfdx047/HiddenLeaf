// Hidden Leaf: Ultimate Ninja 5's overlays in the recompiled runtime.
//
// The game loads its code for a mode from PRG/ (BTL.BIN fights, ADV.BIN the adventure, ETC.BIN the rest, TEXT*.BIN
// the texts) over the same addresses. Each overlay was recompiled on its own, with its own table
// (recomp/overlays.py). When the runtime has no function for a PC, it asks here: the overlay whose code is in guest
// memory now (checked by a few windows of its bytes) gives the function. The overlay is chosen by its content, never
// by its id or address alone: mods reuse ids (Ultimate Ninja 6's MOD.BIN is id 8, the European TEXTSPA.BIN's).
#include "ps2_runtime.h"
#include "runtime/ps2_memory.h"

#include <cstdint>
#include <cstring>
#include <iostream>

// (at namespace scope: the overlays' tables are defined in their own files, un5_<name>_table.cpp)
struct SigWindow
{
    uint32_t offset;
    uint8_t bytes[32];
};

struct Un5Overlay
{
    const char *name;
    uint32_t id;
    uint32_t addr;
    uint32_t codeSize;
    PS2Runtime::RecompiledFunction *table;
    const uint32_t *tableBase;
    const uint32_t *tableSlots;
    SigWindow sig[3];
    int sigCount;
};

#include "un5_overlay_list.inc"

namespace
{
    constexpr size_t kOverlayCount = sizeof(kOverlays) / sizeof(kOverlays[0]);
    constexpr int kResidentSlots = 16;
    const Un5Overlay *g_resident[kResidentSlots] = {}; // the last one found per load address
    uint32_t g_residentAddr[kResidentSlots] = {};

    bool resident(const uint8_t *rdram, const Un5Overlay &o)
    {
        if (o.sigCount <= 0)
            return false;
        for (int i = 0; i < o.sigCount; ++i)
        {
            const uint32_t at = (o.addr + o.sig[i].offset) & 0x1FFFFFFu;
            if (std::memcmp(rdram + at, o.sig[i].bytes, sizeof(o.sig[i].bytes)) != 0)
            {
                return false;
            }
        }
        return true;
    }

    PS2Runtime::RecompiledFunction lookupIn(const Un5Overlay &o, uint32_t address)
    {
        if (address < *o.tableBase || (address & 3u) != 0u)
        {
            return nullptr;
        }
        const uint32_t slot = (address - *o.tableBase) >> 2;
        return slot < *o.tableSlots ? o.table[slot] : nullptr;
    }

    int cacheIndex(uint32_t addr)
    {
        for (int i = 0; i < kResidentSlots; ++i)
        {
            if (g_residentAddr[i] == addr || g_residentAddr[i] == 0u)
            {
                g_residentAddr[i] = addr;
                return i;
            }
        }
        return 0;
    }

    PS2Runtime::RecompiledFunction resolve(PS2Runtime *runtime, uint32_t address)
    {
        address &= 0x1FFFFFFFu;
        const uint8_t *rdram = runtime->memory().getRDRAM();
        for (size_t i = 0; i < kOverlayCount; ++i)
        {
            const Un5Overlay &o = kOverlays[i];
            if (address < o.addr || address >= o.addr + o.codeSize)
            {
                continue;
            }
            const int c = cacheIndex(o.addr);
            // the overlay found last at this address, if it is still the one in memory
            if (g_resident[c] != nullptr && address >= g_resident[c]->addr && address < g_resident[c]->addr + g_resident[c]->codeSize &&
                resident(rdram, *g_resident[c]))
            {
                return lookupIn(*g_resident[c], address);
            }
            if (resident(rdram, o))
            {
                if (g_resident[c] != &o)
                {
                    std::cerr << "[un5] overlay " << o.name << " (id " << o.id << ") resident at 0x" << std::hex << o.addr << std::dec << std::endl;
                }
                g_resident[c] = &o;
                return lookupIn(o, address);
            }
        }
        return nullptr;
    }

    struct Install
    {
        Install() { g_ps2OverlayResolver = &resolve; }
    } install;
}
