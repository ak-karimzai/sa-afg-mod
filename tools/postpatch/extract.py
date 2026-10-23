import os
import struct,re
from scm import *; from asm import *
M=os.environ.get('AFG_ORIG_SCM','main.scm.orig')
d=open(M,'rb').read()
offs=[struct.unpack_from('<I',d,5508+8+16+4*i)[0] for i in range(24)]+[len(d)]
fxt=dict(l.split(' ',1) for l in open(os.environ.get('AFG_FXT','afghan.fxt'),encoding='latin1').read().split('\n') if ' ' in l and not l.startswith('#'))
idx=[]
for i in range(24):
    ins=dis(d,offs[i],offs[i+1]); t=to_text(ins,offs[i])
    nm=[p for p in ins if p[4]=='SCRIPT_NAME'][0][3][0][1]; nm=bytes(nm).split(b'\0')[0].decode()
    tt=re.findall(r'PRINT_BIG s8"(\w+_T)"',t)
    title=fxt.get(tt[0],'?').strip() if tt else '?'
    n='m%02d'%i
    open('work/%s.txt'%n,'w').write('; mission index %d  script %s  title "%s"\n'%(i,nm,title)+t)
    open('orig/%s.txt'%n,'w').write(t)
    idx.append('%s  idx=%d script=%s title=%s bytes=%d instr=%d'%(n,i,nm,title,offs[i+1]-offs[i],len(ins)))
open('INDEX.txt','w').write('\n'.join(idx)+'\n')
print('\n'.join(idx))
