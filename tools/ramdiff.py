#!/usr/bin/env python3
"""Hidden Leaf: compares the game's structures in two dumps of the EE RAM (32 MB), one from PCSX2 and one from the
recompiled runtime, taken at the same point of the game.

The two heaps are not laid out alike (blocks land at other addresses), so the dumps are not compared address by
address: the walk starts at a pair of roots (a global, or a pointer in one) and follows the pointers of both sides in
step. A word that is a pointer on both sides is "the same" and its targets are compared in turn; any other word is
compared as it is (floats with a small tolerance). The size of a block comes from the game's heap header (16 bytes
before the block: prev, next, size with the header, tag; allocator 0x1176c0), or a default.

  ramdiff.py <pcsx2 eeMemory.bin> <recomp ram_*.bin> [roots...] [--depth N] [--max N] [--size HEX]

A root is ADDR (the same global on both sides), *ADDR (the pointer stored at ADDR on each side), ADDR_P,ADDR_R (two
addresses), or a name of NAMED below; each /OFF after it goes to the pointer at +OFF on each side ("title/30" is the
object whose pointer is at title+0x30). Without roots it compares NAMED. The PCSX2 RAM is the eeMemory.bin inside the
savestate (.p2s is a zip; `7z x state.p2s eeMemory.bin`).
"""
import argparse
import math
import struct
import sys

RAM = 32 << 20
HEAP_LO = 0x6c0000  # where the heap blocks are (the arena starts after the ELF's bss)

# the title screen's structures (docs/plano.md, "Tela de título")
NAMED = {
    "title": "*0x617cc4",        # the title object, *(gp-0x22c), 0x48 bytes (0x1e4480)
    "title-objs": "title/1c",    # its scene objects: world matrix +0x00 (0x19fec0), local +0x40, parent +0x80
    "jumptable": "0x5c78b0",     # the switch of 0x1e4790 (7 states): code addresses, same on both sides
    "gfx": "*0x617afc",          # *(gp-0x3f4): drawing state, +0x1c0 the scratchpad stack of the matrix code
    "draw": "*0x617af4",         # *(gp-0x3fc), read by the scene draw 0x1bff50
    "draw2": "*0x617ad4",        # *(gp-0x41c), idem (+0xc8: the object being drawn)
    "mcman": "*0x617cf4",        # the memory card manager, *(gp-0x1fc)
}


def u32(m, a):
    return struct.unpack_from("<I", m, a & (RAM - 1))[0]


def is_ptr(v):
    return 0x100000 <= v < RAM and v & 3 == 0


def block_size(m, a):
    """The heap block's size, if a is the start of one (header: prev, next, size, tag)."""
    if not HEAP_LO <= a < RAM or a < 16:
        return None
    prev, nxt, size = u32(m, a - 16), u32(m, a - 12), u32(m, a - 8)
    if 0x10 < size <= 0x400000 and size & 0xf == 0 and is_ptr(prev) and is_ptr(nxt):
        return size - 16  # the size counts the header
    return None


def as_float(v):
    return struct.unpack("<f", struct.pack("<I", v))[0]


def same_float(a, b):
    """Both look like ordinary floats and are close."""
    ea, eb = (a >> 23) & 0xff, (b >> 23) & 0xff
    if not (0x60 < ea < 0x9f and 0x60 < eb < 0x9f):
        return False
    fa, fb = as_float(a), as_float(b)
    return math.isclose(fa, fb, rel_tol=1e-3, abs_tol=1e-6)


def resolve(root, mp, mr):
    base, *steps = root.split("/")
    if base in NAMED:
        pp, pr = resolve(NAMED[base], mp, mr)
    elif base.startswith("*"):
        a = int(base[1:], 16)
        pp, pr = u32(mp, a), u32(mr, a)
    elif "," in base:
        p, r = base.split(",")
        pp, pr = int(p, 16), int(r, 16)
    else:
        pp = pr = int(base, 16)
    for off in steps:
        pp, pr = u32(mp, pp + int(off, 16)), u32(mr, pr + int(off, 16))
    return pp, pr


def walk(mp, mr, name, pp, pr, depth, limit, default_size, out):
    queue = [(name, pp, pr, 0)]
    seen = set()
    n = 0
    while queue and n < limit:
        path, ap, ar, d = queue.pop(0)
        if (ap, ar) in seen or not (is_ptr(ap) and is_ptr(ar)):
            continue
        seen.add((ap, ar))
        sp, sr = block_size(mp, ap), block_size(mr, ar)
        size = sp or sr or default_size
        if sp and sr and sp != sr:
            out.append(f"{path}  P {ap:08x} / R {ar:08x}: block size {sp:#x} vs {sr:#x}")
            n += 1
            size = min(sp, sr)
        for off in range(0, size, 4):
            vp, vr = u32(mp, ap + off), u32(mr, ar + off)
            if vp == vr:
                if is_ptr(vp) and d + 1 <= depth:
                    queue.append((f"{path}+{off:x}", vp, vr, d + 1))
                continue
            if is_ptr(vp) and is_ptr(vr):
                # a pointer on both sides: follow it (heap blocks only; a static address must be the same)
                if (vp < HEAP_LO) != (vr < HEAP_LO):
                    out.append(f"{path}+{off:x}  P {ap + off:08x} / R {ar + off:08x}: {vp:08x} vs {vr:08x} (static vs heap)")
                    n += 1
                elif d + 1 <= depth:
                    queue.append((f"{path}+{off:x}", vp, vr, d + 1))
                continue
            if same_float(vp, vr):
                continue
            fp, fr = as_float(vp), as_float(vr)
            out.append(f"{path}+{off:x}  P {ap + off:08x} / R {ar + off:08x}: {vp:08x} vs {vr:08x}  ({fp:.6g} vs {fr:.6g})")
            n += 1
    return n


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pcsx2")
    ap.add_argument("recomp")
    ap.add_argument("roots", nargs="*")
    ap.add_argument("--depth", type=int, default=3)
    ap.add_argument("--max", type=int, default=200)
    ap.add_argument("--size", type=lambda s: int(s, 16), default=0x40, help="size of a block without heap header (hex)")
    a = ap.parse_args()
    mp, mr = open(a.pcsx2, "rb").read(), open(a.recomp, "rb").read()
    if len(mp) != RAM or len(mr) != RAM:
        sys.exit("both dumps must be 32 MB of EE RAM")
    for root in a.roots or list(NAMED):
        pp, pr = resolve(root, mp, mr)
        out = []
        walk(mp, mr, root, pp, pr, a.depth, a.max, a.size, out)
        print(f"== {root}: P {pp:08x} / R {pr:08x}, {len(out)} differences")
        for line in out:
            print("  " + line)


if __name__ == "__main__":
    main()
