#!/usr/bin/env bash
# recomp/android/build.sh: libun5.so for Android arm64, from what recomp/build.sh left in build/ (PS2Recomp with its patches
# and the generated code), and everything the launcher needs next to it, into android/app/src/main (git ignores both:
# jniLibs/ carries the game's code). Then: cd android && ./gradlew assembleDebug
#
# Comes from Dragon Rage's tree, built by its port/tools/android.py and port/bt4/build_bt4.py --armsx2 (DR=<folder>):
# SDL3, FFmpeg, libbt4gsx.so (PCSX2's GS with Vulkan, ARMSX2 90126bdfea + patches/armsx2-bt4.patch) and its resources.
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
B="$here/build"
DR=${DR:-$HOME/AndroidStudioProjects/DragonRage}
NDK=${NDK:-$HOME/Android/Sdk/ndk/29.0.14206865}
out="$B/android"
jni="$here/android/app/src/main/jniLibs/arm64-v8a"
assets="$here/android/app/src/main/assets/gsx"
SDL3="$DR/port/build/android-toolchain/sdl3"
FFMPEG="$DR/port/build/bt4/ffmpeg"
for need in "$SDL3/lib/libSDL3.so" "$FFMPEG/lib/libavcodec.a" "$DR/android/app/src/main/jniLibs/arm64-v8a/libbt4gsx.so"; do
    [ -e "$need" ] || { echo "missing $need: build Dragon Rage's Android parts first (port/tools/android.py, port/bt4/build_bt4.py --armsx2)" >&2; exit 1; }
done

cmake -S "$here/recomp/android" -B "$out" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 \
    -DANDROID_STL=c++_static -DPS2R="$B/PS2Recomp" -DSDL3="$SDL3" -DFFMPEG="$FFMPEG" -DARMSX2="$DR/port/build/bt4/ARMSX2"
cmake --build "$out" --target un5 -j"${JOBS:-$(nproc)}"

mkdir -p "$jni"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-debug -o "$jni/libun5.so" "$out/libun5.so"
cp -u "$SDL3/lib/libSDL3.so" "$jni/"
for f in libbt4gsx.so libfile_redirect_hook.so libgsl_alloc_hook.so libhook_impl.so libmain_hook.so; do
    cp -u "$DR/android/app/src/main/jniLibs/arm64-v8a/$f" "$jni/"
done
if [ ! -e "$assets/version.txt" ] || ! cmp -s "$assets/version.txt" "$DR/android/app/src/main/assets/bt4gsx/version.txt"; then
    rm -rf "$assets"
    mkdir -p "$(dirname "$assets")"
    cp -r "$DR/android/app/src/main/assets/bt4gsx" "$assets"
fi
ls -la "$jni" | sed 's/^/  /'
