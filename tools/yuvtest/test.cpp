// tools/yuvtest: the film converter of patch 0062 (hl_yuv2rgba.h) against swscale's C path, frame by frame.
// Input: raw yuv420p 512x448 frames, e.g. ffmpeg -i build/disc/PSS/OPENING.PSS -frames:v 200 -f rawvideo -pix_fmt yuv420p o.yuv
// PC:      g++ -O2 -I build/PS2Recomp/ps2xRuntime/src/lib/Kernel/Stubs tools/yuvtest/test.cpp $(pkg-config --cflags --libs libswscale libavutil)
// Android: the NDK clang++ (aarch64-linux-android29) with DragonRage's port/build/bt4/ffmpeg libswscale.a and libavutil.a,
//          pushed to /data/local/tmp and run there with the .yuv next to it.
#include "hl_yuv2rgba.h"
#include <cstdio>
#include <cstdlib>
#include <vector>
#include <chrono>
extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/cpu.h>
#include <libavutil/pixfmt.h>
}
int main(int argc, char **argv)
{
    const int w = 512, h = 448, fs = w * h * 3 / 2;
    FILE *f = std::fopen(argv[1], "rb");
    std::vector<uint8_t> yuv(fs), a(w * h * 4), b(w * h * 4);
    av_force_cpu_flags(0);
    SwsContext *s = sws_getContext(w, h, AV_PIX_FMT_YUV420P, w, h, AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
    const hl_yuv::Coeffs c = hl_yuv::coeffs();
    std::printf("cy %d crv %d cbu %d cgu %d cgv %d off %d %d %d %d\n", c.cy, c.crv, c.cbu, c.cgu, c.cgv, c.offR, c.offB, c.offGU, c.offGV);
    long frames = 0, diffBytes = 0, diffNeon = 0; int maxd = 0; double tS = 0, tH = 0, tN = 0;
    std::vector<uint8_t> n(w * h * 4);
    while (std::fread(yuv.data(), 1, fs, f) == size_t(fs))
    {
        const uint8_t *src[3] = {yuv.data(), yuv.data() + w * h, yuv.data() + w * h * 5 / 4};
        int ss[3] = {w, w / 2, w / 2};
        uint8_t *dd[1] = {a.data()}; int dl[1] = {w * 4};
        auto t0 = std::chrono::steady_clock::now();
        sws_scale(s, src, ss, 0, h, dd, dl);
        auto t1 = std::chrono::steady_clock::now();
        hl_yuv::convertScalar(c, src[0], w, src[1], w / 2, src[2], w / 2, b.data(), w * 4, w, h);
        auto t2 = std::chrono::steady_clock::now();
        tS += std::chrono::duration<double, std::milli>(t1 - t0).count(); tH += std::chrono::duration<double, std::milli>(t2 - t1).count();
#if defined(__aarch64__)
        auto t3 = std::chrono::steady_clock::now();
        hl_yuv::convertNeon(c, src[0], w, src[1], w / 2, src[2], w / 2, n.data(), w * 4, w, h);
        tN += std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - t3).count();
        for (int i = 0; i < w * h * 4; ++i) diffNeon += a[i] != n[i];
#endif
        for (int i = 0; i < w * h * 4; ++i) { int d = std::abs(int(a[i]) - int(b[i])); if (d) { ++diffBytes; if (d > maxd) maxd = d; } }
        ++frames;
    }
    std::printf("%ld frames, %ld bytes differ, max diff %d, neon differs %ld; sws %.3f ms/frame, scalar %.3f, neon %.3f\n", frames, diffBytes, maxd, diffNeon, tS / frames, tH / frames, tN / frames);
    std::printf("sample sws %d %d %d %d ours %d %d %d %d\n", a[1000], a[1001], a[1002], a[1003], b[1000], b[1001], b[1002], b[1003]);
}
