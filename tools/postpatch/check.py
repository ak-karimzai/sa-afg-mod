"""usage: PYTHONPATH=. python3 check.py mNN [mNN...]   -> assembles work/mNN.txt, lints. exit 1 on error"""
import sys,re,struct
from scm import CMD,dis; from asm import assemble,TOK
bad=0
for n in sys.argv[1:]:
    t=open('work/%s.txt'%n).read()
    try: b=assemble(t)
    except Exception as e: print(n,'ASSEMBLE ERROR:',e); bad=1; continue
    # re-disassemble to prove well-formed bytecode
    try: ins=dis(b,0,len(b))
    except Exception as e: print(n,'DISASSEMBLE ERROR:',e); bad=1; continue
    errs=[];warns=[]
    # lvar range
    mx=max([int(m) for m in re.findall(r'(?<![\w$])(\d+)@',t)]+[0])
    if mx>=1024: errs.append('lvar %d@ >= 1024'%mx)
    # backward-jump loops need a WAIT inside (else the game freezes)
    lines=[l.split(';')[0].strip() for l in t.split('\n')]
    lines=[l for l in lines if l]
    lab={};k=0;seq=[]
    for l in lines:
        if re.fullmatch(r'L\w+:',l): lab[l[:-1]]=len(seq)
        else: seq.append(l)
    for i,l in enumerate(seq):
        m=re.match(r'(NOT )?(0002|004C|004D) \S+ (L\w+)',l)
        if m and lab.get(m.group(3),1e9)<=i:
            body=seq[lab[m.group(3)]:i+1]
            if not any(re.match(r'0001 ',x) or ' 0058 ' in (' '+x) or 'ADD_VAL_TO_INT_LVAR' in x for x in body):
                # a GOSUB'd wait is allowed; warn only
                warns.append('loop %s..%d has no WAIT in body (check GOSUBs inside): %s'%(m.group(3),i,l))
    # operand type checks against the opcode database
    for st,op,neg,ps,nm in ins:
        cm=CMD[op]; inp=cm.get('input',[]); out=cm.get('output',[])
        for j,p in enumerate(ps):
            if j<len(inp):
                if inp[j]['type']=='float' and p[0] in('i','b','w'): errs.append('%s param %d must be float or var, got int literal %s (write 1.0f not 1)'%(nm,j,p[1]))
                if inp[j]['type'] in('int','Char','Car','Player','Object','bool') and p[0]=='f': errs.append('%s param %d must be int/handle, got float literal'%(nm,j))
            elif j-len(inp)<len(out) and p[0] in('i','b','w','f','s8','s16','sv'): errs.append('%s output param %d must be a variable'%(nm,j))
    # a mission must be able to end
    if '0318' not in t and 'REGISTER_MISSION_PASSED' not in t: warns.append('no REGISTER_MISSION_PASSED')
    if 'TERMINATE_THIS_SCRIPT' not in t: errs.append('no TERMINATE_THIS_SCRIPT')
    print(n,'bytes=%d'%len(b),'ERRORS:' if errs else 'ok',errs,'warnings:',warns[:6] if warns else 'none')
    if errs: bad=1
sys.exit(bad)
