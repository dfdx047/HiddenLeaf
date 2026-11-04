#!/usr/bin/env python3
"""vifdump.py: reads the HL_VIF_DUMP pairs (<base>.vif and <base>.gif, or .vif.zst and .gif.zst; docs/vifdump.md).

  vifdump.py info BASE...    header, start snapshot, record counts per type and per GIF path, sizes, vsyncs, the sequence
                             numbers checked (rising, no number in both files), VIF1 codes seen (MPG, MSCAL, UNPACK...)
                             and the VU1 microprograms uploaded (MPG: address, size, CRC32)

BASE is the path without the extension. .zst files need the zstd program.
"""
import struct, subprocess, sys, zlib, collections, os

HDR = struct.Struct('<8sII32s32sQII')
REC = struct.Struct('<IIQ')
VIF_NAMES = {0x00: 'NOP', 0x01: 'STCYCL', 0x02: 'OFFSET', 0x03: 'BASE', 0x04: 'ITOP', 0x05: 'STMOD', 0x06: 'MSKPATH3', 0x07: 'MARK',
             0x10: 'FLUSHE', 0x11: 'FLUSH', 0x13: 'FLUSHA', 0x14: 'MSCAL', 0x15: 'MSCALF', 0x17: 'MSCNT', 0x20: 'STMASK', 0x30: 'STROW',
             0x31: 'STCOL', 0x4A: 'MPG', 0x50: 'DIRECT', 0x51: 'DIRECTHL'}


def open_bytes(base, ext):
    for p in (base + ext, base + ext + '.zst'):
        if os.path.exists(p):
            if p.endswith('.zst'):
                return subprocess.run(['zstd', '-dc', p], check=True, capture_output=True).stdout
            return open(p, 'rb').read()
    raise SystemExit(f'no {base}{ext}[.zst]')


def records(buf):
    magic, ver, hsize, scene, dev, tick, frames, _ = HDR.unpack_from(buf, 0)
    head = dict(magic=magic.rstrip(b'\0').decode(), version=ver, scene=scene.rstrip(b'\0').decode(), device=dev.rstrip(b'\0').decode(),
                tick=tick, frames=frames)
    pos, out = hsize, []
    while pos + REC.size <= len(buf):
        t, size, seq = REC.unpack_from(buf, pos)
        pos += REC.size
        out.append((t, seq, buf[pos:pos + size]))
        pos += size
        if t == 0xFF:
            break
    head['trailing'] = len(buf) - pos
    return head, out


