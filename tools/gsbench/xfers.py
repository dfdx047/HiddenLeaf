import struct, sys, collections
exec(open(sys.argv[0].replace('xfers','capcmp')).read().split('PR=')[0])
A=load(sys.argv[1]); c=collections.Counter()
for fi,pkts in enumerate(A[:int(sys.argv[2]) if len(sys.argv)>2 else 99]):
    reg={}
    for path,b in pkts:
        q=0
        while q+16<=len(b):
            lo,hi=struct.unpack_from('<QQ',b,q); q+=16
            nloop=lo&0x7fff; flg=(lo>>58)&3; nreg=(lo>>60)&0xf or 16
            regs=[(hi>>(4*i))&0xf for i in range(nreg)]
            if flg==0:
                for _ in range(nloop):
                    for r in regs:
                        if q+16>len(b): break
                        a,cc=struct.unpack_from('<QQ',b,q); q+=16
                        if r==0xe:
                            reg[cc&0xff]=a
                            if (cc&0xff)==0x53:
                                bb=reg.get(0x50,0); tr=reg.get(0x52,0); tp=reg.get(0x51,0)
                                c[(fi,a&3,(bb)&0x3fff,(bb>>24)&0x3f,(bb>>32)&0x3fff,(bb>>56)&0x3f,tr&0xfff,(tr>>32)&0xfff, tp&0x7ff,(tp>>16)&0x7ff,(tp>>32)&0x7ff,(tp>>48)&0x7ff)]+=1
            elif flg==1: q+=nloop*nreg*8+(8 if (nloop*nreg)&1 else 0)
            else: q+=nloop*16
ALL=len(sys.argv)>3
print('frame dir sbp spsm dbp dpsm w h  ssax ssay dsax dsay  count')
for k,n in sorted(c.items()):
    if ALL or k[5] in (0x1b,0x24,0x2c) or k[1]==2 or (3584<=k[4]<12000):
        print(*[hex(x) if i in (3,5) else x for i,x in enumerate(k)], n)
