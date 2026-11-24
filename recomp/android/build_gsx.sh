#!/usr/bin/env bash
# recomp/android/build_gsx.sh: libbt4gsx.so of Hidden Leaf (recomp/android/gsx), into android/app/src/main/jniLibs. ARMSX2's
# core comes from Dragon Rage's tree (port/build/bt4/ARMSX2: 90126bdfea + its patch, built once by build_bt4.py --armsx2),
# copied into build/armsx2-hl and changed there by recomp/android/gsx/patches/*.patch (Dragon Rage's tree is only read).
# The build folder is build/gsx. Long the first time (the whole core and shaderc).
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
DR=${DR:-$HOME/AndroidStudioProjects/DragonRage}
W="$DR/port/build/bt4"
TC="$DR/port/build/android-toolchain"
out="$here/build/gsx"
jni="$here/android/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$out"
# Hidden Leaf's copy of the core, with its patches: made again from Dragon Rage's tree when the patches change (a git
# repository in build/ with that tree as its first commit, so a patch is `git -C build/armsx2-hl diff`)
src="$here/build/armsx2-hl"
stamp=$( (cat "$here"/recomp/android/gsx/patches/*.patch 2>/dev/null || true) | sha256sum | cut -c1-16)
if [ ! -e "$src/.hl-patches-$stamp" ]; then
    if [ ! -d "$src/.git" ]; then
        rsync -a --exclude /.git/ "$W/ARMSX2/" "$src/"
        git -C "$src" init -q
        git -C "$src" add -A >/dev/null 2>&1
        git -C "$src" -c user.name=hl -c user.email=hl@localhost commit -q -m "ARMSX2 as Dragon Rage builds it"
    fi
    git -C "$src" checkout -q -- .
    rm -f "$src"/.hl-patches-*
    for p in "$here"/recomp/android/gsx/patches/*.patch; do [ -e "$p" ] && git -C "$src" apply "$p"; done
    touch "$src/.hl-patches-$stamp"
fi
echo 'set(LINUX FALSE)
set(ANDROID TRUE)' > "$out/after_project.cmake"
# (Dragon Rage's toolchain file, with this folder's after_project.cmake)
sed "s#^set(CMAKE_PROJECT_INCLUDE .*#set(CMAKE_PROJECT_INCLUDE \"$out/after_project.cmake\")#" "$W/armsx2-build/android.cmake" > "$out/android.cmake"
if [ ! -e "$out/build.ninja" ]; then
    cmake -S "$src/platforms/android/app/src/main/cpp" -B "$out" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_TOOLCHAIN_FILE="$out/android.cmake" -DANDROID=true -DANDROID_STL=c++_static -DARMSX2_ANDROID_HOST_PAGE_SIZE=0x1000 \
        -DARMSX2_DISABLE_LIBRASHADER=ON -DARMSX2_ENABLE_ZEROFG=ON -DZEROFG_SOURCE_DIR="$here/build/zerofg-src" \
        -DARMSX2_EMUCORE_LIBRARY_NAME=emucore -DHAVE_WL_VERSION_SCRIPT=1 -DCMAKE_HAVE_LIBC_PTHREAD=1 \
        -DANDROID_NDK="$DR/port/bt4/gsx/ndk" -DARMSX2_EXTRA_CMAKE="$here/recomp/android/gsx/hlgsx.cmake" \
        -DBT4GSX_DIR="$here/recomp/android/gsx" -DBT4GSX_RING_DIR="$here/build/PS2Recomp/ps2xRuntime/src/lib/gs"
fi
cmake --build "$out" --target bt4gsx -j"${JOBS:-$(nproc)}"
"$TC/llvm/bin/llvm-strip" --strip-debug -o "$jni/libbt4gsx.so" "$out/libbt4gsx.so"
