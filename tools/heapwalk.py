#!/usr/bin/env python3
"""
Hidden Leaf: the game's own heap in two RAM dumps (32 MB, e.g. HLB_RAM=1 of the PCSX2 bench and of the PC recomp), block
by block. The pool starts at the ELF's end + 0x10 (0x928910 for the European ELF); each block's header is +0 the block
before it, +4 the one after, +8 the size, +0xC a tag.

  heapwalk.py <pcsx2_ram.bin> <recomp_ram.bin> [pool start, default 0x928910]

Prints the first block that differs (with the blocks around it) and the regions that differ, each with the sizes and
tags of their first blocks on both sides.
"""
import struct
import sys


def walk(m, start, limit=400000):
    out, a, seen = [], start, set()
    while a and a not in seen and len(out) < limit and a + 16 <= len(m):
        seen.add(a)
        prev, nxt, size, tag = struct.unpack_from('<4I', m, a)
        out.append((a, size, tag, prev))
        a = nxt & 0x1FFFFFF
    return out


def main():
    p = open(sys.argv[1], 'rb').read()
    r = open(sys.argv[2], 'rb').read()
    start = int(sys.argv[3], 0) if len(sys.argv) > 3 else 0x928910
    wp, wr = walk(p, start), walk(r, start)
    print('blocks: pcsx2 %d, recomp %d' % (len(wp), len(wr)))
    for i, (x, y) in enumerate(zip(wp, wr)):
        if x[:3] != y[:3]:
            print('first difference at block %d' % i)
            for k in range(max(0, i - 4), min(i + 8, len(wp), len(wr))):
                a, b = wp[k], wr[k]
                print('%6d pcsx2 %08x size %8x tag %8x | recomp %08x size %8x tag %8x %s' % (
                    k, a[0], a[1], a[2], b[0], b[1], b[2], '' if a[:3] == b[:3] else '<--'))
            break
    else:
        print('the same chain')
        return
    mp = {b[0]: b for b in wp}
    mr = {b[0]: b for b in wr}
    addrs = sorted(set(mp) | set(mr))
    regions, cur = [], None
    for a in addrs:
        if a in mp and a in mr and mp[a][1:3] == mr[a][1:3]:
            continue
        if cur and a - cur[1] < 0x400:
            cur[1] = a
            cur[2] += 1
        else:
            cur = [a, a, 1]
            regions.append(cur)
    print('%d regions differ' % len(regions))
    for s, e, n in regions[:40]:
        ps = [mp[a] for a in addrs if s <= a <= e and a in mp]
        rs = [mr[a] for a in addrs if s <= a <= e and a in mr]
        print('%08x-%08x %4d blocks | pcsx2 %s | recomp %s' % (
            s, e, n, ' '.join('%x:%x' % (b[1], b[2] & 0xFFFF) for b in ps[:6]),
            ' '.join('%x:%x' % (b[1], b[2] & 0xFFFF) for b in rs[:6])))


main()
