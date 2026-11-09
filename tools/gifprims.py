#!/usr/bin/env python3
"""
Hidden Leaf: the primitives of an hl_gif.bin (HL_GIF_LOG=N@S on the PC, or a .gif of HL_VIF_DUMP), for the 2D widescreen
of the menus: one line per primitive with its type, context, frame buffer (FBP, FBW), scissor, texture (TBP0) and the
window x/y span of its vertices.

  gifprims.py <hl_gif.bin> [first packet] [packets]
  gifprims.py <hl_gif.bin> runs [first] [packets]   the same, runs of primitives with the same frame, texture and type merged
"""
import struct, sys

NVERT = {0: 1, 1: 2, 2: 2, 3: 3, 4: 3, 5: 3, 6: 2, 7: 0}
PRIMN = {0: 'point', 1: 'line', 2: 'lstrip', 3: 'tri', 4: 'tstrip', 5: 'tfan', 6: 'sprite', 7: '?'}


class Gs:
    def __init__(self):
        self.prim = 0
        self.prmodecont = 1
        self.prmode = 0
        self.ofs = [(0, 0), (0, 0)]
        self.sc = [(0, 0, 0, 0), (0, 0, 0, 0)]
        self.frame = [(0, 0), (0, 0)]
        self.tex = [0, 0]
        self.verts = []
        self.out = []

    def ctxt(self):
        attr = self.prim if self.prmodecont else self.prmode
        return (attr >> 9) & 1

    def reg(self, a, v):
        if a == 0x00:
            self.prim = v & 0x7FF
            self.verts = []
        elif a in (0x04, 0x05, 0x0C, 0x0D):
            x = v & 0xFFFF
            y = (v >> 16) & 0xFFFF
            self.vertex(x, y, a in (0x04, 0x05))
        elif a == 0x18 or a == 0x19:
            self.ofs[a - 0x18] = (v & 0xFFFF, (v >> 32) & 0xFFFF)
        elif a == 0x1A:
            self.prmodecont = v & 1
        elif a == 0x1B:
            self.prmode = v & 0x7FF
        elif a == 0x40 or a == 0x41:
            self.sc[a - 0x40] = (v & 0x7FF, (v >> 16) & 0x7FF, (v >> 32) & 0x7FF, (v >> 48) & 0x7FF)
        elif a == 0x4C or a == 0x4D:
            self.frame[a - 0x4C] = (v & 0x1FF, (v >> 16) & 0x3F)
        elif a == 0x06 or a == 0x07:
            self.tex[a - 0x06] = v & 0x3FFF

    def vertex(self, x, y, kick):
        c = self.ctxt()
        ox, oy = self.ofs[c]
        self.verts.append(((x - ox) / 16.0, (y - oy) / 16.0))
        t = self.prim & 7
        n = NVERT.get(t, 0)
        if not kick or n == 0 or len(self.verts) < n:
            return
        vs = self.verts[-n:]
        xs = [v[0] for v in vs]
        ys = [v[1] for v in vs]
        tme = (self.prim >> 4) & 1
        self.out.append((PRIMN[t], c, self.frame[c], self.sc[c], self.tex[c] if tme else -1, min(xs), max(xs), min(ys), max(ys)))
        if t in (6, 3, 0, 1):
            self.verts = []
        elif t == 5:
            self.verts = [self.verts[0]] + self.verts[-1:]


def parse(data, gs):
    i = 0
    n = len(data)
    while i + 16 <= n:
        lo, hi = struct.unpack_from('<QQ', data, i)
        i += 16
        nloop = lo & 0x7FFF
        pre = (lo >> 46) & 1
        prim = (lo >> 47) & 0x7FF
        flg = (lo >> 58) & 3
        nreg = (lo >> 60) & 0xF or 16
        regs = [(hi >> (4 * k)) & 0xF for k in range(nreg)]
        if pre and flg == 0:
            gs.reg(0x00, prim)
        if flg == 0:
            for _ in range(nloop):
                for r in regs:
                    if i + 16 > n:
                        return
                    a, b = struct.unpack_from('<QQ', data, i)
                    i += 16
                    if r == 0x0:
                        gs.reg(0x00, a)
                    elif r in (0x4, 0x5, 0xC, 0xD):
                        x = a & 0xFFFF
                        y = (a >> 32) & 0xFFFF
                        kick = r in (0x4, 0x5) and not ((b >> 47) & 1)
                        gs.vertex(x, y, kick)
                    elif r == 0xE:
                        gs.reg(b & 0xFF, a)
        elif flg == 1:
            total = nloop * nreg
            for k in range(total):
                if i + 8 > n:
                    return
                (v,) = struct.unpack_from('<Q', data, i)
                i += 8
                gs.reg(regs[k % nreg], v)
            if total & 1:
                i += 8
        else:
            i += nloop * 16


def main():
    path = sys.argv[1]
    args = sys.argv[2:]
    runs = bool(args) and args[0] == 'runs'
    if runs:
        args = args[1:]
    first = int(args[0]) if args else 0
    count = int(args[1]) if len(args) > 1 else 200
    gs = Gs()
    with open(path, 'rb') as f:
        k = 0
        while True:
            h = f.read(8)
            if len(h) < 8:
                break
            p, size = struct.unpack('<II', h)
            data = f.read(size)
            if k >= first + count:
                break
            if k >= first:
                if p == 0xFF:
                    gs.out.append(('--vsync', k))
                elif p == 0xFE:
                    pass
                else:
                    gs.out.append(('--packet', k, p, size))
                    parse(data, gs)
            k += 1
    last = None
    for o in gs.out:
        if o[0] == '--vsync':
            if last:
                show(last)
                last = None
            print('---- vsync (packet %d)' % o[1])
            continue
        if o[0] == '--packet':
            if not runs:
                print('packet %d path %d %d bytes' % o[1:])
            continue
        t, c, fr, sc, tex, x0, x1, y0, y1 = o
        if runs:
            key = (t, fr, tex, sc)
            if last and last[0] == key:
                last[1] += 1
                last[2] = min(last[2], x0); last[3] = max(last[3], x1); last[4] = min(last[4], y0); last[5] = max(last[5], y1)
                continue
            if last:
                show(last)
            last = [key, 1, x0, x1, y0, y1]
        else:
            print('  %-6s c%d fbp %3x fbw %2d sc %3d-%3d,%3d-%3d tex %5s x %7.1f..%7.1f y %7.1f..%7.1f' % (
                t, c, fr[0], fr[1], sc[0], sc[1], sc[2], sc[3], ('%x' % tex) if tex >= 0 else '-', x0, x1, y0, y1))
    if runs and last:
        show(last)


def show(r):
    (t, fr, tex, sc), n, x0, x1, y0, y1 = r
    print('%5d %-6s fbp %3x fbw %2d sc %3d-%3d,%3d-%3d tex %5s x %7.1f..%7.1f y %7.1f..%7.1f  span %.0f%%' % (
        n, t, fr[0], fr[1], sc[0], sc[1], sc[2], sc[3], ('%x' % tex) if tex >= 0 else '-', x0, x1, y0, y1,
        100.0 * (x1 - x0 + 1) / max(1, sc[1] - sc[0] + 1)))


main()
