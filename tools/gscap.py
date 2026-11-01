#!/usr/bin/env python3
"""gscap.py FILE: what a captured fight frame draws (HL_GSCAP): by path, frame buffer, texture, primitive and vertices.

Follows GIF tags. PACKED regs: A+D (0xE) updates registers, STQ/RGBA/UV/XYZ2/XYZF2/FOG. REGLIST (mode 1) counts vertices
by XYZ2 too. IMAGE (mode 2) counts upload bytes against the last BITBLTBUF/TRXPOS/TRXREG."""
import collections
import struct
import sys

d = open(sys.argv[1], 'rb').read()
p = 0
vs = []  # per vsync: list of (path, bytes)
cur = []
while p + 8 <= len(d):
    path, size = struct.unpack_from('<II', d, p)
    p += 8
    if path == 0xFF:
        vs.append((size, cur))
        cur = []
        continue
    cur.append((path, d[p:p + size]))
    p += size
if cur:
    vs.append((-1, cur))

R_PRIM, R_RGBAQ, R_ST, R_UV, R_XYZF2, R_XYZ2, R_TEX0_1, R_TEX0_2 = 0, 1, 2, 3, 4, 5, 6, 7
R_FRAME1, R_FRAME2, R_ZBUF1, R_ZBUF2 = 0x4c, 0x4d, 0x4e, 0x4f
R_BITBLT, R_TRXPOS, R_TRXREG, R_TRXDIR = 0x50, 0x51, 0x52, 0x53
PRIMS = ['POINT', 'LINE', 'LSTRIP', 'TRI', 'TSTRIP', 'TFAN', 'SPRITE', '?']


class St:
    def __init__(self):
        self.reg = {}
        self.prim = 0


def frame_stats(pkts):
    st = St()
    draws = collections.OrderedDict()  # (fbp, tbp, prim bits) -> [verts, batches]
    seq = []  # order of draws for a timeline
    by_path = collections.Counter()
    bytes_path = collections.Counter()
    upload = 0
    uploads = []
    tags = 0

    def key():
        prim = st.prim
        fr = st.reg.get(R_FRAME1, 0)
        fbp = fr & 0x1ff
        tex = st.reg.get(R_TEX0_1, 0)
        tbp = tex & 0x3fff
        tme = (prim >> 4) & 1
        abe = (prim >> 6) & 1
        typ = prim & 7
        psm = (tex >> 20) & 0x3f
        return (fbp, tbp if tme else -1, PRIMS[typ], abe, psm if tme else -1)

    for path, b in pkts:
        bytes_path[path] += len(b)
        q = 0
        while q + 16 <= len(b):
            lo, hi = struct.unpack_from('<QQ', b, q)
            q += 16
            tags += 1
            nloop = lo & 0x7fff
            pre = (lo >> 46) & 1
            prim = (lo >> 47) & 0x7ff
            flg = (lo >> 58) & 3
            nreg = (lo >> 60) & 0xf or 16
            if pre:
                st.prim = prim
            regs = [(hi >> (4 * i)) & 0xf for i in range(nreg)]
            nv = 0
            if flg == 0:
                for _ in range(nloop):
                    for r in regs:
                        if q + 16 > len(b):
                            break
                        a, c = struct.unpack_from('<QQ', b, q)
                        q += 16
                        if r == 0xe:
                            reg = c & 0xff
                            st.reg[reg] = a
                            if reg == R_PRIM:
                                st.prim = a & 0x7ff
                        elif r in (5, 4):
                            nv += 1
                        elif r == 0xf:
                            pass
            elif flg == 1:
                for _ in range(nloop):
                    for r in regs:
                        if q + 8 > len(b):
                            break
                        q += 8
                        if r in (5, 4):
                            nv += 1
                if (nloop * nreg) & 1:
                    q += 8
            else:
                n = nloop * 16
                upload += n
                uploads.append((st.reg.get(R_BITBLT, 0) >> 32 & 0x3fff, n))
                q += n
            if nv:
                k = key()
                e = draws.setdefault(k, [0, 0])
                e[0] += nv
                e[1] += 1
                seq.append((k, nv))
                by_path[path] += nv
    return draws, seq, by_path, bytes_path, upload, uploads, tags


for n, (v, pkts) in enumerate(vs):
    draws, seq, by_path, bytes_path, upload, uploads, tags = frame_stats(pkts)
    total = sum(e[0] for e in draws.values())
    print(f'== vsync {v}: {len(pkts)} packets, {sum(len(b) for _, b in pkts)} bytes, tags {tags}, verts {total}, '
          f'state changes (groups) {len(seq)}, upload {upload}, by path verts {dict(by_path)} bytes {dict(bytes_path)}')
    if total < 500:
        continue
    print('   fbp tbp   prim   abe psm   verts batches')
    for k, e in sorted(draws.items(), key=lambda kv: -kv[1][0])[:28]:
        print(f'   {k[0]:<3x} {k[1]:<5x} {k[2]:<6} {k[3]}   {k[4]:<3x} {e[0]:>6} {e[1]:>5}')
    fb = collections.Counter()
    for k, e in draws.items():
        fb[k[0]] += e[0]
    print('   per framebuffer:', {hex(a): b for a, b in fb.most_common(8)})
    # timeline: runs by target texture
    t = []
    last = None
    for k, nv in seq:
        if k != last:
            t.append([k, nv])
            last = k
        else:
            t[-1][1] += nv
    print(f'   distinct runs {len(t)}')
