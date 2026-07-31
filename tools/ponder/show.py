"""Dump every ponder schematic: size, base-plate coverage, and non-plate blocks."""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import read_structure

PD = os.path.join(os.path.normpath(os.path.join(HERE, '..', '..')),
                  'src/main/resources/assets/cesg/ponder')
only = sys.argv[1] if len(sys.argv) > 1 else None

for f in sorted(os.listdir(PD)):
    if only and only not in f:
        continue
    r = read_structure(os.path.join(PD, f))
    sx, sy, sz = [t.v for t in r['size']]
    pal = []
    for e in r['palette']:
        nm = e['Name'].replace('minecraft:', '')
        p = e.get('Properties')
        if p:
            nm += '[' + ','.join('%s=%s' % kv for kv in sorted(p.items())) + ']'
        pal.append(nm)
    plate = sum(1 for b in r['blocks']
                if b['pos'][1].v == 0 and ('concrete' in pal[b['state'].v] or 'snow' in pal[b['state'].v]))
    print('=' * 78)
    print('%s  size=[%d,%d,%d]  plate %d/%d cells at y=0' % (f, sx, sy, sz, plate, sx * sz))
    rows = []
    for b in r['blocks']:
        x, y, z = [t.v for t in b['pos']]
        n = pal[b['state'].v]
        if y == 0 and ('concrete' in n or 'snow' in n):
            continue
        rows.append(((y, z, x), '  (%d,%d,%d) %s%s' % (x, y, z, n, '  <BE>' if 'nbt' in b else '')))
    for _, line in sorted(rows):
        print(line)
