"""Add Create-style base plates to the four station schematics.

These were player-built captures with no y=0 layer at all (shulker_unloader had a single barrel), so
showBasePlate() revealed nothing and every structure floated. Ponder base plates are square, so each
footprint is padded to a square of its longest side and the structure is shifted to centre it; the
storyboard constants shift by the same dx/dz. All original block entity data is preserved verbatim.
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import read_structure, write_structure, TypedList, Tag, INT, COMP, i, pos

PD = os.path.join(os.path.normpath(os.path.join(HERE, '..', '..')),
                  'src/main/resources/assets/cesg/ponder')
STATIONS = ['shulker_loader', 'shulker_unloader', 'shulker_belt_loader', 'shulker_belt_unloader']


def palette_key(entry):
    props = entry.get('Properties', {})
    return (entry['Name'], tuple(sorted((k, v) for k, v in props.items())))


for name in STATIONS:
    root = read_structure('%s/%s.nbt' % (PD, name))
    sx, sy, sz = [t.v for t in root['size']]
    palette = list(root['palette'])

    n = max(sx, sz)
    dx, dz = (n - sx) // 2, (n - sz) // 2

    index = {palette_key(e): k for k, e in enumerate(palette)}

    def intern(nm, **props):
        entry = {'Name': nm}
        if props:
            entry['Properties'] = dict(sorted(props.items()))
        key = palette_key(entry)
        if key not in index:
            index[key] = len(palette)
            palette.append(entry)
        return index[key]

    white = intern('minecraft:white_concrete')
    snow = intern('minecraft:snow_block')

    shifted = []
    for b in root['blocks']:
        x, y, z = [t.v for t in b['pos']]
        nb = {'pos': pos(x + dx, y, z + dz), 'state': b['state']}
        if 'nbt' in b:
            nb['nbt'] = b['nbt']
        shifted.append(nb)

    # Keep any pre-existing y=0 content (the unloader's barrel); the plate fills around it.
    taken = {(t[0].v, t[2].v) for t in (b['pos'] for b in shifted) if t[1].v == 0}
    plate = []
    for x in range(n):
        for z in range(n):
            if (x, z) in taken:
                continue
            plate.append({'pos': pos(x, 0, z), 'state': i(white if (x + z) % 2 == 0 else snow)})

    root['size'] = pos(n, sy, n)
    root['palette'] = TypedList(palette, COMP)
    root['blocks'] = TypedList(plate + shifted, COMP)
    write_structure('%s/%s.nbt' % (PD, name), root)
    print('%-22s [%d,%d,%d] -> [%d,%d,%d]  dx=+%d dz=+%d  plate %dx%d (%d cells, %d kept)'
          % (name, sx, sy, sz, n, sy, n, dx, dz, n, n, len(plate), len(taken)))
