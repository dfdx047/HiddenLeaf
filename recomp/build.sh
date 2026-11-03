#!/usr/bin/env bash
# Hidden Leaf: from the player's European ISO to the recompiled game, on a Linux PC.
#
#   recomp/build.sh <Naruto Shippuden - Ultimate Ninja 5 (Europe).iso> [--run]
#   recomp/build.sh --mod <a mod's ISO>     (after the base: the mod's code, for recomp/android/build.sh --mod <id>)
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
# libxcursor libxi mesa alsa-lib ffmpeg; FFmpeg decodes the movies).
set -euo pipefail

PS2RECOMP_URL=https://github.com/ran-j/PS2Recomp
PS2RECOMP_COMMIT=e563698   # tested 2026-10-25

here=$(cd "$(dirname "$0")/.." && pwd)
iso=${1:?usage: recomp/build.sh <European ISO> [--run] | --mod <mod ISO>}
run=${2:-}
B="$here/build"
mkdir -p "$B"
jobs=$(nproc)

say() { printf '\n== %s\n' "$*"; }

# --mod: a mod that changes the code (its program or the overlays of PRG/). The mod's disc goes to build/mods/<id>/disc, and
# steps 3, 5 and 6 run on it, into build/mods/<id>/gen and a copy of the runtime's sources (build/mods/<id>/PS2Recomp, with
# the base's patches): recomp/android/build.sh --mod <id> makes libun5_<id>.so from it. The tools are the base's.
if [ "$iso" = "--mod" ]; then
    mod_iso=${2:?usage: recomp/build.sh --mod <mod ISO>}
    [ -x "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" ] || { echo "build the base first: recomp/build.sh <European ISO>" >&2; exit 1; }
    tmp="$B/mods/.new"
    rm -rf "$tmp"; mkdir -p "$tmp"
    say "1. the mod's disc"
    7z x -y -o"$tmp" "$mod_iso" >/dev/null
    boot=$(sed -n 's/^BOOT2 *= *cdrom0:\\\([^;]*\);.*/\1/p' "$tmp/SYSTEM.CNF" | tr -d '\r')
    sha=$(sha256sum "$tmp/$boot" | cut -d' ' -f1)
    id=$(python3 -c "import sys; sys.path.insert(0, '$here/tools'); import un5; print(un5.KNOWN_MODS.get('$sha', ('',))[0])")
    [ -n "$id" ] || { echo "unknown mod program $boot (sha256 $sha): add it to KNOWN_MODS and CODE_RANGES in tools/un5.py" >&2; exit 1; }
    M="$B/mods/$id"
    rm -rf "$M/disc"; mkdir -p "$M"; mv "$tmp" "$M/disc"
    echo "$id: $boot"
    say "3. the ELF for the recompiler"
    mkdir -p "$M/gen"
    python3 "$here/tools/un5.py" prepelf "$M/disc/$boot" "$M/gen/un5_split.elf"
    say "4. the runtime's sources"
    rsync -a --delete --exclude /build-tools/ --exclude /build-rt/ --exclude /.git/ "$B/PS2Recomp/" "$M/PS2Recomp/"
    B_DISC="$M/disc"; B_GEN="$M/gen"; RT="$M/PS2Recomp/ps2xRuntime"
fi
B_DISC=${B_DISC:-$B/disc}; B_GEN=${B_GEN:-$B/gen}; RT=${RT:-$B/PS2Recomp/ps2xRuntime}
if [ "$iso" != "--mod" ]; then

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
# checkout -f keeps untracked files, and a file a patch creates would then already exist (the build dirs stay)
git -C "$B/PS2Recomp" clean -fdq -e /build-tools/ -e /build-rt/
for p in "$here"/recomp/patches/*.patch; do
    git -C "$B/PS2Recomp" apply "$p" || { echo "patch failed: $(basename "$p")" >&2; exit 1; }
    echo "patch: $(basename "$p")"
done
if [ ! -f "$B/PS2Recomp/build-tools/build.ninja" ]; then
    cmake -S "$B/PS2Recomp" -B "$B/PS2Recomp/build-tools" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DPS2X_BUILD_RUNTIME=OFF -DPS2X_BUILD_TEST=OFF -DPS2X_BUILD_STUDIO=OFF
fi
# always: a patch to the recompiler must reach the generated code (ninja rebuilds only what changed)
cmake --build "$B/PS2Recomp/build-tools" -j"$jobs"
fi

say "5. analyzer and recompiler"
cd "$B_GEN"
"$B/PS2Recomp/build-tools/ps2xAnalyzer/ps2_analyzer" un5_split.elf un5_config.toml > analyzer.log 2>&1
sed -i 's|^output = .*|output = "./output/"|' un5_config.toml
python3 "$here/tools/un5.py" stubfix un5_config.toml
rm -rf output
"$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" un5_config.toml > recomp.log 2>&1
# second pass: plus the functions only reached through pointers in the data (constructors, vtables, callbacks)
# ... and the ones only the overlays call
for bin in "$B_DISC"/PRG/*.BIN; do
    head -c 4 "$bin" | grep -q MWo3 && python3 "$here/tools/un5.py" ovlelf "$bin" "ovl_$(basename "$bin" .BIN).elf" >/dev/null
done
python3 "$here/tools/un5.py" fnmap un5_split.elf output un5_map.csv --also ovl_*.elf
rm -f ovl_*.elf
sed -i 's|^ghidra_output = .*|ghidra_output = "un5_map.csv"|' un5_config.toml
rm -rf output
"$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" un5_config.toml > recomp.log 2>&1
grep -E "Functions processed|Generated functions" recomp.log
errors=$(grep -c "\[error\]" recomp.log || true)
echo "recompiler errors: $errors"

say "6. the overlays"
rm -rf "$B_GEN/ovl"
mkdir -p "$B_GEN/ovl/modules"
names=()
# every overlay's ELF first: one calls another (the A35's BTL.BIN calls into MOD.BIN), fnmap skips those that overlap
for bin in "$B_DISC"/PRG/*.BIN; do
    n=$(basename "$bin" .BIN | tr 'A-Z' 'a-z')
    head -c 4 "$bin" | grep -q MWo3 || continue
    mkdir -p "$B_GEN/ovl/$n"
    python3 "$here/tools/un5.py" ovlelf "$bin" "$B_GEN/ovl/$n/$n.elf" >/dev/null
done
# again, with the code the others jump or call into (an overlay that keeps its code in its data: the A35's MOD.BIN)
for bin in "$B_DISC"/PRG/*.BIN; do
    n=$(basename "$bin" .BIN | tr 'A-Z' 'a-z')
    head -c 4 "$bin" | grep -q MWo3 || continue
    others=(); for e in "$B_GEN"/ovl/*/*.elf; do [ "$e" = "$B_GEN/ovl/$n/$n.elf" ] || others+=("$e"); done
    python3 "$here/tools/un5.py" ovlelf "$bin" "$B_GEN/ovl/$n/$n.elf" --from "$B_GEN/un5_split.elf" "${others[@]}" >/dev/null
