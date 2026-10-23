"""usage: python3 ops.py <substring-or-hex> [more...]   look up SA opcodes (default set only): id, NAME, inputs/outputs, description"""
import sys
from scm import db
for q in sys.argv[1:]:
    q=q.upper(); n=0
    for ex in db['extensions']:
        if ex['name']!='default': continue
        for c in ex['commands']:
            if q==c['id'] or q in c['name']:
                ins=', '.join('%s:%s'%(p.get('name',''),p['type']) for p in c.get('input',[]))
                out=', '.join('%s:%s'%(p.get('name',''),p['type']) for p in c.get('output',[]))
                print('%s %s  in[%s] out[%s]  -- %s'%(c['id'],c['name'],ins,out,c.get('short_desc','')))
                n+=1
                if n>40: break
