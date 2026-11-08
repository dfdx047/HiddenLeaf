#!/usr/bin/env python3
"""Compares the bench records of PCSX2 (HLBench.cpp, out of the repository) and of the recomp (recomp/runtime/hl_bench.cpp).

    tools/hlbench.py info <run.bin>
    tools/hlbench.py diff <pcsx2.bin> <recomp.bin> [--fight N] [--ignore 0x10,0x14-0x1f] [--show 20]
    tools/hlbench.py dump <run.bin> --fight N --call C [--fighter|--camera] [--from 0x0 --to 0x100]

A record is written at the entry of 0x21E660 (the per-frame input of each fighter): vsync, fight, call number in the fight,
fighter address, return address, the Mersenne Twister's index and a hash of its state, the camera's address, then the
fighter's 0xC00 bytes and the camera's 0x280. The two sides are matched by fight and by call number; a fighter is known
by its order of first appearance in the fight (the heap may place it elsewhere). Words that are pointers on both sides
(0x100000..0x2000000) and differ by the same amount as the fighters' addresses are taken as equal.
"""
import struct
import sys

HDR = struct.Struct('<4I')
REC = struct.Struct('<9I')


def load(path):
    data = open(path, 'rb').read()
    magic, ver, fb, cb = HDR.unpack_from(data, 0)
    assert magic == 0x31424C48, path
    off = HDR.size
    size = REC.size + fb + cb
    out = []
    while off + size <= len(data):
        tag, vs, fight, call, f, ra, mti, mth, cam = REC.unpack_from(data, off)
        if tag != 0x63655246:
            break
        body = data[off + REC.size: off + size]
        out.append(dict(vsync=vs, fight=fight, call=call, f=f, ra=ra, mti=mti, mth=mth, cam=cam,
                        fighter=body[:fb], camera=body[fb:]))
        off += size
    return out, fb, cb


def by_fight(recs):
    fights = {}
    for r in recs:
        fights.setdefault(r['fight'], []).append(r)
    for recs in fights.values():
        order = []
        for r in recs:
            if r['f'] not in order:
                order.append(r['f'])
            r['slot'] = order.index(r['f'])
    # matched across the two runs by fighter and that fighter's own frame number (the global call order can drift)
    for n, recs in fights.items():
        count = {}
        for r in recs:
            r['n'] = count.get(r['slot'], 0)
            count[r['slot']] = r['n'] + 1
        fights[n] = sorted(recs, key=lambda r: (r['n'], r['slot']))
    return fights


