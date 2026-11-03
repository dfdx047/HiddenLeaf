#!/usr/bin/env python3
"""gsdump.py: looks into a PCSX2 GS dump (.gs, .gs.zst) as HL_GS_DUMP writes it on Android.

  gsdump.py info FILE...          header (state version, serial, crc, screenshot) and the packets: transfers per path,
                                  vsyncs, ReadFIFO2, registers; stops with an error where the stream does not parse
  gsdump.py shot FILE PNG         the screenshot of the header (what was on screen when the dump started)

The format is GSDump.h's: [0xFFFFFFFF][header size][GSDumpHeader, serial, screenshot, provenance][state][PRIVREGS 0x2000]
then packets: 0 transfer [path u8][size u32][data], 1 vsync [field u8], 2 ReadFIFO2 [size u32], 3 registers [0x2000].
Needs the zstd program for .zst (no Python module).
"""
import struct
import subprocess
import sys

PRIVREGS = 0x2000


class Stream:
    def __init__(self, path):
        if path.endswith('.zst'):
            self.proc = subprocess.Popen(['zstd', '-dc', path], stdout=subprocess.PIPE)
            self.f = self.proc.stdout
        else:
            self.proc = None
            self.f = open(path, 'rb')
        self.pos = 0

    def read(self, n):
        b = self.f.read(n)
        self.pos += len(b)
        return b

    def exact(self, n, what):
        b = self.read(n)
        if len(b) != n:
            raise EOFError(f'{what}: {len(b)} of {n} bytes at {self.pos}')
        return b

    def close(self):
        self.f.close()
        if self.proc:
            self.proc.wait()


def header(s):
    crc, size = struct.unpack('<II', s.exact(8, 'start'))
    if crc != 0xFFFFFFFF:
        raise ValueError(f'old dump format (crc {crc:08x}), not what PCSX2 writes now')
    blob = s.exact(size, 'header')
    (version, state_size, serial_off, serial_size, dcrc, sw, sh, shot_off, shot_size) = struct.unpack_from('<9I', blob, 0)
    serial = blob[serial_off:serial_off + serial_size].decode('ascii', 'replace')
    shot = blob[shot_off:shot_off + shot_size]
    prov = blob[shot_off + shot_size:]
    provenance = None
    if len(prov) >= 8:
        magic, flags = struct.unpack_from('<II', prov, 0)
        provenance = (magic == 0x564F5250, flags)
    state = s.exact(state_size, 'state')
    regs = s.exact(PRIVREGS, 'registers')
    return dict(version=version, state_size=state_size, serial=serial, crc=dcrc, shot=(sw, sh, shot), provenance=provenance,
                state=state, regs=regs)


def info(path):
    s = Stream(path)
    h = header(s)
    sw, sh, _ = h['shot']
    print(f'{path}')
    print(f'  state version {h["version"]}, state {h["state_size"]} bytes, serial {h["serial"]}, crc {h["crc"]:08X}, '
          f'screenshot {sw}x{sh}, provenance {h["provenance"]}')
    pmode = struct.unpack_from('<Q', h['regs'], 0)[0]
    print(f'  PMODE {pmode:x}')
    counts = {0: 0, 1: 0, 2: 0, 3: 0}
    by_path = {}
    bytes_by_path = {}
    error = None
    while True:
        t = s.read(1)
        if not t:
            break
        t = t[0]
        try:
            if t == 0:
                path_index, size = struct.unpack('<BI', s.exact(5, 'transfer'))
                s.exact(size, 'transfer data')
                by_path[path_index] = by_path.get(path_index, 0) + 1
                bytes_by_path[path_index] = bytes_by_path.get(path_index, 0) + size
            elif t == 1:
                s.exact(1, 'vsync')
            elif t == 2:
                s.exact(4, 'readfifo')
            elif t == 3:
                s.exact(PRIVREGS, 'registers')
            else:
                error = f'unknown packet {t:#x} at {s.pos - 1}'
                break
        except EOFError as e:
            error = f'cut short: {e}'
            break
        counts[t] += 1
    s.close()
    paths = ', '.join(f'path {p}: {by_path[p]} ({bytes_by_path[p] / 1e6:.1f} MB)' for p in sorted(by_path))
    print(f'  {counts[1]} vsyncs, {counts[0]} transfers ({paths}), {counts[2]} ReadFIFO2, {counts[3]} registers, '
          f'{s.pos / 1e6:.1f} MB unpacked')
    if error:
        print(f'  ERROR: {error}')
    return error is None


def shot(path, png):
    from PIL import Image
    s = Stream(path)
    h = header(s)
    s.close()
    w, hh, px = h['shot']
    if not w:
        raise SystemExit(f'{path}: no screenshot')
    Image.frombytes('RGBA', (w, hh), px).convert('RGB').save(png)
    print(f'{png}: {w}x{hh}')


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == 'info':
        ok = all([info(p) for p in sys.argv[2:]])
        sys.exit(0 if ok else 1)
    if len(sys.argv) == 4 and sys.argv[1] == 'shot':
        shot(sys.argv[2], sys.argv[3])
        sys.exit(0)
    print(__doc__)
    sys.exit(2)
