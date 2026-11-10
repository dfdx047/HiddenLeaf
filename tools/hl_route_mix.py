#!/usr/bin/env python3
# hl_route_mix.py BASE [--char MOVES] [--char-at V] [--mix]: a device route (the v3r base, one "V:WORDS" per comma) with
#  --char "DOWN RIGHT": cursor moves on the character select before the 1P choice (the step at --char-at, 4373 in v3r).
#    From the cursor on Naruto (this memcard): "RIGHT RIGHT" Neji, "DOWN" Sakura, "DOWN RIGHT" Kakashi; RIGHT and LEFT
#    alone land on a locked one and the route stalls.
#  --mix: the fight steps (@N+...) become a fixed mix of what a player does (attack, walk, jump, chakra, item, guard,
#    change of item), one word every 12 vsyncs, the same span of vsyncs as the base's.
# The stage moves are tools/hl_stage_route.py's; run this one first.
import sys, argparse
p = argparse.ArgumentParser()
p.add_argument('base'); p.add_argument('--char', default=''); p.add_argument('--char-at', type=int, default=4373)
p.add_argument('--mix', action='store_true')
a = p.parse_args()
steps = open(a.base).read().strip().split(',')
MIX = ['CIRCLE', 'CIRCLE', 'RIGHT*18', 'CIRCLE', 'SQUARE', 'CROSS', 'CIRCLE', 'TRIANGLE', 'CIRCLE', 'R2*18', 'R1',
       'SQUARE', 'LEFT*18', 'CIRCLE', 'CROSS CIRCLE', 'TRIANGLE CIRCLE', 'L1', 'SQUARE', 'RIGHT*30', 'CIRCLE']
out = []; fights = {}
for s in steps:
    at, words = s.split(':', 1)
    if at.startswith('@'):
        n, off = at[1:].split('+'); fights.setdefault(n, []).append(int(off))
        if not a.mix: out.append(s)
        continue
    if a.char and int(at) == a.char_at: words = a.char + ' ' + words
    out.append(f'{at}:{words}')
if a.mix:
    for n, offs in fights.items():
        # (the HL_CPU_HP pokes and the base's own steps keep their span; a word whose hold runs past 12 vsyncs takes its time)
        v = min(offs); i = 0
        while v <= max(offs):
            w = MIX[i % len(MIX)]; out.append(f'@{n}+{v}:{w}')
            hold = max([int(x.split('*')[1]) for x in w.split() if '*' in x] or [6])
            v += max(12, hold + 6) * len(w.split()); i += 1
print(','.join(out))
