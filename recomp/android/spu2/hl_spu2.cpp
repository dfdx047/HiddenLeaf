// The SPU2 of ARMSX2 (PCSX2's, with its NEON reverb) under ps2xIOP. Its sources compile unchanged against compat/; this file
// is the part of pcsx2/SPU2/spu2.cpp that is not about PCSX2's output stream and settings, and the ties to the IOP: its
// registers, DMA channels 4 and 7, its interrupts, its clock. Everything runs on the thread of the IOP.
// HL_SPU2=0 leaves the IOP's old stand-in (no sound).
#include "SPU2/defs.h"
#include "SPU2/spu2.h"
#include "SPU2/Dma.h"
#include "SPU2/regs.h"
#include "SPU2/spu2_neon.h"
#include "IopDma.h"
#include "IopHw.h"
#include "IopCounters.h"
#include "Config.h"
#include "common/ARCADE.h"
#include "common/Console.h"

#include "emulator/core/iop_memory.h"

#include <SDL3/SDL.h>

#include <algorithm>
#include <array>
#include <cstdio>
#include <cstdlib>
#include <cstring>

HlPsxRegs psxRegs;
HlPsxCounter psxCounters[8];
u32 psxNextDeltaCounter = 0, psxNextStartCounter = 0;
HlEmuConfig EmuConfig;
u32 HW_DMA4_MADR = 0, HW_DMA4_BCR = 0, HW_DMA4_CHCR = 0, HW_DMA4_TADR = 0;
u32 HW_DMA7_MADR = 0, HW_DMA7_BCR = 0, HW_DMA7_CHCR = 0, HW_DMA7_TADR = 0;

const StereoOut32 StereoOut32::Empty(0, 0);
u64 lClocks = 0;
namespace Arcade
{
    std::atomic<bool> s_session{false};
}
void Log::Writev(LOGLEVEL, ConsoleColors, const char *, va_list) {}

namespace
{
    uint8_t *s_ram = nullptr;
    const uint64_t *s_cycles = nullptr;
    uint32_t s_irqs = 0;

    // ---- the output: 48 kHz stereo to SDL. The samples come in bursts (the IOP advances with the game's work inside each
    // vsync), so the stream holds some back before it starts, and again after running dry.
    SDL_AudioStream *s_stream = nullptr;
    constexpr uint32_t kChunkFrames = 256u;
    constexpr int kBytesPerFrame = 2 * static_cast<int>(sizeof(float));
    constexpr int kStartBytes = 48000 * kBytesPerFrame * 70 / 1000;
    constexpr int kMaxBytes = 48000 * kBytesPerFrame * 200 / 1000;
    std::array<float, kChunkFrames * 2u> s_chunk;
    uint32_t s_chunkPos = 0;
    bool s_playing = false;
    float s_dcIn[2] = {}, s_dcOut[2] = {};

    void writeChunk()
    {
        // HL_SPU2_DUMP=<file>: the sound as raw 48 kHz stereo floats, the last 120 s (to look at it without a speaker)
        static FILE *const s_dump = []() -> FILE * {
            const char *path = std::getenv("HL_SPU2_DUMP");
            return path ? std::fopen(path, "wb") : nullptr;
        }();
        static uint64_t s_dumped = 0;
        if (s_dump)
        {
            if (s_dumped >= 48000ull * 120ull)
            {
                std::rewind(s_dump);
                s_dumped = 0;
            }
            std::fwrite(s_chunk.data(), sizeof(float), s_chunk.size(), s_dump);
            s_dumped += kChunkFrames;
        }
        if (!s_stream)
            return;
        const int queued = SDL_GetAudioStreamQueued(s_stream);
        if (queued > kMaxBytes)
            return; // (the game ran ahead of real time: HL_FAST_UNTIL)
        if (s_playing && queued == 0)
        {
            SDL_PauseAudioStreamDevice(s_stream);
            s_playing = false;
        }
        SDL_PutAudioStreamData(s_stream, s_chunk.data(), static_cast<int>(s_chunk.size() * sizeof(float)));
        if (!s_playing && SDL_GetAudioStreamQueued(s_stream) >= kStartBytes)
        {
            SDL_ResumeAudioStreamDevice(s_stream);
            s_playing = true;
        }
    }

