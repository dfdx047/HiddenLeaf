// The few raylib functions the runtime still calls, without raylib: the window, the sound and the input belong to the host
// (un5_main.cpp) here. Only the pad does anything: it reads the state the host keeps (touch controller and gamepads).
#include "raylib.h"

#include "hl_host_pad.h"

#include <atomic>

std::atomic<unsigned> g_hlPadButtons{0};
std::atomic<unsigned> g_hlPadAxes{0x80808080u};

namespace
{
    // PS2 pad bits, active high, libpad's order (the launcher's Ps2 object): SELECT 0x0001, L3 2, R3 4, START 8, UP 0x10,
    // RIGHT 0x20, DOWN 0x40, LEFT 0x80, L2 0x100, R2 0x200, L1 0x400, R1 0x800, TRIANGLE 0x1000, CIRCLE 0x2000, CROSS 0x4000,
    // SQUARE 0x8000
    unsigned padBit(int button)
    {
        switch (button)
        {
        case GAMEPAD_BUTTON_MIDDLE_LEFT: return 0x0001u;
        case GAMEPAD_BUTTON_LEFT_THUMB: return 0x0002u;
        case GAMEPAD_BUTTON_RIGHT_THUMB: return 0x0004u;
        case GAMEPAD_BUTTON_MIDDLE_RIGHT: return 0x0008u;
        case GAMEPAD_BUTTON_LEFT_FACE_UP: return 0x0010u;
        case GAMEPAD_BUTTON_LEFT_FACE_RIGHT: return 0x0020u;
        case GAMEPAD_BUTTON_LEFT_FACE_DOWN: return 0x0040u;
        case GAMEPAD_BUTTON_LEFT_FACE_LEFT: return 0x0080u;
        case GAMEPAD_BUTTON_LEFT_TRIGGER_2: return 0x0100u;
        case GAMEPAD_BUTTON_RIGHT_TRIGGER_2: return 0x0200u;
        case GAMEPAD_BUTTON_LEFT_TRIGGER_1: return 0x0400u;
        case GAMEPAD_BUTTON_RIGHT_TRIGGER_1: return 0x0800u;
        case GAMEPAD_BUTTON_RIGHT_FACE_UP: return 0x1000u;
        case GAMEPAD_BUTTON_RIGHT_FACE_RIGHT: return 0x2000u;
        case GAMEPAD_BUTTON_RIGHT_FACE_DOWN: return 0x4000u;
        case GAMEPAD_BUTTON_RIGHT_FACE_LEFT: return 0x8000u;
        default: return 0u;
        }
    }
}

extern "C"
{
    void SetConfigFlags(unsigned int) {}
    void InitWindow(int, int, const char *) {}
    void CloseWindow(void) {}
    bool WindowShouldClose(void) { return false; }
    bool IsWindowReady(void) { return true; }
    void SetTargetFPS(int) {}
    int GetScreenWidth(void) { return 640; }
    int GetScreenHeight(void) { return 448; }
    void InitAudioDevice(void) {}
    void CloseAudioDevice(void) {}
    bool IsAudioDeviceReady(void) { return false; }
    Wave LoadWaveFromMemory(const char *, const unsigned char *, int) { return {}; }
    void UnloadWave(Wave) {}
    Sound LoadSoundFromWave(Wave) { return {}; }
    void UnloadSound(Sound) {}
    void PlaySound(Sound) {}
    void StopSound(Sound) {}
    bool IsSoundPlaying(Sound) { return false; }
    void SetSoundPitch(Sound, float) {}
    void SetSoundVolume(Sound, float) {}
    bool IsKeyDown(int) { return false; }
    bool IsKeyPressed(int) { return false; }
    bool IsGamepadAvailable(int gamepad) { return gamepad == 0; }
    bool IsGamepadButtonDown(int gamepad, int button)
    {
        return gamepad == 0 && (g_hlPadButtons.load(std::memory_order_relaxed) & padBit(button)) != 0u;
    }
    float GetGamepadAxisMovement(int gamepad, int axis)
    {
        if (gamepad != 0 || axis < 0 || axis > 3)
            return 0.0f;
        const unsigned axes = g_hlPadAxes.load(std::memory_order_relaxed); // lx | ly << 8 | rx << 16 | ry << 24, 0x80 centred
        const int value = static_cast<int>((axes >> (8 * axis)) & 0xFFu);
        return (value - 128) / 128.0f;
    }
}
