"""Minimal writer for vanilla StructureTemplate NBT (gzipped, big-endian)."""
import gzip, struct

TAG_END, TAG_BYTE, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 8, 9, 10


def _s(f, text):
    b = text.encode('utf-8')
    f.write(struct.pack('>H', len(b)))
    f.write(b)


def _tag_of(v):
    if isinstance(v, bool):   return TAG_BYTE
    if isinstance(v, int):    return TAG_INT
    if isinstance(v, str):    return TAG_STRING
    if isinstance(v, dict):   return TAG_COMPOUND
    if isinstance(v, list):   return TAG_LIST
    raise TypeError(type(v))


def _payload(f, v):
    if isinstance(v, bool):
        f.write(struct.pack('>b', 1 if v else 0))
    elif isinstance(v, int):
        f.write(struct.pack('>i', v))
    elif isinstance(v, str):
        _s(f, v)
    elif isinstance(v, dict):
        for k, val in v.items():
            f.write(bytes([_tag_of(val)]))
            _s(f, k)
            _payload(f, val)
        f.write(bytes([TAG_END]))
    elif isinstance(v, list):
        et = _tag_of(v[0]) if v else TAG_END
        f.write(bytes([et]))
        f.write(struct.pack('>i', len(v)))
        for item in v:
            if _tag_of(item) != et:
                raise TypeError('heterogeneous list')
            _payload(f, item)
    else:
        raise TypeError(type(v))


def write_structure(path, size, blocks, palette, data_version=3955):
    """blocks: [{'pos':[x,y,z], 'state':int, 'nbt':{...}?}]; palette: [{'Name':str,'Properties':{...}?}]"""
    root = {
        'size': list(size),
        'entities': [],
        'blocks': blocks,
        'palette': palette,
        'DataVersion': data_version,
    }
    # mtime=0 keeps regeneration byte-identical, so re-running a generator produces no git diff.
    buf = gzip.GzipFile(path, 'wb', mtime=0)
    try:
        buf.write(bytes([TAG_COMPOUND]))
        _s(buf, '')
        _payload(buf, root)
    finally:
        buf.close()


class Palette:
    """Interns (name, properties) pairs into palette indices."""

    def __init__(self):
        self.entries = []
        self._index = {}

    def id(self, name, **props):
        props = {k: (v if isinstance(v, str) else ('true' if v is True else 'false' if v is False else str(v)))
                 for k, v in props.items()}
        key = (name, tuple(sorted(props.items())))
        if key not in self._index:
            entry = {'Name': name}
            if props:
                entry['Properties'] = dict(sorted(props.items()))
            self._index[key] = len(self.entries)
            self.entries.append(entry)
        return self._index[key]


def checkerboard(pal, size_x, size_z, y=0, offset_x=0, offset_z=0):
    """Create's ponder base plate: white_concrete on even (x+z), snow_block on odd."""
    white = pal.id('minecraft:white_concrete')
    snow = pal.id('minecraft:snow_block')
    out = []
    for x in range(offset_x, offset_x + size_x):
        for z in range(offset_z, offset_z + size_z):
            out.append({'pos': [x, y, z], 'state': white if (x + z) % 2 == 0 else snow})
    return out
