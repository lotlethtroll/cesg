"""Re-author cross_dimensional_gateway_core.nbt and gateway_flux_battery_array.nbt.

The old gateway schematic was captured from a live world, so it shipped a fully fuelled, lit, bound,
portal-open gateway (core BE held Eye:4000 plus stale FrameLit/Portal/Partner position lists) and its
frames predated the link_* conduit properties, so no conduit ever rendered. It also had no y=0 base
plate. This rebuilds it in the "cold" state with correct conduit links, so the storyboard can drive
the real fuel visuals.
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtwrite import Palette, checkerboard, write_structure

OUT = os.path.join(os.path.normpath(os.path.join(HERE, '..', '..')),
                   'src/main/resources/assets/cesg/ponder')

DIRS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0),
        'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}

# ---------------------------------------------------------------- gateway ring
# Ring plane sits at x=1 (attachments go in the x=2 column), base plate at y=0, ring y=1..5.
RX = 1
CORE = (RX, 5, 0)
FRAMES = [
    (RX, 1, 0), (RX, 1, 1), (RX, 1, 2), (RX, 1, 3),   # bottom row
    (RX, 2, 0), (RX, 2, 3), (RX, 3, 0), (RX, 3, 3),   # uprights
    (RX, 4, 0), (RX, 4, 3),
    (RX, 5, 1), (RX, 5, 2), (RX, 5, 3),               # top row (core closes it at z=0)
]
INTERIOR = [(RX, y, z) for y in (2, 3, 4) for z in (1, 2)]

pal = Palette()
blocks = checkerboard(pal, 4, 4)

frame_set = set(FRAMES)
for pos in FRAMES:
    links = {}
    for name, (dx, dy, dz) in DIRS.items():
        n = (pos[0] + dx, pos[1] + dy, pos[2] + dz)
        links['link_' + name] = 'core' if n == CORE else 'frame' if n in frame_set else 'none'
    blocks.append({'pos': list(pos),
                   'state': pal.id('cesg:gateway_frame', lit=False, fuel='none', **links),
                   'nbt': {'id': 'cesg:gateway_frame'}})

# Core: cold and unbound. The storyboard fills the tanks and lights the ring itself.
blocks.append({'pos': list(CORE),
               'state': pal.id('cesg:cross_dimensional_gateway_core', facing='west', lit=False, fuel='none'),
               'nbt': {'id': 'cesg:cross_dimensional_gateway_core'}})
# Drive shaft into the core, and the pump/cogwheel/tank that supply fuel.
blocks.append({'pos': [RX + 1, 5, 0], 'state': pal.id('create:shaft', axis='x', waterlogged=False),
               'nbt': {'id': 'create:shaft'}})
blocks.append({'pos': [RX, 6, 3], 'state': pal.id('create:mechanical_pump', facing='down', waterlogged=False),
               'nbt': {'id': 'create:mechanical_pump'}})
blocks.append({'pos': [RX + 1, 6, 3], 'state': pal.id('create:cogwheel', axis='y', waterlogged=False),
               'nbt': {'id': 'create:cogwheel'}})
blocks.append({'pos': [RX, 7, 3],
               'state': pal.id('create:fluid_tank', bottom=True, top=True, shape='window'),
               'nbt': {'id': 'create:fluid_tank', 'Size': 1, 'Height': 1, 'Window': 1, 'Luminosity': 0,
                       'TankContent': {'Fluid': {'amount': 8000, 'id': 'cesg:liquid_eye_of_ender'}}}})

# Fuel feed for the Flux Battery scene, stacked in the x=2 column: tank -> downward pump -> battery slot
# at (2,1,0). The network scenes overwrite all three cells with Bridge/Controller/Terminal and the Core
# scene clears them, so this plumbing only ever shows in the battery scene.
blocks.append({'pos': [RX + 1, 2, 0],
               'state': pal.id('create:mechanical_pump', facing='down', waterlogged=False),
               'nbt': {'id': 'create:mechanical_pump'}})
blocks.append({'pos': [RX + 1, 3, 0],
               'state': pal.id('create:fluid_tank', bottom=True, top=True, shape='window'),
               'nbt': {'id': 'create:fluid_tank', 'Size': 1, 'Height': 1, 'Window': 1, 'Luminosity': 0,
                       'TankContent': {'Fluid': {'amount': 8000, 'id': 'cesg:liquid_eye_of_ender'}}}})

write_structure('%s/cross_dimensional_gateway_core.nbt' % OUT, [4, 8, 4], blocks, pal.entries)
print('gateway ring: size [4,8,4], %d blocks, %d palette entries' % (len(blocks), len(pal.entries)))
print('  interior left as air:', INTERIOR)

# ------------------------------------------------------- flux battery array
# A 5x5 plate with a full 3x3x3 array baked in, so sceneBounds reach y=3 and x/z=3. The storyboard
# clears it and rebuilds 1x1x1 -> 2x2x2 -> 3x3x3 from the same origin.
ORIGIN = (1, 1, 1)
W = H = 3

def shape_for(width, xo, zo):
    if width == 1:
        return 'window'
    if width == 2:
        return ('window_nw' if zo == 0 else 'window_sw') if xo == 0 else ('window_ne' if zo == 0 else 'window_se')
    return 'window' if abs(abs(xo) - abs(zo)) == 1 else 'plain'

pal2 = Palette()
blocks2 = checkerboard(pal2, 5, 5)
for yo in range(H):
    for xo in range(W):
        for zo in range(W):
            blocks2.append({
                'pos': [ORIGIN[0] + xo, ORIGIN[1] + yo, ORIGIN[2] + zo],
                'state': pal2.id('cesg:gateway_flux_battery', shape=shape_for(W, xo, zo),
                                 bottom=(yo == 0), top=(yo == H - 1)),
                'nbt': {'id': 'cesg:gateway_flux_battery'}})

write_structure('%s/gateway_flux_battery_array.nbt' % OUT, [5, 4, 5], blocks2, pal2.entries)
print('battery array: size [5,4,5], %d blocks, %d palette entries' % (len(blocks2), len(pal2.entries)))
