#!/usr/bin/env bash
# recomp/pc/build_gsx_pc.sh: ARMSX2's GS runner for the PC (Linux x86_64, Vulkan), the same GS the Android build uses
# (libbt4gsx: ARMSX2 replaying the live GS stream), so the PC can draw what the devices draw and compare it with PCSX2.
#   build/armsx2-pc   a local clone of Dragon Rage's ARMSX2 (same commit, Dragon Rage's local changes, then
#                     recomp/pc/armsx2-pc.patch: the x86 EE recompiler after ARMSX2's FPRreg change, and no AVX-512,
#                     whose 64-byte aligned stores fault on objects PCSX2 allocates 32-aligned)
#   build/gsx-pc-deps plutovg and plutosvg from its 3rdparty (Linux PCSX2 expects them installed)
#   build/gsx-pc      the build (clang: GCC rejects ARMSX2's designated initializers); bin/pcsx2-gsrunner
# Dragon Rage's tree is not changed. Long the first time (the whole core).
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
DR=${DR:-$HOME/AndroidStudioProjects/DragonRage}
A="$DR/port/build/bt4/ARMSX2"
b="$here/build"
if [ ! -d "$b/armsx2-pc/.git" ]; then
    git clone -q --shared --no-checkout "$A" "$b/armsx2-pc"
    git -C "$b/armsx2-pc" checkout -q "$(git -C "$A" rev-parse HEAD)"
    git -C "$A" diff > "$b/armsx2-dr.diff"
    git -C "$b/armsx2-pc" apply "$b/armsx2-dr.diff"
    git -C "$b/armsx2-pc" apply "$here/recomp/pc/armsx2-pc.patch"
fi
D="$b/gsx-pc-deps"
for p in plutovg plutosvg; do
    if [ ! -d "$D/lib/cmake/$p" ]; then
        cmake -S "$b/armsx2-pc/3rdparty/$p" -B "$D/b-$p" -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$D" \
            -DCMAKE_PREFIX_PATH="$D" -DBUILD_SHARED_LIBS=OFF -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DPLUTOSVG_ENABLE_FREETYPE=ON \
            -DPLUTOSVG_BUILD_EXAMPLES=OFF -DPLUTOVG_BUILD_EXAMPLES=OFF
        cmake --build "$D/b-$p"
        cmake --install "$D/b-$p"
    fi
done
if [ ! -e "$b/gsx-pc/build.ninja" ]; then
    cmake -S "$b/armsx2-pc" -B "$b/gsx-pc" -G Ninja -DCMAKE_BUILD_TYPE=Release -DENABLE_QT_UI=OFF -DENABLE_GSRUNNER=ON \
        -DENABLE_TESTS=OFF -DUSE_BACKTRACE=OFF -DWAYLAND_API=OFF -DCMAKE_PREFIX_PATH="$D" \
        -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++
fi
ninja -C "$b/gsx-pc" pcsx2-gsrunner
ln -sfn ../../armsx2-pc/bin/resources "$b/gsx-pc/bin/resources"
echo "$b/gsx-pc/bin/pcsx2-gsrunner"
