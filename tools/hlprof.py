#!/usr/bin/env python3
"""hlprof.py: the samples of HL_PROF=1 (recomp/runtime/hl_prof.cpp, hl_prof.txt) as functions.

  hlprof.py hl_prof.txt [--top N] [--tid TID] [--addr2line PATH] [--lib DIR] [--callers TEXT]

Each line of hl_prof.txt is "module offset tid count". The offsets go through addr2line -f -C on their module (for the
device, --lib points at the unstripped libraries, e.g. build/android, and --addr2line at the NDK's llvm-addr2line).
Prints the threads by samples, then the functions by samples (self time), overall or for one thread.
--callers TEXT (arm64, hl_prof.txt.lr): for the sampled functions whose name holds TEXT, the functions in the link register
(the caller, for a leaf function such as memcpy or malloc).
"""
import argparse
import collections
import os
import subprocess


def symbolize(module, offsets, addr2line, libdir):
    path = module
    if libdir:
        cand = os.path.join(libdir, os.path.basename(module))
        if os.path.exists(cand):
            path = cand
    if not os.path.exists(path):
        return {o: os.path.basename(module) for o in offsets}
    out = {}
    offs = sorted(offsets)
    for i in range(0, len(offs), 4000):
        chunk = offs[i:i + 4000]
        res = subprocess.run([addr2line, "-f", "-C", "-e", path] + [hex(o) for o in chunk],
                             capture_output=True, text=True).stdout.splitlines()
        for j, o in enumerate(chunk):
            name = res[2 * j] if 2 * j < len(res) else "?"
            out[o] = name if name != "??" else f"{os.path.basename(module)}+{o:#x}"
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--tid", type=int)
    ap.add_argument("--addr2line", default="addr2line")
    ap.add_argument("--lib")
    ap.add_argument("--callers")
    a = ap.parse_args()
    if a.callers:
        return callers(a)
    rows = []
    for line in open(a.file):
        if line.startswith("#"):
            continue
        mod, off, tid, cnt = line.split()
        rows.append((mod, int(off, 16), int(tid), int(cnt)))
    total = sum(r[3] for r in rows)
    bytid = collections.Counter()
    for r in rows:
        bytid[r[2]] += r[3]
    print(f"{total} samples")
    print("-- threads")
    for tid, c in bytid.most_common(12):
        print(f"{c:8d} {100.0 * c / total:5.1f}%  tid {tid}")
    if a.tid is not None:
        rows = [r for r in rows if r[2] == a.tid]
    names = {}
    bymod = collections.defaultdict(set)
    for r in rows:
        bymod[r[0]].add(r[1])
    for mod, offs in bymod.items():
        for o, n in symbolize(mod, offs, a.addr2line, a.lib).items():
            names[(mod, o)] = n
    fn = collections.Counter()
    for r in rows:
        fn[names[(r[0], r[1])]] += r[3]
    sub = sum(fn.values())
    print(f"-- functions ({'tid ' + str(a.tid) if a.tid is not None else 'all threads'}, {sub} samples)")
    for n, c in fn.most_common(a.top):
        print(f"{c:8d} {100.0 * c / max(sub, 1):5.1f}%  {n[:150]}")


def callers(a):
    rows = []
    for line in open(a.file + ".lr"):
        pm, po, lm, lo, tid, cnt = line.split()
        if a.tid is None or int(tid) == a.tid:
            rows.append((pm, int(po, 16), lm, int(lo, 16), int(cnt)))
    bymod = collections.defaultdict(set)
    for r in rows:
        bymod[r[0]].add(r[1])
        bymod[r[2]].add(r[3])
    names = {}
    for mod, offs in bymod.items():
        for o, n in symbolize(mod, offs, a.addr2line, a.lib).items():
            names[(mod, o)] = n
    pairs = collections.Counter()
    for r in rows:
        f = names[(r[0], r[1])]
        if a.callers in f:
            pairs[(f[:60], names[(r[2], r[3])][:110])] += r[4]
    total = sum(pairs.values())
    print(f"-- callers of '{a.callers}' ({total} samples)")
    for (f, c), n in pairs.most_common(a.top):
        print(f"{n:7d} {100.0 * n / max(total, 1):5.1f}%  {f}  <-  {c}")


if __name__ == "__main__":
    main()
