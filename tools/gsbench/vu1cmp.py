# vu1cmp.py <a_vu1.cap> <b_vu1.cap>: the VU1 starts of each side (HLB_VU1CAP), in order: start pc, and which of the
# 16 KB data, VF and VI differ
import struct, sys, hashlib
def load(fn):
    d = open(fn, 'rb').read(); p = 0; frames = [[]]
    while p + 8 <= len(d):
        t, n = struct.unpack_from('<II', d, p); p += 8
        if t == 0xFF:
            frames.append([]); continue
        pc = struct.unpack_from('<I', d, p)[0]
        frames[-1].append((pc, d[p + 4:p + 4 + 0x4000], d[p + 4 + 0x4000:p + 4 + 0x4000 + 512], d[p + 4 + 0x4000 + 512:p + n]))
        p += n
    return [f for f in frames if f]
A, B = load(sys.argv[1]), load(sys.argv[2])
print('frames', [len(f) for f in A], [len(f) for f in B])
show = int(sys.argv[3]) if len(sys.argv) > 3 else 40
for fi, (fa, fb) in enumerate(zip(A, B)):
    print('frame', fi, 'starts', len(fa), len(fb), 'pcs equal', [x[0] for x in fa] == [x[0] for x in fb])
    for i, (a, b) in enumerate(zip(fa, fb)):
        dm = [q for q in range(0, 0x4000, 16) if a[1][q:q + 16] != b[1][q:q + 16]]
        dvf = [r for r in range(32) if a[2][r * 16:r * 16 + 16] != b[2][r * 16:r * 16 + 16]]
        dvi = [r for r in range(16) if a[3][r * 4:r * 4 + 4] != b[3][r * 4:r * 4 + 4]]
        if (dm or dvf or dvi or a[0] != b[0]) and show > 0:
            show -= 1
            print('  start %d pc %x/%x: data qw %d (%s) vf %s vi %s' % (i, a[0], b[0], len(dm), ' '.join('%x' % (q // 16) for q in dm[:12]), dvf, dvi))
