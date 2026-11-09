import numpy as np, sys
st=open(sys.argv[1],'rb').read()   # pg2.cap.state
off=int(sys.argv[3]) if len(sys.argv)>3 else 425
P=np.frombuffer(st[off:off+4<<20],dtype='<u4'); R=np.fromfile(sys.argv[2],dtype='<u4')
# block = 256 bytes = 64 words; page = 32 blocks (8 KB)
B=64
print('page(tbp)  low24-diff  high8-diff  P-high-nonzero  R-high-nonzero')
for pg in range(512):
    s=slice(pg*32*B,(pg+1)*32*B)
    p,r=P[s],R[s]
    lo=int(((p^r)&0xffffff).astype(bool).sum()); hi=int(((p^r)>>24).astype(bool).sum())
    if hi>64 or (lo>64 and pg<0x100):
        print(f'{pg:#05x}({pg*32:5d}) {lo:6d} {hi:6d} {int((p>>24).astype(bool).sum()):6d} {int((r>>24).astype(bool).sum()):6d}')
