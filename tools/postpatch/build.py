import os
"""usage: PYTHONPATH=. python3 build.py OUT.scm   (assembles work/m00..m23 into a new main.scm, original main region untouched)"""
import sys,struct,glob
from asm import assemble
M=os.environ.get('AFG_ORIG_SCM','main.scm.orig')
d=bytearray(open(M,'rb').read())
mainsize=struct.unpack_from('<I',d,5508+8)[0]
blobs=[assemble(open('work/m%02d.txt'%i).read()) for i in range(24)]
out=bytearray(d[:mainsize]); offs=[]
for b in blobs: offs.append(len(out)); out+=b
for i,o in enumerate(offs): struct.pack_into('<I',out,5508+8+16+4*i,o)
struct.pack_into('<I',out,5508+8+4,max(len(b) for b in blobs))
open(sys.argv[1],'wb').write(out)
print('wrote',sys.argv[1],len(out),'bytes; largest mission',max(len(b) for b in blobs),'; total delta',len(out)-len(d))
