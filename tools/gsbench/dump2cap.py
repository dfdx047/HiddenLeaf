import sys, struct
import os; sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import gsdump
s=gsdump.Stream(sys.argv[1]); h=gsdump.header(s)
out=open(sys.argv[2],'wb'); v=0
open(sys.argv[2]+'.state','wb').write(h['state'])
while True:
    t=s.read(1)
    if not t: break
    t=t[0]
    if t==0:
        p,n=struct.unpack('<BI',s.exact(5,'x')); d=s.exact(n,'d'); out.write(struct.pack('<II',p,n)); out.write(d)
    elif t==1: s.exact(1,'v'); v+=1; out.write(struct.pack('<II',0xFF,v))
    elif t==2: s.exact(4,'r')
    elif t==3: s.exact(0x2000,'r')
