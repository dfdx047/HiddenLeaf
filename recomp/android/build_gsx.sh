#!/usr/bin/env bash
# recomp/android/build_gsx.sh: libbt4gsx.so of Hidden Leaf (recomp/android/gsx), into android/app/src/main/jniLibs. ARMSX2's
# core comes from Dragon Rage's tree (port/build/bt4/ARMSX2: 90126bdfea + its patch, built once by build_bt4.py --armsx2 and
# not changed here); the build folder is build/gsx. Long the first time (the whole core and shaderc).
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
DR=${DR:-$HOME/AndroidStudioProjects/DragonRage}
W="$DR/port/build/bt4"
TC="$DR/port/build/android-toolchain"
out="$here/build/gsx"
jni="$here/android/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$out"
echo 'set(LINUX FALSE)
set(ANDROID TRUE)' > "$out/after_project.cmake"
# (Dragon Rage's toolchain file, with this folder's after_project.cmake)
sed "s#^set(CMAKE_PROJECT_INCLUDE .*#set(CMAKE_PROJECT_INCLUDE \"$out/after_project.cmake\")#" "$W/armsx2-build/android.cmake" > "$out/android.cmake"
if [ ! -e "$out/build.ninja" ]; then
    cmake -S "$W/ARMSX2/platforms/android/app/src/main/cpp" -B "$out" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_TOOLCHAIN_FILE="$out/android.cmake" -DANDROID=true -DANDROID_STL=c++_static -DARMSX2_ANDROID_HOST_PAGE_SIZE=0x1000 \
        -DARMSX2_DISABLE_LIBRASHADER=ON -DARMSX2_EMUCORE_LIBRARY_NAME=emucore -DHAVE_WL_VERSION_SCRIPT=1 -DCMAKE_HAVE_LIBC_PTHREAD=1 \
        -DANDROID_NDK="$DR/port/bt4/gsx/ndk" -DARMSX2_EXTRA_CMAKE="$here/recomp/android/gsx/hlgsx.cmake" \
        -DBT4GSX_DIR="$here/recomp/android/gsx" -DBT4GSX_RING_DIR="$here/build/PS2Recomp/ps2xRuntime/src/lib/gs"
fi
cmake --build "$out" --target bt4gsx -j"${JOBS:-$(nproc)}"
"$TC/llvm/bin/llvm-strip" --strip-debug -o "$jni/libbt4gsx.so" "$out/libbt4gsx.so"
