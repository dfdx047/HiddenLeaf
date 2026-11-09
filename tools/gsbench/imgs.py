# imgs.py <a.cap> <b.cap>: every host->local transfer (dbp dpsm dbw w h dx dy, data md5) on each side; what one has and the other lacks
import struct, sys, hashlib, collections
BPP = {0x00: 32, 0x01: 24, 0x02: 16, 0x0a: 16, 0x13: 8, 0x14: 4, 0x1b: 8, 0x24: 4, 0x2c: 4, 0x30: 32, 0x31: 24, 0x32: 16, 0x3a: 16}
def load(fn):
    d = open(fn, 'rb').read(); p = 0; out = []
    while p + 8 <= len(d):
        path, size = struct.unpack_from('<II', d, p); p += 8
        if path == 0xFF: continue
        out.append((path, d[p:p + size])); p += size
    return out
def xfers(fn):
    res = []; st = {}  # per path: registers, the open transfer, IMAGE qwords still owed (they may come in its next packet, untagged)
    for path, b in load(fn):
        reg, cur, left = st.get(path, ({}, None, 0))
        q = 0
        if left:
            n = min(left * 16, len(b))
            if cur is not None and len(cur[2]) < cur[1]: cur[2] += b[:n][:cur[1] - len(cur[2])]
            q = n; left -= n // 16
        while q + 16 <= len(b):
            lo, hi = struct.unpack_from('<QQ', b, q); q += 16
            nloop = lo & 0x7fff; flg = (lo >> 58) & 3; nreg = (lo >> 60) & 0xf or 16
            regs = [(hi >> (4 * i)) & 0xf for i in range(nreg)]
            if flg == 0:
                for _ in range(nloop):
                    for r in regs:
                        if q + 16 > len(b): break
                        a, cc = struct.unpack_from('<QQ', b, q); q += 16
                        if r == 0xe:
                            reg[cc & 0xff] = a
                            if (cc & 0xff) == 0x53 and (a & 3) == 0:
                                bb = reg.get(0x50, 0); tr = reg.get(0x52, 0); tp = reg.get(0x51, 0)
                                psm = (bb >> 56) & 0x3f; w = tr & 0xfff; h = (tr >> 32) & 0xfff
                                need = w * h * BPP.get(psm, 32) // 8
                                cur = [((bb >> 32) & 0x3fff, psm, (bb >> 48) & 0x3f, w, h, (tp >> 32) & 0x7ff, (tp >> 48) & 0x7ff), need, bytearray()]
                                res.append(cur)
            elif flg == 1:
                q += nloop * nreg * 8 + (8 if (nloop * nreg) & 1 else 0)
            else:
                n = min(nloop * 16, len(b) - q); left = nloop - n // 16
                if cur is not None and len(cur[2]) < cur[1]:
                    cur[2] += b[q:q + n][:cur[1] - len(cur[2])]
                q += n
        st[path] = (reg, cur, left)
    return [(k, hashlib.md5(bytes(d)).hexdigest()[:8], len(d) >= need) for k, need, d in res]
A, B = xfers(sys.argv[1]), xfers(sys.argv[2])
print(len(A), len(B), 'transfers')
ca = collections.Counter((k, h) for k, h, _ in A); cb = collections.Counter((k, h) for k, h, _ in B)
print('psm counts A', collections.Counter(k[1] for k, h, _ in A)); print('psm counts B', collections.Counter(k[1] for k, h, _ in B))
onlyA = ca - cb; onlyB = cb - ca
print('only in A', sum(onlyA.values()), ' only in B', sum(onlyB.values()))
ka = {k for (k, h) in onlyA}; kb = {k for (k, h) in onlyB}
print('same header, other data:', len(ka & kb), ' header only in A:', len(ka - kb), ' header only in B:', len(kb - ka))
for k in sorted(ka - kb)[:15]: print('  A', k)
for k in sorted(kb - ka)[:15]: print('  B', k)
for k in sorted(ka & kb)[:10]: print('  data', k)
