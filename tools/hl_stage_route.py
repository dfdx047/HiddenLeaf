#!/usr/bin/env python3
# hl_stage_route.py BASE STAGE: the v3w route (build/pcsx2-bench runs/route_v3w.txt) with the stage select moved STAGE times
# down (0 = Hidden Leaf Village, the first of 24). The absolute steps after the stage select move later by the time the moves take.
import sys
base=open(sys.argv[1]).read().strip().split(',')
k=int(sys.argv[2]); sel=4715; gap=24
out=[]
for s in base:
    at,words=s.split(':',1)
    if at.startswith('@'): out.append(s); continue
    v=int(at)
    if v==sel:
        for i in range(k): out.append(f'{sel+i*gap}:DOWN')
        out.append(f'{sel+k*gap}:{words}')
    elif v>sel: out.append(f'{v+k*gap}:{words}')
    else: out.append(s)
print(','.join(out))
