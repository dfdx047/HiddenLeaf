#!/usr/bin/env bash
# Hidden Leaf: from the player's European ISO to the recompiled game, on a Linux PC.
#
#   recomp/build.sh <Naruto Shippuden - Ultimate Ninja 5 (Europe).iso> [--run]
#
# Steps (everything goes into build/, which git ignores; nothing of the game enters the repository):
#   1. checks the ISO (tools/un5.py verify): the unmodified European release, SLES-55605
#   2. extracts the disc to build/disc/ (the runtime reads cdrom0:\ from the folder of the ELF)
#   3. the ELF for the recompiler: its one big section split into code and data (tools/un5.py prepelf)
#   4. PS2Recomp at the pinned commit (build/PS2Recomp), its recompiler and analyzer
#   5. the analyzer's config, then the recompiler twice: the second time with the functions only reached through
#      pointers added (tools/un5.py fnmap): build/gen/output (C++, one file per function)
#   6. the overlays of PRG/ (fights, adventure, menus, texts), each recompiled on its own and renamed into a module
#      the runtime finds by what is in memory (recomp/overlays.py, recomp/runtime/un5_overlays.cpp)
#   7. the runtime (PS2Recomp + recomp/patches) with the generated code: build/PS2Recomp/build-rt/.../ps2EntryRunner
#   8. --run: starts it with build/disc/SLES_556.05
#
# Needs: git, cmake (3.21+), ninja, a C++20 compiler (GCC 13+ or Clang 17+), python3, 7z; for the window, the X11/GL
# development headers raylib uses (Arch/CachyOS: base-devel cmake ninja p7zip libx11 libxrandr libxinerama
# libxcursor libxi mesa alsa-lib).
set -euo pipefail

PS2RECOMP_URL=https://github.com/ran-j/PS2Recomp
PS2RECOMP_COMMIT=e563698   # tested 2026-10-25

here=$(cd "$(dirname "$0")/.." && pwd)
iso=${1:?usage: recomp/build.sh <European ISO> [--run]}
run=${2:-}
B="$here/build"
mkdir -p "$B"
jobs=$(nproc)

say() { printf '\n== %s\n' "$*"; }

say "1. checking the ISO"
python3 "$here/tools/un5.py" verify "$iso" || { echo "not the unmodified European ISO (SLES-55605): see above"; exit 1; }

say "2. extracting the disc to build/disc"
if [ ! -f "$B/disc/SLES_556.05" ]; then
    rm -rf "$B/disc"
    7z x -y -o"$B/disc" "$iso" >/dev/null
fi
ls "$B/disc"

say "3. the ELF for the recompiler"
mkdir -p "$B/gen"
python3 "$here/tools/un5.py" prepelf "$B/disc/SLES_556.05" "$B/gen/un5_split.elf"

say "4. PS2Recomp $PS2RECOMP_COMMIT"
if [ ! -d "$B/PS2Recomp/.git" ]; then
    git clone "$PS2RECOMP_URL" "$B/PS2Recomp"
