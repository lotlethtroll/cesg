"""Build the Flux Battery array schematic from REAL captured multiblocks.

The scene used to assemble arrays by hand -- setBlocks, then modifyBlockEntity to write each
cell's controller. That never rendered connected, because the connected-texture behaviour asks
ConnectivityHandler.isConnected, which simply compares the two block entities' controllers, and
the hand-written controllers are not in place when the section mesh bakes.

A genuine capture sidesteps the whole problem: every cell already carries the same Controller
value, so the equality test succeeds. The captured coordinates are stale world positions, which
does not matter -- only that neighbours agree, and they do.

Lays a lone battery, a formed 2x2x2 and a formed 3x3x3 side by side on one square plate, and
rewrites each array's Controller to its own controller cell so the three stay distinct.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import read_structure, write_structure, TypedList, Tag, COMP, i, pos

ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
SRC = os.path.join(ROOT, 'run/schematics')
DST = os.path.join(ROOT, 'src/main/resources/assets/cesg/ponder/gateway_flux_battery_array.nbt')
INT_LIST = 11

N = 8                     # square plate; 1 + gap + 2 + gap + 3 across
SINGLE_AT = (0, 1, 3)
TWO_AT = (2, 1, 3)
THREE_AT = (5, 1, 2)

palette, index, blocks = [], {}, []


def palette_key(entry):
    return (entry['Name'], tuple(sorted(entry.get('Properties', {}).items())))


def intern(entry):
    key = palette_key(entry)
    if key not in index:
        index[key] = len(palette)
        palette.append(entry)
    return index[key]


def place(src_name, origin):
    """Copy a captured array in at `origin`, re-homing its Controller to the new position."""
    r = read_structure(os.path.join(SRC, src_name))
    src_pal = list(r['palette'])
    ox, oy, oz = origin
    # The controller cell is the one carrying Width/Height rather than a Controller pointer.
    head = None
    for b in r['blocks']:
        d = b.get('nbt') or {}
        if 'Width' in d and 'Controller' not in d:
            head = [t.v for t in b['pos']]
    if head is None:
        raise SystemExit('%s: no controller cell found' % src_name)
    real = [head[0] + ox, head[1] + oy, head[2] + oz]
    for b in r['blocks']:
        x, y, z = [t.v for t in b['pos']]
        nb = {'pos': pos(x + ox, y + oy, z + oz), 'state': i(intern(src_pal[b['state'].v]))}
        if 'nbt' in b:
            d = dict(b['nbt'])
            if 'Controller' in d:
                d['Controller'] = Tag(INT_LIST, list(real))
            nb['nbt'] = d
        blocks.append(nb)
    return real


place('2_by_2.nbt', TWO_AT)
place('3_by_3.nbt', THREE_AT)

# A lone battery is its own controller and needs no wiring -- the blockstate alone renders it.
blocks.append({'pos': pos(*SINGLE_AT),
               'state': i(intern({'Name': 'cesg:gateway_flux_battery',
                                  'Properties': {'bottom': 'true', 'shape': 'window', 'top': 'true'}}))})

white = intern({'Name': 'minecraft:white_concrete'})
snow = intern({'Name': 'minecraft:snow_block'})
plate = [{'pos': pos(x, 0, z), 'state': i(white if (x + z) % 2 == 0 else snow)}
         for x in range(N) for z in range(N)]

root = {'size': pos(N, 4, N), 'palette': TypedList(palette, COMP),
        'blocks': TypedList(plate + blocks, COMP), 'entities': TypedList([], COMP),
        'DataVersion': Tag(3, 3955)}
write_structure(DST, root)
print('gateway_flux_battery_array [%d,4,%d]  single=%s  2x2x2=%s  3x3x3=%s  (%d blocks)'
      % (N, N, SINGLE_AT, TWO_AT, THREE_AT, len(plate) + len(blocks)))
