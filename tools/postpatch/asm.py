"""text <-> SCM mission code. Line: [NOT ]<hexop> NAME args ; comment.  Labels: 'Lname:' lines, label args are Lname.
args: int, 12@ (lvar) $N (gvar idx), 1.5f, s8"..", s16"..", sv"..", 37@[59@:11/f0] (lvar array: base@[idxvar:size/flags]), $37[$59:11/f0] ."""
import struct,re
from scm import CMD
def f32s(v):
    for p in range(1,10):
        s='%.*g'%(p,v)
        if struct.unpack('<f',struct.pack('<f',float(s)))[0]==v: return s
    return repr(v)
def fa(p,label=None):
    k,v=p
    if label is not None: return label
    if k=='i' or k=='b' or k=='w': return str(v)
    if k=='f': return f32s(v)+'f'
    if k=='g': return '$@%d'%v if v%4 else '$%d'%(v//4)
    if k=='l': return '%d@'%v
    if k=='s8': return 's8"%s"'%bytes(v).split(b'\0')[0].decode('latin1')
    if k=='s16': return 's16"%s"'%bytes(v).split(b'\0')[0].decode('latin1')
    if k=='sv': return 'sv"%s"'%bytes(v).decode('latin1')
    if k in('gA','lA'):
        off,idx,size,fl=struct.unpack('<HHBB',bytes(v))
        sig='$%d' if k=='gA' else '%d@'
        base=('$%d'%(off//4)) if k=='gA' else '%d@'%off
        ix=('$%d'%(idx//4)) if fl&0x80 else '%d@'%idx
        return '%s[%s:%d/f%d]'%(base,ix,size,fl&0x7f)
    raise ValueError(k)
def to_text(ins,base):
    tg=set()
    for st,op,neg,ps,nm in ins:
        inp=CMD[op].get('input',[])
        for j,p in enumerate(ps):
            if j<len(inp) and inp[j].get('type')=='label' and p[1]<0: tg.add(-p[1])
    L=[]
    for st,op,neg,ps,nm in ins:
        r=st-base
        if r in tg: L.append('L%d:'%r)
        inp=CMD[op].get('input',[])
        a=[]
        for j,p in enumerate(ps):
            lab=('L%d'%(-p[1])) if (j<len(inp) and inp[j].get('type')=='label' and p[1]<0) else None
            a.append(fa(p,lab))
        L.append('  %s%04X %s %s'%('NOT ' if neg else '',op,nm,' '.join(a)))
    return '\n'.join(L)+'\n'
TOK=re.compile(r'''s8"[^"]*"|s16"[^"]*"|sv"[^"]*"|[\$\d@]+\[[^\]]*\]|\S+''')
def enc_int(v,force=None):
    if force=='i': return b'\x01'+struct.pack('<i',v)
    if -128<=v<=127: return b'\x04'+struct.pack('<b',v)
    if -32768<=v<=32767: return b'\x05'+struct.pack('<h',v)
    return b'\x01'+struct.pack('<i',v)
def enc_arg(a,is_label,labels,cur):
    if is_label and re.fullmatch(r'L\w+',a):
        return ('label',a)
    if a.startswith('s8"'): return b'\x09'+a[3:-1].encode('latin1').ljust(8,b'\0')[:8]
    if a.startswith('s16"'): return b'\x0f'+a[4:-1].encode('latin1').ljust(16,b'\0')[:16]
    if a.startswith('sv"'): s=a[3:-1].encode('latin1'); return b'\x0e'+bytes([len(s)])+s
    m=re.fullmatch(r'(\$?)(\d+)(@?)\[(\$?)(\d+)(@?):(\d+)/f(\d+)\]',a)
    if m:
        gl=m.group(1)=='$'; off=int(m.group(2))*(4 if gl else 1); ig=m.group(4)=='$'
        idx=int(m.group(5))*(4 if ig else 1); fl=int(m.group(8))|(0x80 if ig else 0)
        return bytes([7 if gl else 8])+struct.pack('<HHBB',off,idx,int(m.group(7)),fl)
    if a.startswith('$@'): return b'\x02'+struct.pack('<H',int(a[2:]))
    if a.startswith('$'): return b'\x02'+struct.pack('<H',int(a[1:])*4)
    if a.endswith('@'): return b'\x03'+struct.pack('<H',int(a[:-1]))
    if a.endswith('f') and re.fullmatch(r'-?[\d.eE+-]+f',a): return b'\x06'+struct.pack('<f',float(a[:-1]))
    return enc_int(int(a),'i' if is_label else None)
def assemble(text):
    """returns bytes; labels are mission-local offsets (negative int32 operands)"""
    items=[]  # bytes or ('label',name) placeholders
    lines=[]
    for raw in text.split('\n'):
        l=raw.split(';')[0].strip() if '"' not in raw else raw.strip()
        if '"' in raw: # strip comment outside quotes
            out='';q=False
            for ch in raw:
                if ch=='"': q=not q
                if ch==';' and not q: break
                out+=ch
            l=out.strip()
        if l: lines.append(l)
    ops=[];pos=0;labels={}
    for l in lines:
        if re.fullmatch(r'L\w+:',l):
            labels[l[:-1]]=pos; continue
        neg=l.startswith('NOT '); 
        if neg: l=l[4:]
        t=TOK.findall(l); op=int(t[0],16); args=t[2:]
        c=CMD[op]; inp=c.get('input',[])
        buf=[struct.pack('<H',op|(0x8000 if neg else 0))]; size=2
        for j,a in enumerate(args):
            isl=j<len(inp) and inp[j].get('type')=='label'
            e=enc_arg(a,isl,labels,pos)
            buf.append(e); size+=5 if isinstance(e,tuple) else len(e)
        if c['num_params']==-1: buf.append(b'\0'); size+=1
        elif c['num_params']!=len(args): raise ValueError('param count %s: %s'%(c['name'],l))
        ops.append(buf); pos+=size
    out=b''
    for buf in ops:
        for e in buf:
            if isinstance(e,tuple):
                n=e[1]
                if n not in labels: raise ValueError('undefined label '+n)
                out+=b'\x01'+struct.pack('<i',-labels[n])
            else: out+=e
    return out
