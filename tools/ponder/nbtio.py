"""Type-preserving NBT read/write for vanilla StructureTemplate files (gzipped, big-endian).

Numeric tags are wrapped so a read->write round trip keeps Byte/Short/Int/Long/Float/Double and the
array tags exactly as they were. Plain python int/str/dict/list are still accepted when authoring new
data (int -> TAG_Int, str -> TAG_String).
"""
import gzip, struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BARR, STR, LIST, COMP, IARR, LARR = range(13)


class Tag:
    __slots__ = ('t', 'v')

    def __init__(self, t, v):
        self.t, self.v = t, v

    def __repr__(self):
        return 'Tag(%d,%r)' % (self.t, self.v)


class TypedList(list):
    """A list that remembers its NBT element type (needed for empty lists)."""

    def __init__(self, items, elem_type):
        super().__init__(items)
        self.elem_type = elem_type


def _read(f):
    def u1():
        return f.read(1)[0]

    def st():
        n = struct.unpack('>H', f.read(2))[0]
        return f.read(n).decode('utf-8')

    def pay(t):
        if t == BYTE:   return Tag(t, struct.unpack('>b', f.read(1))[0])
        if t == SHORT:  return Tag(t, struct.unpack('>h', f.read(2))[0])
        if t == INT:    return Tag(t, struct.unpack('>i', f.read(4))[0])
        if t == LONG:   return Tag(t, struct.unpack('>q', f.read(8))[0])
        if t == FLOAT:  return Tag(t, struct.unpack('>f', f.read(4))[0])
        if t == DOUBLE: return Tag(t, struct.unpack('>d', f.read(8))[0])
        if t == BARR:   return Tag(t, list(f.read(struct.unpack('>i', f.read(4))[0])))
        if t == STR:    return st()
        if t == IARR:
            n = struct.unpack('>i', f.read(4))[0]
            return Tag(t, [struct.unpack('>i', f.read(4))[0] for _ in range(n)])
        if t == LARR:
            n = struct.unpack('>i', f.read(4))[0]
            return Tag(t, [struct.unpack('>q', f.read(8))[0] for _ in range(n)])
        if t == LIST:
            et = u1()
            n = struct.unpack('>i', f.read(4))[0]
            return TypedList([pay(et) for _ in range(n)], et)
        if t == COMP:
            d = {}
            while True:
                tt = u1()
                if tt == END:
                    return d
                k = st()
                d[k] = pay(tt)
            return d
        raise ValueError('tag %d' % t)

    t = u1()
    st()  # root name
    return pay(t)


def type_of(v):
    if isinstance(v, Tag):        return v.t
    if isinstance(v, TypedList):  return LIST
    if isinstance(v, bool):       return BYTE
    if isinstance(v, int):        return INT
    if isinstance(v, str):        return STR
    if isinstance(v, dict):       return COMP
    if isinstance(v, list):       return LIST
    raise TypeError(type(v))


def _write_payload(f, v):
    if isinstance(v, Tag):
        t = v.t
        if t == BYTE:     f.write(struct.pack('>b', v.v))
        elif t == SHORT:  f.write(struct.pack('>h', v.v))
        elif t == INT:    f.write(struct.pack('>i', v.v))
        elif t == LONG:   f.write(struct.pack('>q', v.v))
        elif t == FLOAT:  f.write(struct.pack('>f', v.v))
        elif t == DOUBLE: f.write(struct.pack('>d', v.v))
        elif t == BARR:
            f.write(struct.pack('>i', len(v.v))); f.write(bytes(v.v))
        elif t == IARR:
            f.write(struct.pack('>i', len(v.v)))
            for i in v.v: f.write(struct.pack('>i', i))
        elif t == LARR:
            f.write(struct.pack('>i', len(v.v)))
            for i in v.v: f.write(struct.pack('>q', i))
        else:
            raise TypeError('Tag type %d' % t)
        return
    if isinstance(v, bool):
        f.write(struct.pack('>b', 1 if v else 0)); return
    if isinstance(v, int):
        f.write(struct.pack('>i', v)); return
    if isinstance(v, str):
        b = v.encode('utf-8'); f.write(struct.pack('>H', len(b))); f.write(b); return
    if isinstance(v, dict):
        for k, val in v.items():
            f.write(bytes([type_of(val)]))
            kb = k.encode('utf-8')
            f.write(struct.pack('>H', len(kb))); f.write(kb)
            _write_payload(f, val)
        f.write(bytes([END])); return
    if isinstance(v, list):
        et = v.elem_type if isinstance(v, TypedList) else (type_of(v[0]) if v else END)
        f.write(bytes([et])); f.write(struct.pack('>i', len(v)))
        for item in v:
            _write_payload(f, item)
        return
    raise TypeError(type(v))


def read_structure(path):
    with gzip.open(path, 'rb') as f:
        return _read(f)


def write_structure(path, root):
    # mtime=0 keeps regeneration byte-identical, so re-running a generator produces no git diff.
    with gzip.GzipFile(path, 'wb', mtime=0) as f:
        f.write(bytes([COMP]))
        f.write(struct.pack('>H', 0))
        _write_payload(f, root)


def i(n):
    return Tag(INT, n)


def pos(x, y, z):
    return TypedList([i(x), i(y), i(z)], INT)
