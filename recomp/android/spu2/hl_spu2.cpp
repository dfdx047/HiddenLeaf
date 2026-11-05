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
#include "../host/hl_game.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>

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
        // HL_PERF: how loud it was, a line a second
        static const bool s_levels = std::getenv("HL_PERF") != nullptr;
        if (s_levels)
        {
            static float s_peak = 0.0f;
            static double s_sum = 0.0;
            static uint32_t s_chunks = 0u;
            for (const float v : s_chunk)
            {
                s_peak = std::max(s_peak, std::fabs(v));
                s_sum += static_cast<double>(v) * v;
            }
            if (++s_chunks == 48000u / kChunkFrames)
            {
                std::fprintf(stderr, "[snd] rms %.4f peak %.3f queued %d playing %d\n", std::sqrt(s_sum / (s_chunks * s_chunk.size())), s_peak,
                             s_stream ? SDL_GetAudioStreamQueued(s_stream) : -1, s_playing ? 1 : 0);
                s_peak = 0.0f;
                s_sum = 0.0;
                s_chunks = 0u;
            }
        }
        if (!s_stream)
            return;
        const int queued = SDL_GetAudioStreamQueued(s_stream);
        // HL_SPU2_REC=<file>: the sound that goes to the device, as 48 kHz stereo 16-bit, on the wall clock: while the device's
        // queue ran dry the speaker played nothing, and so many zeros go in, so the file plays as the speaker did. The log says
        // when its first sample is ([snd-rec] start, epoch ms), for putting it under a screen recording.
        static FILE *const s_rec = []() -> FILE * {
            const char *path = std::getenv("HL_SPU2_REC");
            return path ? std::fopen(path, "wb") : nullptr;
        }();
        if (s_rec)
        {
            static int64_t s_t0 = 0;
            static uint64_t s_recFrames = 0;
            timespec ts{};
            clock_gettime(CLOCK_REALTIME, &ts);
            const int64_t nowMs = static_cast<int64_t>(ts.tv_sec) * 1000 + ts.tv_nsec / 1000000;
            if (s_t0 == 0)
            {
                s_t0 = nowMs;
                std::fprintf(stderr, "[snd-rec] start %lld\n", static_cast<long long>(s_t0));
            }
            if (queued == 0 && queued <= kMaxBytes)
            {
                const int64_t due = (nowMs - s_t0) * 48;
                static std::array<int16_t, 4096> s_zero{};
                for (int64_t pad = due - static_cast<int64_t>(s_recFrames); pad > 0;)
                {
                    const size_t n = static_cast<size_t>(std::min<int64_t>(pad, static_cast<int64_t>(s_zero.size() / 2u)));
                    std::fwrite(s_zero.data(), sizeof(int16_t) * 2u, n, s_rec);
                    s_recFrames += n;
                    pad -= static_cast<int64_t>(n);
                }
            }
            if (queued <= kMaxBytes)
            {
                std::array<int16_t, kChunkFrames * 2u> pcm;
                for (size_t i = 0; i < pcm.size(); ++i)
                    pcm[i] = static_cast<int16_t>(std::lrint(std::clamp(s_chunk[i], -1.0f, 1.0f) * 32767.0f));
                std::fwrite(pcm.data(), sizeof(pcm), 1, s_rec);
                s_recFrames += kChunkFrames;
            }
        }
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
        // Hold the queue near kTargetBytes by playing up to 1% faster or slower. Without it the queue settles wherever the
        // first bursts left it (117-197 ms in the films, Odin and Cube, 2026-11-05) and the sound plays that far behind the
        // picture. The average queue moves about 1 s behind (a chunk is 5.3 ms). HL_SPU2_DRIFT=0: off.
        static const bool s_drift = []() {
            const char *e = std::getenv("HL_SPU2_DRIFT");
            return e == nullptr || e[0] != '0';
        }();
        if (s_drift && s_playing)
        {
            constexpr float kTargetBytes = 48000.0f * kBytesPerFrame * 80.0f / 1000.0f; // (60 ms ran nearly dry at the films' ends)
            static float s_avg = kTargetBytes, s_ratio = 1.0f;
            s_avg += (static_cast<float>(SDL_GetAudioStreamQueued(s_stream)) - s_avg) * (1.0f / 190.0f);
            const float ratio = 1.0f + std::clamp((s_avg - kTargetBytes) / kTargetBytes * 0.01f, -0.01f, 0.01f);
            if (std::fabs(ratio - s_ratio) >= 0.0005f)
            {
                SDL_SetAudioStreamFrequencyRatio(s_stream, ratio);
                s_ratio = ratio;
            }
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
    float filtered[2];
    for (int c = 0; c < 2; ++c)
    {
        const float output = conv[c] - s_dcIn[c] + 0.995f * s_dcOut[c];
        s_dcIn[c] = conv[c];
        s_dcOut[c] = output;
        filtered[c] = output;
    }
    // Peak limiter, both channels linked (HL_SPU2_LIMIT=0 turns it off): the DC filter takes a mix that clamp_mix already
    // saturated past 1.0 (up to 1.46 seen in the route, half of the seconds above 1.0), where PCSX2's output to 16 bits
    // clips it. Instant attack, 100 ms release, ceiling 0.98: the gain only drops around a peak, so the loudness is the
    // same as PCSX2's and nothing goes past the ceiling.
    static const bool s_limit = []() { const char *v = std::getenv("HL_SPU2_LIMIT"); return v == nullptr || v[0] != '0'; }();
    if (s_limit)
    {
        constexpr float kCeiling = 0.98f;
        constexpr float kRelease = 0.99979f; // exp(-1 / (0.1 s * 48000))
        static float s_env = 0.0f;
        const float level = std::max(std::fabs(filtered[0]), std::fabs(filtered[1]));
        s_env = std::max(level, s_env * kRelease);
        if (s_env > kCeiling)
        {
            const float gain = kCeiling / s_env;
            filtered[0] *= gain;
            filtered[1] *= gain;
        }
    }
    // HL_SND_ONSET=1: where the sound starts and stops (10 ms windows, before the limiter), with the vsync, against the
    // films' [mpeg-pic] lines
    static const bool s_onset = std::getenv("HL_SND_ONSET") != nullptr;
    if (s_onset)
    {
        static float s_sq = 0.0f;
        static int s_n = 0, s_quiet = 0, s_logs = 0;
        static bool s_loud = false;
        s_sq += filtered[0] * filtered[0] + filtered[1] * filtered[1];
        if (++s_n == 480)
        {
            const float rms = std::sqrt(s_sq / 960.0f);
            s_sq = 0.0f;
            s_n = 0;
            if (!s_loud && rms > 0.02f)
            {
                s_loud = true;
                s_quiet = 0;
                if (s_logs++ < 400)
                    std::fprintf(stderr, "[snd-on] v%u rms %.3f\n", hl_game::g_vsync.load(std::memory_order_relaxed), rms);
            }
            else if (s_loud && rms < 0.003f && ++s_quiet == 5)
            {
                s_loud = false;
                if (s_logs++ < 400)
                    std::fprintf(stderr, "[snd-off] v%u (quiet for 50 ms)\n", hl_game::g_vsync.load(std::memory_order_relaxed));
            }
            else if (rms >= 0.003f)
                s_quiet = 0;
        }
    }
    s_chunk[s_chunkPos++] = filtered[0];
    s_chunk[s_chunkPos++] = filtered[1];
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

// The panel's pause: the queued sound stays where it is.
void hl_spu2_pause(bool paused)
{
    if (!s_stream || !s_playing)
        return;
    if (paused)
        SDL_PauseAudioStreamDevice(s_stream);
    else
        SDL_ResumeAudioStreamDevice(s_stream);
}
