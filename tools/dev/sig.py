#!/usr/bin/env python3
"""Print gta3sc (SA) command signatures: dev/sig.py NAME_SUBSTRING..."""
import os, sys
import xml.etree.ElementTree as ET
base = os.path.join(os.path.dirname(__file__), "..", "vendor", "gta3sc", "config", "gtasa")
for fn in ("commands.xml", "cleo.xml"):
    for c in ET.parse(os.path.join(base, fn)).getroot().iter("Command"):
        n = c.get("Name")
        if any(q.upper() in n for q in sys.argv[1:]):
            args = [a.get("Type") + (":" + a.get("Enum") if a.get("Enum") else "") + ("[out]" if a.get("Out") == "true" else "")
                    for a in c.iter("Arg")]
            print(f"{c.get('ID'):6s} {n} {' '.join(args)}")
