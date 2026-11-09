import struct, sys, collections
exec(open(sys.argv[0].replace('frames','capcmp')).read().split('PR=')[0])
A=load(sys.argv[1]); c=collections.OrderedDict()
for fi,pkts in enumerate(A):
    reg={}; prim=0
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
                        a,cc=struct.unpack_from('<QQ',b,q); q+=16
                        if r==0xe:
                            reg[cc&0xff]=a
                            if (cc&0xff)==0: prim=a&0x7ff
                        elif r==0: prim=a&0x7ff
                        elif r in (4,5): nv+=1
            elif flg==1:
                for _ in range(nloop):
                    for r in regs:
                        if r in (4,5): nv+=1
                q+=nloop*nreg*8+(8 if (nloop*nreg)&1 else 0)
            else: q+=nloop*16
            if nv:
                at = prim if (reg.get(0x1a,1)&1) else ((reg.get(0x1b,0)&0x7f8)|(prim&7)); cx=(at>>9)&1
                fr=reg.get(0x4c+cx,0); tex=reg.get(6+cx,0); tme=(at>>4)&1
                k=(fr&0x1ff,(fr>>16)&0x3f,(fr>>24)&0x3f,fr>>32,(tex&0x3fff) if tme else -1,((tex>>20)&0x3f) if tme else -1)
                e=c.setdefault(k,[fi,fi,0]); e[1]=fi; e[2]+=nv
print('fbp fbw psm fbmsk tbp tpsm : first last verts')
for k,e in c.items():
    if k[3]!=0 and k[3]!=0xff000000 or k[0]>=0x70:
        print(f'{k[0]:#x} {k[1]} {k[2]:#x} {k[3]:08x} {k[4]} {k[5]:#x} : {e}')