    void reset()
    {
        spu2Mix = MULTI_ISA_SELECT(spu2Mix);
        SPU2::RegisterNEONBackend();
        std::memset(spu2regs, 0, 0x010000);
        std::memset(_spu2mem, 0, 0x200000);
        std::memset(_spu2mem + 0x2800, 7, 0x10); // (from pcsx2: what the BIOS leaves there, and voices loop on it)
        std::memset(_spu2mem + 0xe870, 7, 0x10);
        Spdif.Info = 0;
        Cores[0].Init(0);
        Cores[1].Init(1);
    }

    // ---- what ps2xIOP calls
    void attach(const uint64_t *cycles, uint8_t *ram)
    {
        s_cycles = cycles;
        s_ram = ram;
        psxRegs.cycle = static_cast<u32>(*cycles);
        lClocks = psxRegs.cycle;
        reset();
    }

    void advance()
    {
        psxRegs.cycle = static_cast<u32>(*s_cycles);
        TimeUpdate(psxRegs.cycle);
    }

    // HL_SPU2_LOG=N: the first N register accesses and DMAs
    bool logging()
    {
        static const long s_limit = [] {
            const char *v = std::getenv("HL_SPU2_LOG");
            return v ? std::atol(v) : 0L;
        }();
        static long s_count = 0;
        return s_limit != 0L && s_count++ < s_limit;
    }

    uint16_t read16(uint32_t phys)
    {
        psxRegs.cycle = static_cast<u32>(*s_cycles);
        const uint16_t value = SPU2read(phys);
        if (logging())
            std::fprintf(stderr, "[spu2] r %x = %04x @%u\n", phys, value, psxRegs.cycle);
        return value;
    }

    void write16(uint32_t phys, uint16_t value)
    {
        psxRegs.cycle = static_cast<u32>(*s_cycles);
        if (logging())
            std::fprintf(stderr, "[spu2] w %x = %04x @%u\n", phys, value, psxRegs.cycle);
        SPU2write(phys, value);
    }

    // As pcsx2's psxDma4 and psxDma7: the size in 16-bit units, the direction in CHCR
    void dma(bool secondCore, uint32_t madr, uint32_t bcr, uint32_t chcr)
    {
        psxRegs.cycle = static_cast<u32>(*s_cycles);
        const uint32_t words = std::max<uint32_t>(bcr >> 16, 1u) * std::max<uint32_t>(bcr & 0xFFFFu, 1u);
        u16 *const memory = reinterpret_cast<u16 *>(iopPhysMem(madr));
        const bool toSpu = (chcr & 1u) != 0u;
        if (logging())
            std::fprintf(stderr, "[spu2] dma core %d madr %x bcr %x chcr %x @%u\n", secondCore ? 1 : 0, madr, bcr, chcr, psxRegs.cycle);
        if (!secondCore)
        {
            HW_DMA4_MADR = madr;
            HW_DMA4_BCR = bcr;
            HW_DMA4_CHCR = chcr;
            toSpu ? SPU2writeDMA4Mem(memory, words * 2u) : SPU2readDMA4Mem(memory, words * 2u);
        }
        else
        {
            HW_DMA7_MADR = madr;
            HW_DMA7_BCR = bcr;
            HW_DMA7_CHCR = chcr;
            toSpu ? SPU2writeDMA7Mem(memory, words * 2u) : SPU2readDMA7Mem(memory, words * 2u);
        }
    }

    uint32_t takeIrqs()
    {
        const uint32_t irqs = s_irqs;
        s_irqs = 0u;
        return irqs;
    }

    const ps2x::iop::detail::Spu2Hooks kHooks{attach, advance, read16, write16, dma, takeIrqs};
}

// ---- what the SPU2's sources ask of the rest of PCSX2
u8 *iopPhysMem(u32 address)
{
    return s_ram + (address & 0x1FFFFFu);
}

void spu2DMA4Irq()
{
    SPU2interruptDMA4();
    HW_DMA4_CHCR &= ~0x01000000u;
    s_irqs |= ps2x::iop::detail::Spu2Hooks::kDma4Done;
}

void spu2DMA7Irq()
{
    SPU2interruptDMA7();
    HW_DMA7_CHCR &= ~0x01000000u;
    s_irqs |= ps2x::iop::detail::Spu2Hooks::kDma7Done;
}

