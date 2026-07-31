"""Repair belt block-entity NBT in the station schematics.

These schematics were captured from a live world, so every belt carries the world
coordinates it had there: `Controller` points at e.g. [7,64,16], which does not exist
in the ponder scene, and `Source` names a kinetic neighbour that is equally absent. A
belt whose controller cannot be resolved never runs its transport logic, so items
placed on it just sit there. Two schematics also shipped real items baked into a belt's
Inventory, which then appear in the scene as stuck cargo nobody created.

Rewrites `Controller` to the run's controller position in SCHEMATIC coordinates (which
are the ponder world's coordinates), drops the stale `Source`, and empties any saved
belt inventory. Idempotent.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import read_structure, write_structure, Tag

PD = os.path.join(os.path.normpath(os.path.join(HERE, '..', '..')),
                  'src/main/resources/assets/cesg/ponder')
INT_LIST = 11


def fix(path):
    root = read_structure(path)
    pal = [e['Name'] for e in root['palette']]
    belts = [b for b in root['blocks']
             if 'nbt' in b and pal[b['state'].v] == 'create:belt']
    if not belts:
        return None

    # Group each run by the stale controller it currently names, then find that run's
    # own controller segment and use its real position.
    runs = {}
    for b in belts:
        runs.setdefault(tuple(b['nbt']['Controller'].v), []).append(b)

    changes = []
    for stale, members in runs.items():
        head = [m for m in members if m['nbt'].get('IsController')
                and m['nbt']['IsController'].v == 1]
        if len(head) != 1:
            changes.append('  !! %d controllers for run %s -- left alone' % (len(head), list(stale)))
            continue
        real = [t.v for t in head[0]['pos']]
        for m in members:
            m['nbt']['Controller'] = Tag(INT_LIST, list(real))
            m['nbt'].pop('Source', None)
            inv = m['nbt'].get('Inventory')
            if inv is not None and inv.get('Items'):
                changes.append('  cleared %d baked item(s) from belt at %s'
                               % (len(inv['Items']), [t.v for t in m['pos']]))
                inv['Items'] = []
        changes.append('  run %s -> Controller %s (%d segments)' % (list(stale), real, len(members)))

    write_structure(path, root)
    return changes


for f in sorted(os.listdir(PD)):
    if not f.endswith('.nbt'):
        continue
    out = fix(os.path.join(PD, f))
    if out:
        print('--- %s' % f)
        print('\n'.join(out))
