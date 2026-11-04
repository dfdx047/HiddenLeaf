// Hidden Leaf (Android): NOMINUM_VU1=1, the VIF1 and VU1 of Nominum Render (core/vif1, its VU1 with the recompiled blocks of
// nominum-vurec) instead of the runtime's processVIF1Data, through runtime/hl_nominum_vu1.h.
//
// Nominum's docs/integracao.md (Phase 3, step 2) has the game send each VIF1 transfer to nominum_submit_vif1, inside
// libbt4gsx.so, through a new packet type in the live ring. That type is not defined yet in either tree (the ring header must
// stay identical in both, and this session does not change Nominum). So this takes the same Nominum code, the same VIF1,
// the same VU1 and the same catalog, and runs it here, in the runtime's VuWorker, where the runtime's own VIF1 ran. Its GIF
// packets go to the ring as before (PATH1/PATH2), and libbt4gsx.so does not change. The runtime keeps its copies of what the
// game reads (vif1_regs, the VU1 memories, a DIRECT's debt, the PATH3 mask): they go in before each transfer, back after it.
//
// Nominum's code is built from build/nominum-<commit> (git archive of the Nominum repository; recomp/android/CMakeLists.txt,
// HL_NOMINUM_DIR), and its catalog from build/nominum-<commit>/build/vu1rec (nominum-vurec over ~/Downloads/vifdumps: game
// code, never in git).
#include "runtime/hl_nominum_vu1.h"

#include "core/vif1.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <memory>

namespace
{
    std::unique_ptr<nominum::core::Vif1> s_vif;
    hl_nvu1::GifFn s_gif = nullptr;
    hl_nvu1::MaskFn s_mask = nullptr;
    void *s_ctx = nullptr;
    uint64_t s_transfers = 0, s_codeLoads = 0;

    bool transfer(const hl_nvu1::Shared &shared, const uint8_t *data, uint32_t size, hl_nvu1::GifFn gif, hl_nvu1::MaskFn mask, void *ctx)
    {
        nominum::core::Vif1 &v = *s_vif;
        s_gif = gif;
        s_mask = mask;
        s_ctx = ctx;
        constexpr uint32_t kCode = nominum::vu1::PS2_VU1_CODE_SIZE, kData = nominum::vu1::PS2_VU1_DATA_SIZE;
        // what the game may have written by address since the last transfer
        if (std::memcmp(v.code(), shared.code, kCode) != 0)
        {
            std::memcpy(v.code(), shared.code, kCode);
            v.codeChanged();
            ++s_codeLoads;
        }
        std::memcpy(v.data(), shared.data, kData);
        std::memcpy(&v.regs(), shared.regs, sizeof(nominum::core::Vif1Regs));
        nominum::core::Vif1Pending &p = v.pending();
        p.directQwc = *shared.pendingDirectQwc;
        p.imageQwc = *shared.pendingImageQwc;
        p.imageDirectHl = *shared.pendingImageDirectHl;
        p.path3Masked = *shared.path3Masked;

        const uint64_t generation = v.getVU1CodeGeneration();
        v.transfer(data, size);
        const bool codeChanged = v.getVU1CodeGeneration() != generation;

        if (codeChanged)
            std::memcpy(shared.code, v.code(), kCode);
        std::memcpy(shared.data, v.data(), kData);
        std::memcpy(shared.regs, &v.regs(), sizeof(nominum::core::Vif1Regs));
        *shared.pendingDirectQwc = p.directQwc;
        *shared.pendingImageQwc = p.imageQwc;
        *shared.pendingImageDirectHl = p.imageDirectHl;
        *shared.path3Masked = p.path3Masked;
        if ((++s_transfers % 3000u) == 0u)
            std::fprintf(stderr, "[nvu1] %llu transfers, %llu MSCAL, %llu MSCNT, %llu kicks, VU1 %.1f ms in all; micro memory loaded by address %llu times\n",
                         static_cast<unsigned long long>(s_transfers), static_cast<unsigned long long>(v.counts.mscal),
                         static_cast<unsigned long long>(v.counts.mscnt), static_cast<unsigned long long>(v.counts.kicks),
                         static_cast<double>(v.counts.vuNanoseconds) / 1e6, static_cast<unsigned long long>(s_codeLoads));
        return codeChanged;
    }
}

namespace hl_nominum_vu1
{
    // From un5_main.cpp, before the game starts: NOMINUM_VU1=1 turns it on
    void setup()
    {
        const char *on = std::getenv("NOMINUM_VU1");
        if (on == nullptr || std::strcmp(on, "1") != 0)
            return;
        s_vif = std::make_unique<nominum::core::Vif1>();
        s_vif->setSink([](int path, const uint8_t *d, uint32_t s, bool directHl) { s_gif(s_ctx, path, d, s, directHl); });
        s_vif->setPath3MaskCallback([](bool masked) { s_mask(s_ctx, masked); });
        hl_nvu1::g_transfer = transfer;
        const char *rec = std::getenv("NOMINUM_VU1_REC");
        std::fprintf(stderr, "[nvu1] Nominum's VIF1 and VU1 (core/vif1, %s) in place of the runtime's\n",
                     rec != nullptr && std::strcmp(rec, "0") == 0 ? "recompiled blocks off" : "recompiled blocks where the catalog has them");
    }
}
