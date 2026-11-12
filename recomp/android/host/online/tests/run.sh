#!/usr/bin/env bash
# The online match's tests on the PC (no game, no phone): the lockstep and the settings' conversation over simulated lossy lines, and
# two whole matches over the loopback and over a stand-in for the room's relay. C++17, with the address and undefined-behaviour
# sanitizers. Run: recomp/android/host/online/tests/run.sh   (the programs go to $OUT, default a temporary folder)
set -euo pipefail
here=$(cd "$(dirname "$0")/.." && pwd)
OUT=${OUT:-$(mktemp -d)}
CXX=${CXX:-g++}
flags=(-std=c++17 -O1 -g -Wall -Wextra -fsanitize=address,undefined -fno-sanitize-recover=undefined -I "$here")
"$CXX" "${flags[@]}" "$here/tests/lockstep_tests.cpp" "$here/lockstep.cpp" -o "$OUT/lockstep_tests"
"$CXX" "${flags[@]}" "$here/tests/session_tests.cpp" "$here/session.cpp" -o "$OUT/session_tests"
"$CXX" "${flags[@]}" "$here/tests/match_tests.cpp" "$here/match_session.cpp" "$here/link.cpp" "$here/stun.cpp" "$here/session.cpp" "$here/lockstep.cpp" \
    -o "$OUT/match_tests" -pthread
for t in lockstep_tests session_tests match_tests; do
    echo "== $t"
    "$OUT/$t"
done
