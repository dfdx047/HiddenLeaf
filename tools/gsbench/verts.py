import struct, sys
exec(open(sys.argv[0].replace('verts','capcmp')).read().split('PR=')[0])
def verts(pkts, want):
    reg={}; prim=0; out=[]; cur={}
    for path,b in pkts:
        q=0
        while q+16<=len(b):
            lo,hi=struct.unpack_from('<QQ',b,q); q+=16
            nloop=lo&0x7fff; pre=(lo>>46)&1; flg=(lo>>58)&3; nreg=(lo>>60)&0xf or 16
            if pre: prim=(lo>>47)&0x7ff
            regs=[(hi>>(4*i))&0xf for i in range(nreg)]
            if flg==0:
                for _ in range(nloop):
                    for r in regs:
                        if q+16>len(b): break
                        a,c=struct.unpack_from('<QQ',b,q); q+=16
                        if r==0xe:
                            reg[c&0xff]=a
                            if (c&0xff)==0: prim=a&0x7ff
                        elif r==0: prim=a&0x7ff
                        elif r==1: cur['rgba']=(a&0xff,(a>>32)&0xff,c&0xff,(c>>32)&0xff)
                        elif r==2: cur['st']=struct.unpack('<ff',struct.pack('<Q',a)); cur['q']=struct.unpack('<f',struct.pack('<I',c&0xffffffff))[0]
                        elif r in (4,5):
                            at = prim if (reg.get(0x1a,1)&1) else ((reg.get(0x1b,0)&0x7f8)|(prim&7)); cx=(at>>9)&1
                            tbp=reg.get(6+cx,0)&0x3fff
                            if tbp in want:
                                z=(c>>4)&0xffffff if r==4 else c&0xffffffff
                                out.append((tbp,(a&0xffff)/16,((a>>32)&0xffff)/16,z,cur.get('rgba'),tuple(round(x,4) for x in cur.get('st',(0,0))),round(cur.get('q',0),4), 'nk' if (c>>47)&1 else ''))
            elif flg==1:
                q+=nloop*nreg*8 + (8 if (nloop*nreg)&1 else 0)
            else: q+=nloop*16
    return out
A=load(sys.argv[1]); B=load(sys.argv[2]); i=int(sys.argv[3]); j=int(sys.argv[4]); want={int(x) for x in sys.argv[5].split(',')}
va=verts(A[i],want); vb=verts(B[j],want)
print(len(va),len(vb))
for k in range(min(len(va),len(vb),int(sys.argv[6]) if len(sys.argv)>6 else 12)):
    print('P',va[k]); print('R',vb[k])