def words(b):
    return struct.unpack('<%dI' % (len(b) // 4), b)


def fl(w):
    return struct.unpack('<f', struct.pack('<I', w))[0]


def is_ptr(w):
    return 0x100000 <= w < 0x2000000


def parse_ignore(s):
    out = set()
    if not s:
        return out
    for part in s.split(','):
        if '-' in part:
            a, b = part.split('-')
            out.update(range(int(a, 0) & ~3, (int(b, 0) & ~3) + 4, 4))
        else:
            out.add(int(part, 0) & ~3)
    return out


def diff_words(a, b, delta, ignore):
    wa, wb = words(a), words(b)
    out = []
    for i, (x, y) in enumerate(zip(wa, wb)):
        if x == y or i * 4 in ignore:
            continue
        if is_ptr(x) and is_ptr(y) and delta is not None and (y - x) & 0xFFFFFFFF == delta:
            continue
        out.append((i * 4, x, y))
    return out


def semantic(r):
    b = r['fighter']
    return (r['slot'],) + struct.unpack_from('<3I', b, 0x30) + struct.unpack_from('<2I', b, 0x6C) + struct.unpack_from('<H', b, 0x18E)


def show_semantic(r):
    b = r['fighter']
    x, y, z = struct.unpack_from('<3f', b, 0x30)
    hp, ck = struct.unpack_from('<2f', b, 0x6C)
    act = struct.unpack_from('<H', b, 0x18E)[0]
    return 'slot %d pos %.4f %.4f %.4f hp %.5f chakra %.3f act %d' % (r['slot'], x, y, z, hp, ck, act)


def fmt(off, x, y):
    return '+0x%03x %08x %08x  (%g / %g)' % (off, x, y, fl(x), fl(y))


def cmd_info(path):
    recs, fb, cb = load(path)
    fights = by_fight(recs)
    print('%s: %d records, fighter %#x bytes, camera %#x' % (path, len(recs), fb, cb))
    for n, rs in sorted(fights.items()):
        slots = sorted(set(r['slot'] for r in rs))
        print('  fight %d: %d calls, v%d..v%d, fighters %s, mti at start %d, ra %s' % (
            n, len(rs), rs[0]['vsync'], rs[-1]['vsync'], ['%x' % next(r['f'] for r in rs if r['slot'] == s) for s in slots],
            rs[0]['mti'], sorted(set('%x' % r['ra'] for r in rs))))


def cmd_diff(pa, pb, fight=None, ignore='', show=20):
    ra, _, _ = load(pa)
    rb, _, _ = load(pb)
    fa, fb = by_fight(ra), by_fight(rb)
    ign = parse_ignore(ignore)
    for n in sorted(set(fa) & set(fb)):
        if fight is not None and n != fight:
            continue
        A, B = fa[n], fb[n]
        keys = set((r['n'], r['slot']) for r in A) & set((r['n'], r['slot']) for r in B)
        A = [r for r in A if (r['n'], r['slot']) in keys]
        B = [r for r in B if (r['n'], r['slot']) in keys]
        print('fight %d: PCSX2 %d calls (v%d), recomp %d calls (v%d)' % (n, len(A), A[0]['vsync'], len(B), B[0]['vsync']))
        first = None
        counts = []
        for i, (x, y) in enumerate(zip(A, B)):
            problems = []
            if x['slot'] != y['slot']:
                problems.append('order: fighter slot %d / %d' % (x['slot'], y['slot']))
            if x['ra'] != y['ra']:
                problems.append('caller %x / %x' % (x['ra'], y['ra']))
            if x['mti'] != y['mti'] or x['mth'] != y['mth']:
                problems.append('rng: mti %d / %d, state %08x / %08x' % (x['mti'], y['mti'], x['mth'], y['mth']))
            delta = (y['f'] - x['f']) & 0xFFFFFFFF
            fd = diff_words(x['fighter'], y['fighter'], delta, ign)
            cd = diff_words(x['camera'], y['camera'], (y['cam'] - x['cam']) & 0xFFFFFFFF, set())
            counts.append((len(fd), len(cd), bool(problems)))
            if (fd or cd or problems) and first is None:
                first = i
                print('  first difference: fighter slot %d, its frame %d (PCSX2 v%d, recomp v%d)' % (x['slot'], x['n'], x['vsync'], y['vsync']))
                for p in problems:
                    print('    ' + p)
                if fd:
                    print('    fighter: %d words differ' % len(fd))
                    for d in fd[:show]:
                        print('      ' + fmt(*d))
                if cd:
                    print('    camera: %d words differ' % len(cd))
                    for d in cd[:show]:
                        print('      ' + fmt(*d))
        # what a player sees: position, health, chakra, action of each fighter
        game = None
        for i, (x, y) in enumerate(zip(A, B)):
            if semantic(x) != semantic(y):
                game = i
                break
        if game is None:
            print('  game state (position +0x30/34/38, health +0x6C, chakra +0x70, action +0x18E): equal in all calls')
        else:
            x, y = A[game], B[game]
            print('  game state first differs at frame %d of fighter slot %d (PCSX2 v%d = fight +%d vsyncs, recomp v%d = +%d):' % (
                x['n'], x['slot'], x['vsync'], x['vsync'] - A[0]['vsync'], y['vsync'], y['vsync'] - B[0]['vsync']))
            print('    PCSX2  ' + show_semantic(x))
            print('    recomp ' + show_semantic(y))
            # a timeline of the CPU fighter and the other one
            step = max(1, (min(len(A), len(B)) - game) // 8)
            for i in range(game, min(len(A), len(B)), step):
                print('    frame %5d  P %s | R %s' % (A[i]['n'], show_semantic(A[i]), show_semantic(B[i])))
        if first is None:
            print('  equal for all %d calls compared' % min(len(A), len(B)))
        else:
            # how it spreads: words differing per call, every so often
            step = max(1, (len(counts) - first) // 15)
            print('  spread (call: fighter words, camera words): ' + ', '.join(
                '%d:%d/%d' % (i + 1, counts[i][0], counts[i][1]) for i in range(first, len(counts), step)))


def cmd_dump(path, fight, call, what='fighter', a=0, b=None):
    recs, _, _ = load(path)
    fights = by_fight(recs)
    r = fights[fight][call - 1]
    body = r[what]
    b = len(body) if b is None else b
    print('fight %d call %d: v%d fighter %x slot %d ra %x mti %d cam %x' % (fight, call, r['vsync'], r['f'], r['slot'], r['ra'], r['mti'], r['cam']))
    w = words(body)
    for off in range(a & ~3, b, 16):
        print('  +0x%03x ' % off + ' '.join('%08x' % w[(off >> 2) + k] for k in range(4) if (off >> 2) + k < len(w)) +
              '   ' + ' '.join('%10.4g' % fl(w[(off >> 2) + k]) for k in range(4) if (off >> 2) + k < len(w)))


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 1
    opts = {}
    pos = []
    i = 2
    while i < len(argv):
        if argv[i].startswith('--'):
            key = argv[i][2:]
            if key in ('fighter', 'camera'):
                opts['what'] = key
                i += 1
                continue
            opts[key] = argv[i + 1]
            i += 2
        else:
            pos.append(argv[i])
            i += 1
    if argv[1] == 'info':
        for p in pos:
            cmd_info(p)
    elif argv[1] == 'diff':
        cmd_diff(pos[0], pos[1], int(opts['fight']) if 'fight' in opts else None, opts.get('ignore', ''), int(opts.get('show', 20)))
    elif argv[1] == 'dump':
        cmd_dump(pos[0], int(opts['fight']), int(opts['call']), opts.get('what', 'fighter'), int(opts.get('from', '0'), 0),
                 int(opts['to'], 0) if 'to' in opts else None)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
