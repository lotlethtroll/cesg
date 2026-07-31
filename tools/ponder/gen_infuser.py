"""Build the Ender Infuser ponder schematic from the hand-built capture.

The scene previously reused the Shulker Loader's schematic, so it narrated an infuser
over a completely unrelated workshop. This takes the purpose-built capture in
run/schematics/ender_infuser.nbt -- fluid tank -> pump -> smart pipe -> infuser ->
funnel -> barrel, with a gearbox drive train -- and converts it to the same convention
as the other scenes: square base plate at y=0, structure starting at y=1, centred.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import read_structure, write_structure, TypedList, Tag, COMP, i, pos

ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
SRC = os.path.join(ROOT, 'run/schematics/ender_infuser.nbt')
DST = os.path.join(ROOT, 'src/main/resources/assets/cesg/ponder/ender_infuser.nbt')


def palette_key(entry):
    props = entry.get('Properties', {})
    return (entry['Name'], tuple(sorted((k, v) for k, v in props.items())))


root = read_structure(SRC)
sx, sy, sz = [t.v for t in root['size']]
palette = list(root['palette'])
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

n = max(sx, sz)
dx, dz = (n - sx) // 2, (n - sz) // 2

OPPOSITE = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


def rotated_state(idx):
    """Palette entry for this state turned 180 degrees about Y.

    Only horizontal `facing` flips: an `axis` is preserved by a half turn, and up/down facings
    (the downward output pump, the barrel) are unaffected.
    """
    e = palette[idx]
    props = e.get('Properties')
    if not props or props.get('facing') not in OPPOSITE:
        return idx
    turned = dict(props)
    turned['facing'] = OPPOSITE[props['facing']]
    return intern(e['Name'], **turned)


# The capture faces away from Ponder's camera, so turn the whole structure to put the Infuser's
# working face toward the viewer. Positions mirror within the square plate.
shifted = []
for b in root['blocks']:
    x, y, z = [t.v for t in b['pos']]
    nx, nz = (n - 1) - (x + dx), (n - 1) - (z + dz)
    nb = {'pos': pos(nx, y + 1, nz), 'state': i(rotated_state(b['state'].v))}   # +1: sit on the plate
    if 'nbt' in b:
        nb['nbt'] = b['nbt']
    shifted.append(nb)

plate = [{'pos': pos(x, 0, z), 'state': i(white if (x + z) % 2 == 0 else snow)}
         for x in range(n) for z in range(n)]

root['size'] = pos(n, sy + 1, n)
root['palette'] = TypedList(palette, COMP)
root['blocks'] = TypedList(plate + shifted, COMP)
write_structure(DST, root)

print('ender_infuser [%d,%d,%d] -> [%d,%d,%d]  dx=+%d dz=+%d dy=+1  plate %dx%d'
      % (sx, sy, sz, n, sy + 1, n, dx, dz, n, n))
print('new positions:')
pal = []
for e in palette:
    nm = e['Name']
    p = e.get('Properties')
    if p:
        nm += '[' + ','.join('%s=%s' % kv for kv in sorted(p.items())) + ']'
    pal.append(nm)
for b in sorted(shifted, key=lambda b: (b['pos'][1].v, b['pos'][0].v, b['pos'][2].v)):
    x, y, z = [t.v for t in b['pos']]
    print('   (%d,%d,%d) %s' % (x, y, z, pal[b['state'].v]))
