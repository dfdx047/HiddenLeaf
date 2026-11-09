import struct, sys, collections
def load(fn):
    d=open(fn,'rb').read(); p=0; vs=[]; cur=[]
    while p+8<=len(d):
        path,size=struct.unpack_from('<II',d,p); p+=8
        if path==0xFF: vs.append(cur); cur=[]; continue
        cur.append((path,d[p:p+size])); p+=size
    if cur: vs.append(cur)
    return [v for v in vs if v]
PR=['PT','LN','LS','TRI','TS','TF','SPR','?']
def stats(pkts):
    reg={}; prim=0; draws=collections.Counter(); tri=collections.Counter()
    def key():
        at = prim if (reg.get(0x1a,1)&1) else ((reg.get(0x1b,0)&0x7f8)|(prim&7))
        c=(at>>9)&1
        tme=(at>>4)&1; tex=reg.get(6+c,0)
        return (reg.get(0x4c+c,0)&0x1ff, (tex&0x3fff) if tme else -1, ((tex>>20)&0x3f) if tme else -1, ((tex>>37)&0x3fff) if tme else -1,
                PR[prim&7], (at>>6)&1, f'{reg.get(0x42+c,0)&0xff00000000ff:x}' if (at>>6)&1 else '-', f'{reg.get(0x47+c,0):x}', c, (at>>8)&1)
    for path,b in pkts:
        q=0
        while q+16<=len(b):
            lo,hi=struct.unpack_from('<QQ',b,q); q+=16
            nloop=lo&0x7fff; pre=(lo>>46)&1; flg=(lo>>58)&3; nreg=(lo>>60)&0xf or 16
            if pre: prim=(lo>>47)&0x7ff
            regs=[(hi>>(4*i))&0xf for i in range(nreg)]; nv=0
            if flg==0:
                for _ in range(nloop):
                    for r in regs:
                        if q+16>len(b): break
                        a,c=struct.unpack_from('<QQ',b,q); q+=16
                        if r==0xe:
                            reg[c&0xff]=a
                            if (c&0xff)==0: prim=a&0x7ff
                        elif r==0: prim=a&0x7ff
                        elif r in (4,5): nv+=1
            elif flg==1:
                for _ in range(nloop):
                    for r in regs:
                        if r in (4,5): nv+=1
                        q+=8
                if (nloop*nreg)&1: q+=8
            else: q+=nloop*16
            if nv: draws[key()]+=nv
    return draws
A=[stats(v) for v in load(sys.argv[1])]; B=[stats(v) for v in load(sys.argv[2])]
print('frames', len(A), len(B), [sum(x.values()) for x in A], [sum(x.values()) for x in B])
ka=set().union(*A); kb=set().union(*B)
print('fbp tbp psm cbp prim abe alpha test tme? fst')
print('--- only in', sys.argv[1]); 
for k in sorted(ka-kb, key=lambda k: -max(a[k] for a in A)): print(k, [a[k] for a in A])
print('--- only in', sys.argv[2])
for k in sorted(kb-ka, key=lambda k: -max(b[k] for b in B)): print(k, [b[k] for b in B])
if len(sys.argv)>3:
    want=[int(x) for x in sys.argv[3].split(',')]
    for name,F in ((sys.argv[1],A),(sys.argv[2],B)):
        print(name.split('/')[-1], [[ (k[1],v) for k,v in f.items() if k[1] in want] for f in F])
