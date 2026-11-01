#pragma once

// Shared-memory ring between the PS2Recomp runtime (producer) and the PCSX2-based GS host
// (consumer, "pcsx2-gsrunner -live"). The byte stream is exactly a PCSX2 GS dump (.gs): the
// file header, then Transfer / Registers / VSync / ReadFIFO2 packets. The consumer also
// publishes the host window's keyboard state back, since that window has the focus.
// Keep this file identical in both source trees.

#include <cstdint>
#include <string>

#if defined(__APPLE__)
#include <os/os_sync_wait_on_address.h>
#elif defined(__EMSCRIPTEN__)
#include <emscripten/threading.h>
#elif defined(__linux__)
#include <climits>
#include <ctime>
#include <linux/futex.h>
#include <sys/syscall.h>
#include <unistd.h>
#endif

namespace ps2live
{
    constexpr uint32_t kMagic = 0x50324C52u; // "RL2P"
    constexpr uint32_t kVersion = 2u; // 2: ReadFIFO2 replies (readback area after the events)
    constexpr uint64_t kDefaultCapacity = 256ull << 20;

    constexpr const wchar_t *kMappingName = L"Local\\PS2RecompGsLiveRing";
    constexpr const wchar_t *kDataEventName = L"Local\\PS2RecompGsLiveData";
    constexpr const wchar_t *kSpaceEventName = L"Local\\PS2RecompGsLiveSpace";

    // Pad buttons, active high, in the libpad bit order (bit 0 = SELECT ... bit 15 = SQUARE).
    struct alignas(64) RingHeader
    {
        uint32_t magic;
        uint32_t version;
        uint64_t capacity; // bytes of the data area after the header
        volatile int64_t writePos; // monotonic byte counters
        alignas(64) volatile int64_t readPos;
        alignas(64) volatile uint32_t producerAlive;
        volatile uint32_t consumerAlive;
        volatile uint32_t consumerQuit; // window closed: the game should exit
        volatile uint32_t padButtons;
        volatile uint32_t padAxes; // lx | ly << 8 | rx << 16 | ry << 24 (0x80 = centre)
        volatile uint32_t padSequence;
        volatile uint32_t hostMenuOpen; // the host's options menu is open: the game gets no pad input
        // Wake-ups only when needed: each side sets its *Flags to 1 at start (it understands the
        // *Waiting fields) and raises *Waiting while blocked on its event. The other side then
        // signals only if that side is waiting (or is an older build with no flags).
        volatile uint32_t consumerFlags;
        volatile uint32_t consumerWaiting; // host blocked on the data event
        volatile uint32_t producerFlags;
        volatile uint32_t producerWaiting; // game blocked on the space event (ring full)
    };

    // Object names for one game instance: base name + "_" + suffix (the game's process id, passed
    // to the host as "-livering <suffix>"), so several instances never share a ring.
    inline std::wstring objectName(const wchar_t *base, const std::wstring &suffix)
    {
        return suffix.empty() ? std::wstring(base) : std::wstring(base) + L"_" + suffix;
    }

    inline uint8_t *ringData(RingHeader *header)
    {
        return reinterpret_cast<uint8_t *>(header) + sizeof(RingHeader);
    }

#if !defined(_WIN32)
    // POSIX: the ring is a shm_open object and the two Windows auto-reset events are words in
    // the same mapping, after the data area (macOS: os_sync_wait_on_address, Linux: futex).
    // macOS limits shm names to 31 characters, so the base name is short.
    constexpr const char *kPosixMappingName = "/ps2lr";

    struct alignas(64) RingEvents
    {
        alignas(64) uint32_t data;  // set by the producer: new bytes in the ring
        alignas(64) uint32_t space; // set by the consumer: ring space freed
        // GS -> EE downloads: for each ReadFIFO2 packet the consumer copies PCSX2's FIFO data into the readback
        // area, stores its size, bumps readbackSeq and sets readback (the game waits for it).
        alignas(64) uint32_t readback;
        uint32_t readbackSeq;
        uint32_t readbackBytes;
    };
    constexpr uint64_t kReadbackCapacity = 8ull << 20; // a 1024x1024 32-bit download plus slack

    inline std::string objectName(const char *base, const std::string &suffix)
    {
        return suffix.empty() ? std::string(base) : std::string(base) + "_" + suffix;
    }

    inline uint64_t mappingSize(uint64_t capacity)
    {
        return sizeof(RingHeader) + capacity + sizeof(RingEvents) + kReadbackCapacity;
    }

    inline RingEvents *ringEvents(RingHeader *header)
    {
        return reinterpret_cast<RingEvents *>(ringData(header) + header->capacity);
    }

    inline uint8_t *readbackData(RingHeader *header)
    {
        return reinterpret_cast<uint8_t *>(ringEvents(header)) + sizeof(RingEvents);
    }

    // SetEvent on an auto-reset event.
    inline void eventSet(uint32_t *word)
    {
        __atomic_store_n(word, 1u, __ATOMIC_SEQ_CST);
#if defined(__APPLE__)
        os_sync_wake_by_address_any(word, sizeof(*word), OS_SYNC_WAKE_BY_ADDRESS_SHARED);
#elif defined(__EMSCRIPTEN__)
        emscripten_futex_wake(word, 1);
#elif defined(__linux__)
        syscall(SYS_futex, word, FUTEX_WAKE, 1, nullptr, nullptr, 0);
#endif
    }

    // WaitForSingleObject(event, timeoutMs) on an auto-reset event: true if it was signalled.
    inline bool eventWait(uint32_t *word, uint32_t timeoutMs)
    {
        if (__atomic_exchange_n(word, 0u, __ATOMIC_ACQ_REL) != 0u)
            return true;
#if defined(__APPLE__)
        os_sync_wait_on_address_with_timeout(word, 0u, sizeof(*word), OS_SYNC_WAIT_ON_ADDRESS_SHARED,
                                             OS_CLOCK_MACH_ABSOLUTE_TIME, static_cast<uint64_t>(timeoutMs) * 1000000ull);
#elif defined(__EMSCRIPTEN__)
        emscripten_futex_wait(word, 0u, static_cast<double>(timeoutMs)); // never on the browser's main thread
#elif defined(__linux__)
        const timespec timeout{static_cast<time_t>(timeoutMs / 1000u), static_cast<long>(timeoutMs % 1000u) * 1000000l};
        syscall(SYS_futex, word, FUTEX_WAIT, 0, &timeout, nullptr, 0);
#endif
        return __atomic_exchange_n(word, 0u, __ATOMIC_ACQ_REL) != 0u;
    }
#endif
}