def vif_codes(data, stats, mpgs):
    """Walks one VIF1 transfer's codes (enough to count them and find MPG uploads; DIRECT owed from an earlier transfer is not followed)."""
    pos, n = 0, len(data)
    while pos + 4 <= n:
        cmd = struct.unpack_from('<I', data, pos)[0]
        pos += 4
        op, num, imm = (cmd >> 24) & 0x7F, (cmd >> 16) & 0xFF, cmd & 0xFFFF
        if op >= 0x60:
            vn, vl = (op >> 2) & 3, op & 3
            nn = num or 256
            bits = (32 >> vl) * (vn + 1)
            stats['UNPACK'] += 1
            # (the length with CYCLE's WL < CL is the count of written vectors; num counts them, so this is the usual case)
            pos += ((bits * nn + 31) // 32) * 4
            continue
        name = VIF_NAMES.get(op, f'0x{op:02x}')
        stats[name] += 1
        if op in (0x20,):
            pos += 4
        elif op in (0x30, 0x31):
            pos += 16
        elif op == 0x4A:
            size = (num or 256) * 8
            mpgs.append((imm * 8, size, zlib.crc32(data[pos:pos + size]) & 0xFFFFFFFF))
            pos += size
        elif op in (0x50, 0x51):
            pos += (imm or 65536) * 16


def info(base):
    vb, gb = open_bytes(base, '.vif'), open_bytes(base, '.gif')
    vh, vr = records(vb)
    gh, gr = records(gb)
    print(f'## {os.path.basename(base)}: {vh["scene"]} on {vh["device"]}, from route vsync {vh["tick"]}, {vh["frames"]} vsyncs asked')
    print(f'   .vif {len(vb)} bytes, {len(vr)} records; .gif {len(gb)} bytes, {len(gr)} records; trailing {vh["trailing"]}/{gh["trailing"]}')
    ok = True
    for name, rs in (('.vif', vr), ('.gif', gr)):
        seqs = [s for _, s, _ in rs]
        if any(b <= a for a, b in zip(seqs, seqs[1:])):
            print(f'   {name}: sequence numbers do not rise'); ok = False
        if not rs or rs[-1][0] != 0xFF:
            print(f'   {name}: no end record'); ok = False
    vs = {s for t, s, _ in vr if t not in (2, 0xFF)}
    gs = {s for t, s, _ in gr if t not in (2, 0xFF)}
    if vs & gs:
        print(f'   {len(vs & gs)} sequence numbers in both files'); ok = False
    st = [p for t, _, p in vr if t == 0x54415453]
    if len(st) != 1:
        print(f'   {len(st)} start snapshots'); ok = False
    else:
        p = st[0]
        cs, ds = struct.unpack_from('<II', p, 0)
        code = p[8:8 + cs]
        regs = struct.unpack_from('<23I', p, 8 + cs + ds)
        pend = struct.unpack_from('<4I', p, 8 + cs + ds + 92)
        vu = 8 + cs + ds + 92 + 16
        pc = struct.unpack_from('<I', p, vu + 512 + 64 + 16 + 12 + 4)[0]
        print(f'   snapshot: {len(p)} bytes, VU1 code {cs} (crc {zlib.crc32(code) & 0xFFFFFFFF:08x}), data {ds}; VIF1 cycle {regs[4]:04x} mode {regs[5]} '
              f'mask {regs[7]:08x} base {regs[10]:x} ofst {regs[11]:x} tops {regs[12]:x} itop {regs[13]:x} top {regs[14]:x}; owed DIRECT {pend[0]} '
              f'IMAGE {pend[1]}; PATH3 masked {pend[3]}; VU1 pc {pc:#x}')
    vif = [p for t, _, p in vr if t == 1]
    vsy = [p for t, _, p in vr if t == 2]
    print(f'   VIF1: {len(vif)} transfers, {sum(map(len, vif))} bytes; {len(vsy)} vsyncs '
          f'({sum(map(len, vif)) / max(1, len(vsy)) / 1024:.0f} KiB a vsync)')
    crcs = [struct.unpack_from('<QIII', p, 0) for p in vsy]
    codechanges = sum(1 for a, b in zip(crcs, crcs[1:]) if a[1] != b[1])
    print(f'   VU1 micro memory changed between vsyncs {codechanges} times; distinct code CRCs {len({c[1] for c in crcs})}')
    stats, mpgs = collections.Counter(), []
    for p in vif:
        vif_codes(p, stats, mpgs)
    print('   codes: ' + ', '.join(f'{k} {v}' for k, v in stats.most_common()))
    up = collections.Counter(mpgs)
    print(f'   MPG uploads: {len(mpgs)}, distinct (address, size, crc) {len(up)}: ' +
          ', '.join(f'{a:#06x}+{s} {c:08x} x{n}' for (a, s, c), n in sorted(up.items())[:12]) + (' ...' if len(up) > 12 else ''))
    paths, inside, sizes = collections.Counter(), collections.Counter(), collections.Counter()
    for t, _, p in gr:
        if t == 1:
            path, hl, inv = p[0], p[1], p[2]
            paths[path] += 1
            inside[(path, inv)] += 1
            sizes[path] += len(p) - 16
    priv = sum(1 for t, _, _ in gr if t == 3)
    gvs = sum(1 for t, _, _ in gr if t == 2)
    print('   GIF: ' + ', '.join(f'PATH{k} {paths[k]} packets {sizes[k]} bytes ({inside[(k, 1)]} inside a VIF1 transfer)' for k in sorted(paths)) +
          f'; {priv} privileged writes; {gvs} vsyncs')
    print('   ' + ('ok' if ok else 'PROBLEMS'))
    return ok


if __name__ == '__main__':
    if len(sys.argv) < 3 or sys.argv[1] != 'info':
        raise SystemExit(__doc__)
    bad = [b for b in sys.argv[2:] if not info(b[:-4] if b.endswith(('.vif', '.gif')) else b)]
    sys.exit(1 if bad else 0)
