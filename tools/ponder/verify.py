"""Static checks on the Ponder scenes — everything that can be proven without a dev client.

Run from anywhere:  python tools/ponder/verify.py

Checks, in order of how much pain each one has already saved:
  1. every registered storyboard resolves to a schematic file and an existing method
  2. every BlockPos a scene touches is inside that schematic's bounds (a position outside the
     footprint is set in the world but can never appear in a layersFrom selection, so it silently
     never renders — this is what broke the Bridge scene)
  3. every createItemOnBelt / createItemOnBeltLike / flapFunnel target is the right kind of block
     (a wrong target silently no-ops instead of crashing)
  4. every GATEWAY_RING entry is really a gateway frame
  5. each schematic has a y=0 base plate covering its footprint
  6. lang keys: header + text_1..N present for every scene id, no orphans, index tag covers all
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, HERE)
from nbtio import read_structure

PONDER = os.path.join(ROOT, 'src/main/resources/assets/cesg/ponder')
SCENES = os.path.join(ROOT, 'src/main/java/com/cesg/ponder/CESGPonderScenes.java')
PLUGIN = os.path.join(ROOT, 'src/main/java/com/cesg/ponder/CESGPonderPlugin.java')
LANG = os.path.join(ROOT, 'src/generated/resources/assets/cesg/lang/en_us.json')

BELT = ('create:belt',)
BELT_LIKE = ('create:chute', 'create:smart_chute', 'create:item_drain', 'create:depot')

read = lambda p: open(p, encoding='utf-8').read()
failures = []


def fail(msg):
    failures.append(msg)
    print('  FAIL  ' + msg)


# ---------------------------------------------------------------- load schematics
schematics = {}
for f in sorted(os.listdir(PONDER)):
    if not f.endswith('.nbt'):
        continue
    r = read_structure(os.path.join(PONDER, f))
    pal = [e['Name'] for e in r['palette']]
    grid = {tuple(t.v for t in b['pos']): pal[b['state'].v] for b in r['blocks']}
    schematics[f[:-4]] = (tuple(t.v for t in r['size']), grid)

scenes, plugin = read(SCENES), read(PLUGIN)
lang = json.loads(read(LANG))

# ---------------------------------------------------------------- parse the storyboards
consts = {n: tuple(map(int, v.split(','))) for n, v in
          re.findall(r'BlockPos (\w+) = new BlockPos\(([^)]+)\)', scenes)}
consts.update({n: consts[a] for n, a in re.findall(r'BlockPos (\w+) = (\w+);', scenes) if a in consts})

lines = scenes.splitlines()
bodies = {}
for i, line in enumerate(lines):
    m = re.match(r'    (?:public|private) static void (\w+)\(', line)
    if not m:
        continue
    body = []
    for nxt in lines[i + 1:]:
        if nxt == '    }':
            break
        body.append(nxt)
    bodies[m.group(1)] = '\n'.join(body)

RING = set(tuple(map(int, t.split(',')))
           for t in re.findall(r'new BlockPos\((\d+, \d+, \d+)\)',
                               scenes.split('GATEWAY_RING = {')[1].split('};')[0]))

regs = re.findall(r'addStoryBoard\(CESG\.id\("(\w+)"\),\s*CESG\.id\("(\w+)"\),\s*CESGPonderScenes::(\w+)',
                  plugin)


def scene_source(method):
    src = bodies.get(method, '')
    if 'storageNetwork(builder' in src:
        src += '\n' + bodies['storageNetwork']
    if 'primeGateway(scene' in src:
        src += '\n' + bodies['primeGateway']
    if 'formBatteryArray(scene' in src:
        src += '\n' + bodies['formBatteryArray']
    return src


print('1. registration: %d storyboards, %d distinct components'
      % (len(regs), len({c for c, _, _ in regs})))
for comp, schem, method in regs:
    if schem not in schematics:
        fail('%s -> missing schematic %s.nbt' % (comp, schem))
    if method not in bodies:
        fail('%s -> missing method %s' % (comp, method))

print('\n2. scene positions inside schematic bounds')
for comp, schem, method in regs:
    (sx, sy, sz), _ = schematics[schem]
    src = scene_source(method)
    used = {consts[n] for n in re.findall(r'\b([A-Z][A-Z0-9_]{2,})\b', src) if n in consts}
    used |= {tuple(map(int, t.split(','))) for t in re.findall(r'new BlockPos\((\d+,\s*\d+,\s*\d+)\)', src)}
    if 'GATEWAY_RING' in src:
        used |= RING
    if method == 'gatewayFluxBatteryArray':
        o = consts['ARRAY_ORIGIN']
        used |= {(o[0] + dx, o[1] + dy, o[2] + dz)
                 for dx in range(3) for dy in range(3) for dz in range(3)}
    oob = sorted(p for p in used if not (0 <= p[0] < sx and 0 <= p[1] < sy and 0 <= p[2] < sz))
    print('   %-30s %-30s x<%d y<%d z<%d  n=%2d' % (comp, schem, sx, sy, sz, len(used)))
    if oob:
        fail('%s: positions outside bounds: %s' % (comp, oob))

print('\n3. belt / belt-like / funnel targets are the right block type')
schem_of = {m: s for _, s, m in regs}


def check_target(method, grid, label, arg, allowed, what):
    """arg must name a BlockPos constant whose schematic block is one of `allowed`."""
    if arg not in consts:
        fail('%s: %s(%s) — not a BlockPos constant, cannot verify' % (method, label, arg))
        return
    pos = consts[arg]
    block = grid.get(pos, '<air>')
    if not any(block.startswith(a) for a in allowed) if allowed else 'funnel' not in block:
        fail('%s: %s(%s %s) -> %s (want %s)' % (method, label, arg, pos, block, what))
    else:
        print('   %-22s %-20s %-27s %-11s %s' % (method, label, arg, pos, block))


for method, schem in schem_of.items():
    _, grid = schematics[schem]
    src = bodies.get(method, '')
    for kind, arg in re.findall(r'createItemOn(Belt|BeltLike)\((\w+),', src):
        check_target(method, grid, 'createItemOn' + kind, arg,
                     BELT if kind == 'Belt' else BELT_LIKE, 'belt' if kind == 'Belt' else 'belt-like')
    for arg in re.findall(r'flapFunnel\((\w+),', src):
        check_target(method, grid, 'flapFunnel', arg, None, 'funnel')
    for arg in re.findall(r'removeItemsFromBelt\((\w+)\)', src):
        check_target(method, grid, 'removeItemsFromBelt', arg, BELT, 'belt')
    # beltItemConsumedAt(scene, insertAt, travel, arriveAt, funnel, stack, blocks) — the helper takes its
    # belt positions as parameters, so validate the call sites and that `blocks` matches the real distance.
    for call in re.findall(r'beltItemConsumedAt\(scene,\s*(\w+),\s*Direction\.(\w+),\s*(\w+),\s*'
                           r'(\w+),\s*[^;]*?,\s*(\d+)\)', src, re.S):
        insert_at, travel, arrive_at, funnel, blocks = call
        check_target(method, grid, 'consumedAt.insert', insert_at, BELT, 'belt')
        check_target(method, grid, 'consumedAt.arrive', arrive_at, BELT, 'belt')
        if funnel != 'null':
            check_target(method, grid, 'consumedAt.funnel', funnel, None, 'funnel')
        a, b = consts.get(insert_at), consts.get(arrive_at)
        if a and b:
            axis_delta = abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
            if axis_delta != int(blocks):
                fail('%s: beltItemConsumedAt blocks=%s but %s->%s is %d blocks apart (travel timing wrong)'
                     % (method, blocks, insert_at, arrive_at, axis_delta))
            else:
                print('   %-22s %-20s %s -> %s = %s blocks (%s)'
                      % (method, 'consumedAt.travel', insert_at, arrive_at, blocks, travel))

print('\n4. gateway ring integrity')
_, gw = schematics['cross_dimensional_gateway_core']
bad = sorted(p for p in RING if 'gateway_frame' not in gw.get(p, ''))
print('   %d frames in GATEWAY_RING' % len(RING))
if bad:
    fail('GATEWAY_RING entries that are not frames: %s' % bad)
for name in ('GATEWAY_CORE', 'GATEWAY_PUMP'):
    print('   %-14s %-10s -> %s' % (name, consts[name], gw.get(consts[name], '<air>')))
if 'cross_dimensional_gateway_core' not in gw.get(consts['GATEWAY_CORE'], ''):
    fail('GATEWAY_CORE is not the core block')

print('\n5. base plate coverage at y=0')
for name, ((sx, sy, sz), grid) in sorted(schematics.items()):
    plate = sum(1 for (x, y, z), b in grid.items()
                if y == 0 and ('white_concrete' in b or 'snow_block' in b))
    other = sum(1 for (x, y, z), b in grid.items()
                if y == 0 and 'white_concrete' not in b and 'snow_block' not in b)
    print('   %-32s [%d,%d,%d]  plate %d + %d other = %d of %d cells'
          % (name, sx, sy, sz, plate, other, plate + other, sx * sz))
    if plate + other < sx * sz:
        fail('%s: y=0 layer only covers %d of %d cells' % (name, plate + other, sx * sz))

print('\n6. lang keys')
titles = re.findall(r'scene\.title\("(\w+)"', scenes)
declared = {'cesg.ponder.tag.end_storage', 'cesg.ponder.tag.end_storage.description'}
for sid in titles:
    hdr = 'cesg.ponder.%s.header' % sid
    if hdr not in lang:
        fail('missing ' + hdr)
    declared.add(hdr)
    n = 1
    while 'cesg.ponder.%s.text_%d' % (sid, n) in lang:
        declared.add('cesg.ponder.%s.text_%d' % (sid, n))
        n += 1
    print('   %-32s header + text_1..%d' % (sid, n - 1))
orphans = [k for k in lang if k.startswith('cesg.ponder.') and k not in declared]
if orphans:
    fail('orphan lang keys: %s' % orphans)

tagged = set(re.findall(r'\.add\(CESG\.id\("(\w+)"\)\)', plugin))
comps = {c for c, _, _ in regs}
if comps - tagged:
    fail('components missing from index tag: %s' % sorted(comps - tagged))
if tagged - comps:
    fail('tagged but not registered: %s' % sorted(tagged - comps))
# Scene ids need not match component ids: a component with several scenes gives each one its own id
# so their titles and text resolve to separate lang keys (gateway_flux_battery has two). What must hold
# is that every registered storyboard method sets a title, and that no two methods share one.
titled = {m for m in bodies if re.search(r'scene\.title\("', bodies[m])} | {
    m for m in bodies if 'storageNetwork(builder' in bodies.get(m, '')}
for _, _, method in regs:
    if method not in titled:
        fail('storyboard %s never calls scene.title()' % method)
if len(titles) != len(set(titles)):
    dupes = sorted({t for t in titles if titles.count(t) > 1})
    fail('scene ids used by more than one storyboard: %s' % dupes)

print('\n' + ('=' * 60))
print('FAILURES: %d' % len(failures) if failures else 'all checks passed')
sys.exit(1 if failures else 0)
