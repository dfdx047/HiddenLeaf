#!/usr/bin/env bash
# recomp/pc/build.sh: un5pc, the PC runner with the Android's drawing (recomp/pc/CMakeLists.txt), into build/pc.
# Needs build/PS2Recomp with the generated code (recomp/build.sh) and libbt4gsx.so for Linux (recomp/pc/build_gsx_pc.sh).
# Run it in build/disc:  HL_GSX_ARGS="-surfaceless -dumpdir <dir>" ../pc/un5pc SLES_556.05
set -euo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
B="$here/build"
NOMINUM_REPO="${NOMINUM_REPO:-$HOME/AndroidStudioProjects/Nominum-Render}"
NOMINUM_COMMIT="${NOMINUM_COMMIT:-d398993}"
nominum="$B/nominum-$NOMINUM_COMMIT"
if [ ! -e "$nominum/core/vif1.cpp" ]; then
    mkdir -p "$nominum"
    git -C "$NOMINUM_REPO" archive "$NOMINUM_COMMIT" | tar -x -C "$nominum"
fi
raylib="$B/PS2Recomp/build-rt/_deps/raylib-src"
[ -e "$raylib/src/raylib.h" ] || { echo "no raylib source at $raylib (build the PC runner first: recomp/build.sh)" >&2; exit 1; }
if [ ! -e "$B/pc/build.ninja" ]; then
    cmake -S "$here/recomp/pc" -B "$B/pc" -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ \
        -DPS2R="$B/PS2Recomp" -DRAYLIB="$raylib" -DHL_NOMINUM_DIR="$nominum"
fi
cmake --build "$B/pc" --target un5pc -j"${JOBS:-$(nproc)}"
echo "$B/pc/un5pc"
