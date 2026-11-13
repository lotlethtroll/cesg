"""Dump and compare CESG block entities straight from a world's region files (stdlib only).

  python -I cesg_nbt.py dump <world dir> <out.json>
  python -I cesg_nbt.py compare <before.json> <after.json> <out.json>

The dump maps "dimension:x,y,z" to the block entity's NBT as typed JSON, for every block entity whose
id starts with "cesg:" in the Overworld, Nether and End.
"""
import json
import os
import struct
import sys
import zlib
import gzip

DIMENSIONS = {"minecraft:overworld": "region", "minecraft:the_nether": "DIM-1/region",
              "minecraft:the_end": "DIM1/region"}
TYPES = {1: "B", 2: "S", 3: "I", 4: "L", 5: "F", 6: "D", 7: "BA", 8: "Str", 9: "List", 10: "C",
         11: "IA", 12: "LA"}


class Reader:
    def __init__(self, data):
        self.data, self.pos = data, 0

    def take(self, n):
        chunk = self.data[self.pos:self.pos + n]
        self.pos += n
        return chunk

    def unpack(self, fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(">" + fmt, self.take(size))[0]

    def string(self):
        return self.take(self.unpack("H")).decode("utf-8", "replace")

    def payload(self, tag):
        if tag == 1: return self.unpack("b")
        if tag == 2: return self.unpack("h")
        if tag == 3: return self.unpack("i")
        if tag == 4: return self.unpack("q")
        if tag == 5: return self.unpack("f")
        if tag == 6: return self.unpack("d")
        if tag == 7: return list(self.take(self.unpack("i")))
        if tag == 8: return self.string()
        if tag == 9:
            inner, count = self.unpack("b"), self.unpack("i")
            return {"List": TYPES.get(inner, "End"), "v": [self.typed(inner) for _ in range(count)]}
        if tag == 10:
            out = {}
            while True:
                child = self.unpack("b")
                if child == 0:
                    return out
                name = self.string()
                out[name] = self.typed(child)
        if tag == 11: return [self.unpack("i") for _ in range(self.unpack("i"))]
        if tag == 12: return [self.unpack("q") for _ in range(self.unpack("i"))]
        raise ValueError(f"bad tag {tag}")

    def typed(self, tag):
        value = self.payload(tag)
        return value if tag in (9, 10) else {TYPES[tag]: value}

    def root(self):
        tag = self.unpack("b")
        self.string()
        return self.payload(tag)


def plain(value):
    """Typed NBT -> plain value, for reading ids/positions."""
    if isinstance(value, dict) and len(value) == 1:
        (k, v), = value.items()
        if k in TYPES.values() and k not in ("C",):
            return v
    if isinstance(value, dict) and "List" in value:
        return [plain(v) for v in value["v"]]
    return value


def chunks(region_file):
    with open(region_file, "rb") as f:
        data = f.read()
    for i in range(1024):
        entry = struct.unpack(">I", data[i * 4:i * 4 + 4])[0]
        offset, sectors = entry >> 8, entry & 0xFF
        if not offset or not sectors:
            continue
        start = offset * 4096
        length, compression = struct.unpack(">IB", data[start:start + 5])
        raw = data[start + 5:start + 4 + length]
        if compression == 2:
            raw = zlib.decompress(raw)
        elif compression == 1:
            raw = gzip.decompress(raw)
        elif compression != 3:
            raise ValueError(f"{region_file}: unsupported compression {compression}")
        yield Reader(raw).root()


def dump(world, out):
    result = {}
    for dim, sub in DIMENSIONS.items():
        folder = os.path.join(world, sub)
        if not os.path.isdir(folder):
            continue
        for name in sorted(os.listdir(folder)):
            if not name.endswith(".mca") or os.path.getsize(os.path.join(folder, name)) == 0:
                continue
            for chunk in chunks(os.path.join(folder, name)):
                for be in plain(chunk.get("block_entities", {"List": "C", "v": []})):
                    bid = plain(be.get("id", {"Str": ""}))
                    if isinstance(bid, str) and bid.startswith("cesg:"):
                        key = f"{dim}:{plain(be['x'])},{plain(be['y'])},{plain(be['z'])}"
                        result[key] = be
    with open(out, "w", encoding="utf-8") as f:
        json.dump(result, f, sort_keys=True)
    ids = {}
    for be in result.values():
        ids[plain(be["id"])] = ids.get(plain(be["id"]), 0) + 1
    print(f"{len(result)} CESG block entities -> {out}")
    for k in sorted(ids):
        print(f"  {ids[k]:5d} {k}")


def compare(before_path, after_path, out):
    before = json.load(open(before_path, encoding="utf-8"))
    after = json.load(open(after_path, encoding="utf-8"))
    missing = sorted(set(before) - set(after))
    added = sorted(set(after) - set(before))
    changed, field_counts = {}, {}
    for key in sorted(set(before) & set(after)):
        b, a = before[key], after[key]
        if plain(b["id"]) != plain(a["id"]):
            changed[key] = {"id": [plain(b["id"]), plain(a["id"])]}
            continue
        diff = {}
        for field in sorted(set(b) | set(a)):
            if b.get(field) != a.get(field):
                diff[field] = [b.get(field), a.get(field)]
                field_counts[field] = field_counts.get(field, 0) + 1
        if diff:
            changed[key] = diff
    report = {"before": len(before), "after": len(after), "missing": missing, "added": added,
              "changed_fields": field_counts, "changed": changed}
    with open(out, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=1, sort_keys=True)
    print(f"before={len(before)} after={len(after)} missing={len(missing)} added={len(added)} "
          f"changed_entities={len(changed)}")
    for field, count in sorted(field_counts.items(), key=lambda x: -x[1]):
        print(f"  {count:5d} {field}")


if __name__ == "__main__":
    if sys.argv[1] == "dump":
        dump(sys.argv[2], sys.argv[3])
    else:
        compare(sys.argv[2], sys.argv[3], sys.argv[4])
