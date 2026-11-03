#!/usr/bin/env bash
# recomp/android/build.sh: libun5.so for Android arm64, from what recomp/build.sh left in build/ (PS2Recomp with its patches
# and the generated code), and everything the launcher needs next to it, into android/app/src/main (git ignores both:
# jniLibs/ carries the game's code). Then: cd android && ./gradlew assembleDebug
#
# Comes from Dragon Rage's tree, built by its port/tools/android.py and port/bt4/build_bt4.py --armsx2 (DR=<folder>):
# SDL3, FFmpeg, libbt4gsx.so (PCSX2's GS with Vulkan, ARMSX2 90126bdfea + patches/armsx2-bt4.patch) and its resources.
#
#   recomp/android/build.sh --mod <id>: a mod's code pack, libun5_<id>.so (with - as _), from build/mods/<id> (recomp/build.sh
#   --mod): the launcher loads it in place of libun5.so while that mod is switched on.
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
B="$here/build"
mod=""
[ "${1:-}" = "--mod" ] && mod=${2:?usage: recomp/android/build.sh --mod <id>}
DR=${DR:-$HOME/AndroidStudioProjects/DragonRage}
NDK=${NDK:-$HOME/Android/Sdk/ndk/29.0.14206865}
out="$B/android"
ps2r="$B/PS2Recomp"
lib=libun5.so
pack=()
if [ -n "$mod" ]; then
    out="$B/android_$mod"; ps2r="$B/mods/$mod/PS2Recomp"; lib="libun5_${mod//-/_}.so"; pack=(-DHL_PACK_ID="$mod")
    [ -d "$ps2r/ps2xRuntime/src/runner" ] || { echo "no $ps2r: recomp/build.sh --mod <the mod's ISO> first" >&2; exit 1; }
    # (the runtime's own sources follow the base's: only the generated code is the mod's)
    rsync -a --exclude /ps2xRuntime/src/runner/ --exclude /ps2xRuntime/include/ps2_recompiled_functions.h --exclude /ps2xRuntime/include/ps2_recompiled_stubs.h \
        --exclude /build-tools/ --exclude /build-rt/ --exclude /.git/ "$B/PS2Recomp/" "$ps2r/"
fi
jni="$here/android/app/src/main/jniLibs/arm64-v8a"
assets="$here/android/app/src/main/assets/gsx"
SDL3="$DR/port/build/android-toolchain/sdl3"
FFMPEG="$DR/port/build/bt4/ffmpeg"
for need in "$SDL3/lib/libSDL3.so" "$FFMPEG/lib/libavcodec.a" "$DR/android/app/src/main/jniLibs/arm64-v8a/libbt4gsx.so"; do
    [ -e "$need" ] || { echo "missing $need: build Dragon Rage's Android parts first (port/tools/android.py, port/bt4/build_bt4.py --armsx2)" >&2; exit 1; }
done

cmake -S "$here/recomp/android" -B "$out" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 \
    -DANDROID_STL=c++_static -DPS2R="$ps2r" "${pack[@]}" -DSDL3="$SDL3" -DFFMPEG="$FFMPEG" -DARMSX2="$DR/port/build/bt4/ARMSX2"
cmake --build "$out" --target un5 -j"${JOBS:-$(nproc)}"

mkdir -p "$jni"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-debug -o "$jni/$lib" "$out/libun5.so"
cp -u "$SDL3/lib/libSDL3.so" "$jni/"
# (libbt4gsx.so: Hidden Leaf's own, recomp/android/build_gsx.sh, once it was built)
[ -e "$B/gsx/libbt4gsx.so" ] || cp -u "$DR/android/app/src/main/jniLibs/arm64-v8a/libbt4gsx.so" "$jni/"
for f in libfile_redirect_hook.so libgsl_alloc_hook.so libhook_impl.so libmain_hook.so; do
    cp -u "$DR/android/app/src/main/jniLibs/arm64-v8a/$f" "$jni/"
done
if [ ! -e "$assets/version.txt" ] || ! cmp -s "$assets/version.txt" "$DR/android/app/src/main/assets/bt4gsx/version.txt"; then
    rm -rf "$assets"
    mkdir -p "$(dirname "$assets")"
    cp -r "$DR/android/app/src/main/assets/bt4gsx" "$assets"
fi
ls -la "$jni" | sed 's/^/  /'
