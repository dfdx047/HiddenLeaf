#!/usr/bin/env python3
"""
Hidden Leaf: decodes the hl_gif.bin the runtime writes with HL_GIF_LOG=N@S (u32 path, u32 size, the packet) into
GIFtags and GS register writes, one line per loop.

  gifdec.py <hl_gif.bin> [max lines]
"""
import struct, sys

REG = {0x00: 'PRIM', 0x01: 'RGBAQ', 0x02: 'ST', 0x03: 'UV', 0x04: 'XYZF2', 0x05: 'XYZ2', 0x06: 'TEX0_1',
       0x07: 'TEX0_2', 0x08: 'CLAMP_1', 0x09: 'CLAMP_2', 0x0a: 'FOG', 0x0c: 'XYZF3', 0x0d: 'XYZ3',
       0x14: 'TEX1_1', 0x15: 'TEX1_2', 0x16: 'TEX2_1', 0x17: 'TEX2_2', 0x18: 'XYOFFSET_1', 0x19: 'XYOFFSET_2',
       0x1a: 'PRMODECONT', 0x1b: 'PRMODE', 0x1c: 'TEXCLUT', 0x22: 'SCANMSK', 0x34: 'MIPTBP1_1', 0x3b: 'TEXA',
       0x3d: 'FOGCOL', 0x3f: 'TEXFLUSH', 0x40: 'SCISSOR_1', 0x41: 'SCISSOR_2', 0x42: 'ALPHA_1', 0x43: 'ALPHA_2',
       0x44: 'DIMX', 0x45: 'DTHE', 0x46: 'COLCLAMP', 0x47: 'TEST_1', 0x48: 'TEST_2', 0x49: 'PABE',
       0x4a: 'FBA_1', 0x4b: 'FBA_2', 0x4c: 'FRAME_1', 0x4d: 'FRAME_2', 0x4e: 'ZBUF_1', 0x4f: 'ZBUF_2',
       0x50: 'BITBLTBUF', 0x51: 'TRXPOS', 0x52: 'TRXREG', 0x53: 'TRXDIR', 0x54: 'HWREG', 0x60: 'SIGNAL',
       0x61: 'FINISH', 0x62: 'LABEL'}


def packed(desc, lo, hi):
    """One PACKED qword → (register name, value text)."""
    if desc == 0x0:  # PRIM
        return 'PRIM', f'{lo & 0x7ff:#x}'
    if desc == 0x1:  # RGBAQ
        return 'RGBAQ', f'{lo & 0xff},{(lo >> 32) & 0xff},{hi & 0xff},{(hi >> 32) & 0xff}'
    if desc == 0x2:
        return 'ST', f'{struct.unpack("<f", struct.pack("<I", lo & 0xffffffff))[0]:.3f},' \
                     f'{struct.unpack("<f", struct.pack("<I", lo >> 32))[0]:.3f}'
    if desc == 0x3:
        return 'UV', f'{(lo & 0x3fff) / 16:.1f},{((lo >> 32) & 0x3fff) / 16:.1f}'
    if desc in (0x4, 0x5):
        x, y, z = (lo & 0xffff) / 16, ((lo >> 32) & 0xffff) / 16, (hi >> 4) & 0xffffff if desc == 4 else hi & 0xffffffff
        kick = '' if (hi >> 47) & 1 == 0 else ' noKick'
        return ('XYZF2' if desc == 4 else 'XYZ2'), f'{x:.1f},{y:.1f} z={z:#x}{kick}'
    if desc == 0xe:  # A+D
        return REG.get(hi & 0xff, f'reg{hi & 0xff:#x}'), f'{lo:#x}'
    if desc == 0xf:
        return 'NOP', ''
    return REG.get(desc, f'd{desc:#x}'), f'{lo:#x}/{hi:#x}'


def decode(buf, out, maxlines):
    off = 0
    n = 0
    while off + 8 <= len(buf) and n < maxlines:
        path, size = struct.unpack_from('<II', buf, off)
        data = buf[off + 8: off + 8 + size]
        off += 8 + size
        print(f'--- packet path={path} size={size:#x}', file=out)
        n += 1
        p = 0
        while p + 16 <= len(data):
            lo, hi = struct.unpack_from('<QQ', data, p)
            p += 16
            nloop, eop, pre, prim, flg, nreg = lo & 0x7fff, (lo >> 15) & 1, (lo >> 46) & 1, (lo >> 47) & 0x7ff, \
                (lo >> 58) & 3, (lo >> 60) & 0xf
            nreg = nreg or 16
            descs = [(hi >> (4 * i)) & 0xf for i in range(nreg)]
            print(f'  tag nloop={nloop} eop={eop} flg={flg} pre={pre} prim={prim:#x} regs={"".join("%x" % d for d in descs)}',
                  file=out)
            n += 1
            if flg == 0:
                for _ in range(nloop):
                    line = []
                    for d in descs:
                        if p + 16 > len(data):
                            break
                        qlo, qhi = struct.unpack_from('<QQ', data, p)
                        p += 16
                        name, val = packed(d, qlo, qhi)
                        line.append(f'{name}={val}')
                    print('    ' + ' '.join(line), file=out)
                    n += 1
            elif flg == 1:
                words = nloop * nreg
                p += ((words + 1) // 2) * 16
                print(f'    (reglist {words} words)', file=out)
            else:
                p += nloop * 16
                print(f'    (image {nloop * 16} bytes)', file=out)
            if eop and p < len(data):
                print(f'    ({len(data) - p} bytes after EOP)', file=out)
                break


if __name__ == '__main__':
    decode(open(sys.argv[1], 'rb').read(), sys.stdout, int(sys.argv[2]) if len(sys.argv) > 2 else 10 ** 9)