void spu2Irq()
{
    s_irqs |= ps2x::iop::detail::Spu2Hooks::kSpu2Irq;
}

// ---- from pcsx2/SPU2/spu2.cpp
void SPU2readDMA4Mem(u16 *pMem, u32 size)
{
    TimeUpdate(psxRegs.cycle);
    Cores[0].DoDMAread(pMem, size);
}

void SPU2writeDMA4Mem(u16 *pMem, u32 size)
{
    TimeUpdate(psxRegs.cycle);
    Cores[0].DoDMAwrite(pMem, size);
}

void SPU2interruptDMA4()
{
    if (Cores[0].DmaMode)
        Cores[0].Regs.STATX |= 0x80;
    Cores[0].Regs.STATX &= ~0x400;
    Cores[0].TSA = Cores[0].ActiveTSA;
}

void SPU2interruptDMA7()
{
    if (Cores[1].DmaMode)
        Cores[1].Regs.STATX |= 0x80;
    Cores[1].Regs.STATX &= ~0x400;
    Cores[1].TSA = Cores[1].ActiveTSA;
}

void SPU2readDMA7Mem(u16 *pMem, u32 size)
{
    TimeUpdate(psxRegs.cycle);
    Cores[1].DoDMAread(pMem, size);
}

void SPU2writeDMA7Mem(u16 *pMem, u32 size)
{
    TimeUpdate(psxRegs.cycle);
    Cores[1].DoDMAwrite(pMem, size);
}

u16 SPU2read(u32 rmem)
{
    u16 ret = 0xDEAD;
    u32 core = 0;
    const u32 mem = rmem & 0xFFFF;
    u32 omem = mem;
    if (mem & 0x400)
    {
        omem ^= 0x400;
        core = 1;
    }
    if (omem == 0x1f9001AC) // (as in pcsx2: mem has only the low half, so this is the register below in practice)
    {
        Cores[core].ActiveTSA = Cores[core].TSA;
        for (int i = 0; i < 2; i++)
        {
            if (Cores[i].IRQEnable && (Cores[i].IRQA == Cores[core].ActiveTSA))
                SetIrqCall(i);
        }
        ret = Cores[core].DmaRead();
    }
    else
    {
        TimeUpdate(psxRegs.cycle);
        if (mem >= 0x800)
            ret = spu2Ru16(mem);
        else
            ret = *(regtable[(mem >> 1)]);
    }
    return ret;
}

void SPU2write(u32 rmem, u16 value)
{
    TimeUpdate(psxRegs.cycle);
    SPU2_FastWrite(rmem, value);
}

void spu2Output(StereoOut32 out)
{
    float conv[2] = {static_cast<float>(clamp_mix(out.Left)) / INT16_MAX, static_cast<float>(clamp_mix(out.Right)) / INT16_MAX};
    // (some games leave a voice paused at volume: a DC offset; pcsx2 filters it out)
    for (int c = 0; c < 2; ++c)
    {
        const float output = conv[c] - s_dcIn[c] + 0.995f * s_dcOut[c];
        s_dcIn[c] = conv[c];
        s_dcOut[c] = output;
        s_chunk[s_chunkPos++] = output;
    }
    if (s_chunkPos == s_chunk.size())
    {
        s_chunkPos = 0u;
        writeChunk();
    }
}

bool SPU2::IsSwapChannels()
{
    return false;
}

bool SPU2::IsRunningPSXMode()
{
    return false;
}

u32 SPU2::GetConsoleSampleRate()
{
    return SPU2::SAMPLE_RATE;
}

// Called by the host before the game starts, on the main thread.
void hl_spu2_install()
{
    if (const char *value = std::getenv("HL_SPU2"); value && value[0] == '0')
        return;
    ps2x::iop::detail::g_spu2 = &kHooks;
    if (!SDL_InitSubSystem(SDL_INIT_AUDIO))
    {
        std::fprintf(stderr, "[spu2] no audio: %s\n", SDL_GetError());
        return;
    }
    const SDL_AudioSpec spec{SDL_AUDIO_F32, 2, static_cast<int>(SPU2::SAMPLE_RATE)};
    s_stream = SDL_OpenAudioDeviceStream(SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK, &spec, nullptr, nullptr);
    if (!s_stream)
        std::fprintf(stderr, "[spu2] no audio stream: %s\n", SDL_GetError());
}
