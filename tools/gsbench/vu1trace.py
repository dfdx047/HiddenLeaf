# vu1trace.py <a_vu1.cap> <a_first> <b_vu1.cap> <b_first> [n]: the VU1 instruction traces (HLB_VU1TRACE) of the
# starts a_first.. and b_first.. side by side: for each start, the first instruction whose pc, VI or VF differ
import struct, sys
def load(fn):
    d = open(fn, 'rb').read(); p = 0; starts = []; code = None
    while p + 8 <= len(d):
        t, n = struct.unpack_from('<II', d, p); p += 8
        if t == 0xFF:
            continue
        if t == 0xA4:
            code = d[p:p + n]
        elif t == 0xA1:
            starts.append([struct.unpack_from('<I', d, p)[0], d[p + 4:p + 4 + 0x4000], d[p + 4 + 0x4000:p + n], []])
        elif t == 0xA3:
            pc = struct.unpack_from('<I', d, p)[0]
            starts[-1][3].append((pc, struct.unpack_from('<16H', d, p + 4), d[p + 36:p + n]))
        p += n
    return starts, code
A, ca = load(sys.argv[1]); B, cb = load(sys.argv[3])
a0, b0 = int(sys.argv[2]), int(sys.argv[4]); n = int(sys.argv[5]) if len(sys.argv) > 5 else 11
print('code equal', ca == cb if ca and cb else 'n/a')
for j in range(n):
    sa, sb = A[a0 + j], B[b0 + j]
    ta, tb = sa[3], sb[3]
    first = None
    for k, (x, y) in enumerate(zip(ta, tb)):
        if x[0] != y[0] or x[1][1:] != y[1][1:] or x[2] != y[2]:
            first = k; break
    print('start %d/%d pc %x/%x steps %d/%d first diff %s' % (a0 + j, b0 + j, sa[0], sb[0], len(ta), len(tb), first))
    if first is not None:
        for k in range(max(0, first - 2), min(first + 2, len(ta), len(tb))):
            x, y = ta[k], tb[k]
            print('   step %d pc %x/%x' % (k, x[0], y[0]))
            dvi = [(r, hex(x[1][r]), hex(y[1][r])) for r in range(1, 16) if x[1][r] != y[1][r]]
            if dvi: print('     VI', dvi)
            for r in range(32):
                u, v = x[2][r * 16:r * 16 + 16], y[2][r * 16:r * 16 + 16]
                if u != v:
                    print('     VF%d' % r, u.hex(), v.hex(), ['%.7g' % f for f in struct.unpack('<4f', u)], ['%.7g' % f for f in struct.unpack('<4f', v)])