done
for bin in "$B_DISC"/PRG/*.BIN; do
    n=$(basename "$bin" .BIN | tr 'A-Z' 'a-z')
    head -c 4 "$bin" | grep -q MWo3 || continue
    d="$B_GEN/ovl/$n"
    ( cd "$d"
      "$B/PS2Recomp/build-tools/ps2xAnalyzer/ps2_analyzer" "$n.elf" cfg.toml > analyzer.log 2>&1
      sed -i 's|^output = .*|output = "./output/"|' cfg.toml
      "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" cfg.toml > recomp.log 2>&1
      others=(); for e in "$B_GEN"/ovl/*/*.elf; do [ "$e" = "$d/$n.elf" ] || others+=("$e"); done
      python3 "$here/tools/un5.py" fnmap "$n.elf" output map.csv --also "$B_GEN/un5_split.elf" "${others[@]}" >/dev/null
      sed -i 's|^ghidra_output = .*|ghidra_output = "map.csv"|' cfg.toml
      rm -rf output
      "$B/PS2Recomp/build-tools/ps2xRecomp/ps2_recomp" cfg.toml > recomp.log 2>&1
      echo "$n: $(grep -E 'Functions processed' recomp.log), errors: $(grep -c '\[error\]' recomp.log || true)" )
    python3 "$here/recomp/overlays.py" "$n" "$bin" "$d/output" "$B_GEN/ovl/modules" >/dev/null
    names+=("$n")
done
python3 "$here/recomp/overlays.py" --list "$B_GEN/ovl/modules" "${names[@]}"

say "7. the runtime"
rt="$RT"
# the generated code goes where the runtime's build looks for it (runner/ and include/)
find "$rt/src/runner" \( -name '*.cpp' -o -name '*.h' -o -name '*.inc' \) ! -name 'main.cpp' -delete
cp output/*.cpp "$rt/src/runner/"
cp output/*.h "$rt/include/"
cp "$B_GEN"/ovl/modules/* "$rt/src/runner/"
cp "$here"/recomp/runtime/*.cpp "$rt/src/runner/"
if [ "$iso" = "--mod" ]; then
    echo "next: recomp/android/build.sh --mod $id"
    exit 0
fi
# link-time optimization only with HL_LTO=1: the LTO link of the whole game takes long and a lot of memory
no_lto=ON; [ "${HL_LTO:-0}" = "1" ] && no_lto=OFF
cmake -S "$B/PS2Recomp" -B "$B/PS2Recomp/build-rt" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DPS2X_BUILD_RECOMP=OFF -DPS2X_BUILD_ANALYZER=OFF -DPS2X_BUILD_TEST=OFF -DPS2X_BUILD_STUDIO=OFF \
    -DPS2X_ENABLE_AGRESSIVE_LOGS=OFF -DPS2X_ENABLE_FFMPEG=ON -DPS2X_DISABLE_IPO="$no_lto" \
    -DCMAKE_CXX_FLAGS="-march=native" -DCMAKE_C_FLAGS="-march=native"
cmake --build "$B/PS2Recomp/build-rt" -j"$jobs"
runner=$(find "$B/PS2Recomp/build-rt" -name ps2EntryRunner -type f | head -1)
echo "runner: $runner"

if [ "$run" = "--run" ]; then
    say "8. running (log: build/run.log)"
    cd "$B/disc"
    "$runner" "$B/disc/SLES_556.05" 2>&1 | tee "$B/run.log"
fi
