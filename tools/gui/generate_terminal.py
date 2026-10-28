"""Generate original CESG pixel-art GUI textures. Run from the repository root."""
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path('src/main/resources/assets/cesg/textures/gui')
OUT.mkdir(parents=True, exist_ok=True)

def panel(image, box):
    d = ImageDraw.Draw(image)
    x, y, right, bottom = box
    d.rectangle(box, fill='#393b36')
    d.rectangle((x+1, y+1, right-1, bottom-1), fill='#b8ad91')
    d.rectangle((x+2, y+2, right-2, bottom-2), fill='#777a6b')
    d.rectangle((x+3, y+3, right-3, bottom-3), fill='#c9c3ad')
    for row in range(y+4, bottom-3):
        for col in range(x+4, right-3):
            # Sparse, deterministic grain; no noise behind item silhouettes.
            if (col*13 + row*7) % 47 == 0:
                d.point((col, row), fill='#c2bca7')
    d.line((x+3, bottom-2, right-2, bottom-2), fill='#555b50')
    d.line((right-2, y+3, right-2, bottom-2), fill='#555b50')

def rivet(d, x, y):
    d.rectangle((x, y, x+2, y+2), fill='#625c49')
    d.point((x, y), fill='#e7d5a4')

image = Image.new('RGBA', (268, 234))
panel(image, (0, 0, 175, 233))
panel(image, (176, 0, 267, 91))
d = ImageDraw.Draw(image)
# Join the two plates without a double border.
d.rectangle((173, 3, 179, 88), fill='#c9c3ad')
d.rectangle((4, 3, 171, 13), fill='#a89b79')
d.line((4, 13, 171, 13), fill='#756b51')
d.rectangle((180, 3, 263, 14), fill='#b5a7bf')
d.line((180, 14, 263, 14), fill='#756180')
d.line((7, 141, 168, 141), fill='#8d8264')
d.line((7, 142, 168, 142), fill='#e1d9bf')
for x,y in [(3,3),(169,3),(3,228),(169,228),(261,3),(261,85)]:
    rivet(d,x,y)
# Arrow between the crafting inputs and result; leaves slot hit boxes untouched.
d.polygon([(238,39),(238,42),(240,42),(240,44),(242,40),(240,36),(240,39)], fill='#756180')
# Layered perimeter rails: steel backing, brass channel, and a light bevel.
for x in (0, 172):
    d.rectangle((x, 15, x+3, 226), fill='#424b45')
    d.line((x+1, 15, x+1, 226), fill='#b39759')
    d.line((x+2, 15, x+2, 226), fill='#e0c78d')
    for y in (24, 88, 145, 202):
        d.rectangle((x, y, x+3, y+5), fill='#686c5c')
        rivet(d, x, y+1)
# A recessed mechanical joint joins the crafting attachment to the main body.
d.rectangle((175, 17, 179, 82), fill='#555e54')
d.line((177, 18, 177, 81), fill='#b39759')
for y in (20, 72):
    d.rectangle((174, y, 180, y+8), fill='#7e806c')
    d.line((174, y, 180, y), fill='#e0c78d')
    rivet(d, 176, y+3)
# Reinforced cross-member above the inventory; label remains on a quiet plate.
d.line((7, 138, 168, 138), fill='#424b45')
d.line((7, 139, 168, 139), fill='#b39759')
d.line((7, 140, 168, 140), fill='#eee0b9')
for x in range(103, 165, 5):
    d.line((x, 144, x+2, 144), fill='#626c5e')
    d.line((x, 145, x+2, 145), fill='#eee0b9')
# Crafting module's vented lower lip and right-hand rail.
d.rectangle((181, 85, 263, 89), fill='#5a6257')
d.line((181, 85, 263, 85), fill='#d2b774')
for x in range(193, 253, 4):
    d.line((x, 87, x+1, 87), fill='#2e3934')
d.rectangle((264, 16, 266, 84), fill='#565e53')
d.line((265, 16, 265, 84), fill='#b39759')
for x,y in ((181,86),(259,86),(3,228),(169,228)):
    rivet(d,x,y)
image.save(OUT / 'terminal.png')

slot = Image.new('RGBA', (18,18), '#42473f')
s = ImageDraw.Draw(slot)
s.rectangle((1,1,17,17), fill='#eee5cb')
s.rectangle((1,1,16,16), fill='#747b6e')
s.line((1,1,16,1), fill='#555e52')
s.line((1,1,1,16), fill='#555e52')
slot.save(OUT / 'slot.png')