fi
git -C "$B/PS2Recomp" fetch -q origin
git -C "$B/PS2Recomp" checkout -q -f "$PS2RECOMP_COMMIT"
for p in "$here"/recomp/patches/*.patch; do
    git -C "$B/PS2Recomp" apply "$p" && echo "patch: $(basename "$p")"
done
if [ ! -x "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" ]; then
    cmake -S "$B/PS2Recomp" -B "$B/PS2Recomp/build-tools" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DPS2X_BUILD_RUNTIME=OFF -DPS2X_BUILD_TEST=OFF -DPS2X_BUILD_STUDIO=OFF
    cmake --build "$B/PS2Recomp/build-tools" -j"$jobs"
fi

say "5. analyzer and recompiler"
cd "$B/gen"
"$B/PS2Recomp/build-tools/ps2xAnalyzer/ps2_analyzer" un5_split.elf un5_config.toml > analyzer.log 2>&1
sed -i 's|^output = .*|output = "./output/"|' un5_config.toml
python3 "$here/tools/un5.py" stubfix un5_config.toml
rm -rf output
"$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" un5_config.toml > recomp.log 2>&1
# second pass: plus the functions only reached through pointers in the data (constructors, vtables, callbacks)
python3 "$here/tools/un5.py" fnmap un5_split.elf output un5_map.csv
sed -i 's|^ghidra_output = .*|ghidra_output = "un5_map.csv"|' un5_config.toml
rm -rf output
"$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" un5_config.toml > recomp.log 2>&1
grep -E "Functions processed|Generated functions" recomp.log
errors=$(grep -c "\[error\]" recomp.log || true)
echo "recompiler errors: $errors"

say "6. the overlays"
rm -rf "$B/gen/ovl"
mkdir -p "$B/gen/ovl/modules"
names=()
for bin in "$B"/disc/PRG/*.BIN; do
    n=$(basename "$bin" .BIN | tr 'A-Z' 'a-z')
    head -c 4 "$bin" | grep -q MWo3 || continue
    d="$B/gen/ovl/$n"
    mkdir -p "$d"
    python3 "$here/tools/un5.py" ovlelf "$bin" "$d/$n.elf" >/dev/null
    ( cd "$d"
      "$B/PS2Recomp/build-tools/ps2xAnalyzer/ps2_analyzer" "$n.elf" cfg.toml > analyzer.log 2>&1
      sed -i 's|^output = .*|output = "./output/"|' cfg.toml
      "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" cfg.toml > recomp.log 2>&1
      python3 "$here/tools/un5.py" fnmap "$n.elf" output map.csv >/dev/null
      sed -i 's|^ghidra_output = .*|ghidra_output = "map.csv"|' cfg.toml
      rm -rf output
      "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" cfg.toml > recomp.log 2>&1
      echo "$n: $(grep -E 'Functions processed' recomp.log), errors: $(grep -c '\[error\]' recomp.log || true)" )
    python3 "$here/recomp/overlays.py" "$n" "$bin" "$d/output" "$B/gen/ovl/modules" >/dev/null
    names+=("$n")
done
python3 "$here/recomp/overlays.py" --list "$B/gen/ovl/modules" "${names[@]}"

say "7. the runtime"
rt="$B/PS2Recomp/ps2xRuntime"
# the generated code goes where the runtime's build looks for it (runner/ and include/)
find "$rt/src/runner" \( -name '*.cpp' -o -name '*.h' -o -name '*.inc' \) ! -name 'main.cpp' -delete
cp output/*.cpp "$rt/src/runner/"
cp output/*.h "$rt/include/"
cp "$B"/gen/ovl/modules/* "$rt/src/runner/"
cp "$here"/recomp/runtime/*.cpp "$rt/src/runner/"
# link-time optimization only with HL_LTO=1: the LTO link of the whole game takes long and a lot of memory
no_lto=ON; [ "${HL_LTO:-0}" = "1" ] && no_lto=OFF
cmake -S "$B/PS2Recomp" -B "$B/PS2Recomp/build-rt" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DPS2X_BUILD_RECOMP=OFF -DPS2X_BUILD_ANALYZER=OFF -DPS2X_BUILD_TEST=OFF -DPS2X_BUILD_STUDIO=OFF \
    -DPS2X_ENABLE_AGRESSIVE_LOGS=OFF -DPS2X_ENABLE_FFMPEG=OFF -DPS2X_DISABLE_IPO="$no_lto" \
    -DCMAKE_CXX_FLAGS="-march=native" -DCMAKE_C_FLAGS="-march=native"
cmake --build "$B/PS2Recomp/build-rt" -j"$jobs"
runner=$(find "$B/PS2Recomp/build-rt" -name ps2EntryRunner -type f | head -1)
echo "runner: $runner"

if [ "$run" = "--run" ]; then
    say "8. running (log: build/run.log)"
    cd "$B/disc"
    "$runner" "$B/disc/SLES_556.05" 2>&1 | tee "$B/run.log"
fi
