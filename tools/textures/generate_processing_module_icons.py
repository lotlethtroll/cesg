"""Generates the Haunting and Blasting upgrade item textures (Mk I-III).

The frame is copied from the matching washing_upgrade_t<tier>.png so every processing module shares the
same tier frame (Mk I iron, Mk II brass, Mk III teal); only the 10x12 interior motif differs. Run from the
repo root:  python tools/textures/generate_processing_module_icons.py [--sheet docs/dev/2.0/processing-modules-icons.png]
"""
import argparse
import os

from PIL import Image

ITEM_DIR = "src/main/resources/assets/cesg/textures/item"
CHECK_DARK = (0x20, 0x1E, 0x26, 255)
CHECK_LIGHT = (0x2E, 0x2C, 0x36, 255)
X0, Y0 = 3, 2  # interior origin; interior is 10 wide (x 3..12) and 12 tall (y 2..13)

# Soul campfire: teal soul flame over two crossed logs.
HAUNTING = [
    "..........",
    ".....a....",
    "....ab....",
    "....abb.a.",
    "..a.bcbbb.",
    "..bbbccb..",
    "..bccwcb..",
    "..bcwwcb..",
    "...dccd...",
    ".LLlLLlLL.",
    ".mLLmmLLm.",
    "..........",
]
HAUNTING_COLORS = {
    "a": (0xB4, 0xFB, 0xFF, 255),  # flame tips
    "b": (0x3E, 0xD0, 0xD8, 255),  # flame body
    "c": (0x8C, 0xF4, 0xF8, 255),  # inner flame
    "w": (0xE6, 0xFF, 0xFF, 255),  # core
    "d": (0x1A, 0x8A, 0x98, 255),  # flame base shade
    "L": (0x6B, 0x4A, 0x2E, 255),  # log
    "l": (0x8A, 0x64, 0x3E, 255),  # log highlight
    "m": (0x3A, 0x28, 0x1A, 255),  # log end / shade
}

# Blast furnace mouth: dark iron block with a glowing grate.
BLASTING = [
    "..........",
    "....ab....",
    "....bc....",
    ".IIIIIIII.",
    ".IhhhhhhK.",
    ".IhKKKKhK.",
    ".IKfgfgKK.",
    ".IKgrgrKK.",
    ".IKKKKKKK.",
    ".IhKhhKhK.",
    ".KKKKKKKK.",
    "..........",
]
BLASTING_COLORS = {
    "a": (0xFF, 0xD2, 0x5A, 255),  # spark / flame tip (smelting palette)
    "b": (0xEB, 0x78, 0x1E, 255),  # flame
    "c": (0xA8, 0x3A, 0x14, 255),  # flame base
    "f": (0xFF, 0xD2, 0x5A, 255),  # glow bright
    "g": (0xEB, 0x78, 0x1E, 255),  # glow
    "r": (0xA8, 0x3A, 0x14, 255),  # glow deep
    "I": (0x8A, 0x8A, 0x90, 255),  # iron highlight
    "h": (0x5C, 0x5C, 0x64, 255),  # iron face
    "K": (0x3A, 0x3A, 0x42, 255),  # iron shade
}


def build(tier, art, colors):
    frame = Image.open(os.path.join(ITEM_DIR, f"washing_upgrade_t{tier}.png")).convert("RGBA")
    img = frame.copy()
    for y in range(12):
        for x in range(10):
            px, py = X0 + x, Y0 + y
            ch = art[y][x]
            if ch == ".":
                img.putpixel((px, py), CHECK_DARK if (px + py) % 2 == 1 else CHECK_LIGHT)
            else:
                img.putpixel((px, py), colors[ch])
    return img


def contact_sheet(path, scale=16, gap=8):
    rows = [
        ["washing_upgrade_t1", "washing_upgrade_t2", "washing_upgrade_t3", "smelting_upgrade"],
        ["crushing_upgrade_t1", "crushing_upgrade_t2", "crushing_upgrade_t3"],
        ["haunting_upgrade_t1", "haunting_upgrade_t2", "haunting_upgrade_t3"],
        ["blasting_upgrade_t1", "blasting_upgrade_t2", "blasting_upgrade_t3"],
    ]
    cell = 16 * scale
    width = max(len(r) for r in rows) * (cell + gap) + gap
    height = len(rows) * (cell + gap) + gap
    sheet = Image.new("RGBA", (width, height), (0x3C, 0x3C, 0x3C, 255))
    for r, names in enumerate(rows):
        for c, name in enumerate(names):
            icon = Image.open(os.path.join(ITEM_DIR, name + ".png")).convert("RGBA")
            sheet.alpha_composite(icon.resize((cell, cell), Image.NEAREST),
                                  (gap + c * (cell + gap), gap + r * (cell + gap)))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sheet", help="also write a 16x contact sheet of old + new icons to this path")
    args = parser.parse_args()
    for tier in (1, 2, 3):
        build(tier, HAUNTING, HAUNTING_COLORS).save(os.path.join(ITEM_DIR, f"haunting_upgrade_t{tier}.png"))
        build(tier, BLASTING, BLASTING_COLORS).save(os.path.join(ITEM_DIR, f"blasting_upgrade_t{tier}.png"))
    if args.sheet:
        contact_sheet(args.sheet)


if __name__ == "__main__":
    main()
