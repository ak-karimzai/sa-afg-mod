import os
import json,struct,sys
SA=os.environ.get('AFG_SA_JSON','cleo/.config/sa.json')
db=json.load(open(SA))
CMD={}
for ex in db['extensions']:
    if ex['name']!='default': continue
    for c in ex['commands']:
        CMD[int(c['id'],16)]=c
def rd(d,o):
    """read one param -> (kind,val,newo)"""
    t=d[o]; o+=1
    if t==1: return ('i',struct.unpack_from('<i',d,o)[0],o+4)
    if t==2: return ('g',struct.unpack_from('<H',d,o)[0],o+2)
    if t==3: return ('l',struct.unpack_from('<H',d,o)[0],o+2)
    if t==4: return ('b',struct.unpack_from('<b',d,o)[0],o+1)
    if t==5: return ('w',struct.unpack_from('<h',d,o)[0],o+2)
    if t==6: return ('f',struct.unpack_from('<f',d,o)[0],o+4)
    if t==9: return ('s8',d[o:o+8],o+8)
    if t==0xA: return ('gA',d[o:o+6],o+6)
    if t==0xB: return ('lA',d[o:o+6],o+6)
    if t==0xE:
        n=d[o]; return ('sv',d[o+1:o+1+n],o+1+n)
    if t==0xF: return ('s16',d[o:o+16],o+16)
    if t==7: return ('gA',d[o:o+6],o+6)
    if t==8: return ('lA',d[o:o+6],o+6)
    raise ValueError('bad type %d at %d'%(t,o-1))
def dis(d,start,end=None):
    o=start; out=[]
    end=end or len(d)
    while o<end:
        op=struct.unpack_from('<H',d,o)[0]; neg=bool(op&0x8000); op&=0x7fff
        st=o; o+=2
        c=CMD.get(op)
        ps=[]
        n=c['num_params'] if c else None
        if n is None: raise ValueError('unknown opcode %04X at %d'%(op,st))
        if n==-1:
            while d[o]!=0:
                k,v,o=rd(d,o); ps.append((k,v))
            o+=1
        else:
            for _ in range(n):
                k,v,o=rd(d,o); ps.append((k,v))
        out.append((st,op,neg,ps,c['name']))
    return out
def fmt(p):
    k,v=p
    if k in('s8','s16','sv'): return repr(bytes(v).split(b'\0')[0].decode('latin1'))
    if k=='f': return '%.4gf'%v
    if k=='g': return '$%d'%(v//4) if v%4==0 else '$@%d'%v
    if k=='l': return '%d@'%v
    return str(v)
def show(ins):
    for st,op,neg,ps,nm in ins:
        print('%6d: %s%04X %s %s'%(st,'NOT ' if neg else '',op,nm,' '.join(fmt(p) for p in ps)))
if __name__=='__main__':
    d=open(sys.argv[1],'rb').read()
    a=int(sys.argv[2]); b=int(sys.argv[3]) if len(sys.argv)>3 else None
    show(dis(d,a,b))
