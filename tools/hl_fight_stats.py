#!/usr/bin/env python3
# hl_fight_stats.py RUN...: build/lab/mia3/<RUN>/ perf lines in windows relative to fight 1 (A = f+0..440, B = f+460..2400)
import sys,re
# fight-relative windows: A = f+0..f+440 (the fight before the CPU's health drops), B = f+460..f+2400
print(f"{'run':16s} {'f1':>5s} {'A fps':>6s} {'A min':>6s} {'A EE':>5s} {'A IOP':>5s} {'A VU1':>5s} {'B fps':>6s} {'B min':>6s} {'B EE':>5s} {'B IOP':>5s} {'B VU1':>5s}")
for t in sys.argv[1:]:
    log=open(f'mia3/{t}/log.txt',errors='ignore').read()
    m=re.search(r'\[route\] v(\d+) fight 1 begins',log)
    if not m: print(t,'no fight'); continue
    f=int(m[1]); rows=open(f'mia3/{t}/perf.txt').read().splitlines()
    def st(lo,hi):
        v=[];c=[]
        for l in rows:
            mm=re.match(r'\[perf\] v(\d+) ([\d.]+) fps \| EE ([\d.]+) VU0 [\d.]+ VU1 ([\d.]+) GS [\d.]+ IOP ([\d.]+)',l)
            if mm and f+lo<=int(mm[1])<f+hi: v.append(float(mm[2])); c.append((float(mm[3]),float(mm[5]),float(mm[4])))
        if not v: return '   -'*5
        n=len(c); return f"{sum(v)/len(v):6.1f} {min(v):6.1f} {sum(x[0] for x in c)/n:5.1f} {sum(x[1] for x in c)/n:5.1f} {sum(x[2] for x in c)/n:5.1f}"
    print(f"{t:16s} {f:5d} {st(0,440)} {st(460,2400)}")
