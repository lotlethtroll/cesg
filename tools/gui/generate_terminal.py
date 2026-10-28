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
d.rectangle((4, 3, 171, 14), fill='#a89b79')
d.line((4, 14, 171, 14), fill='#756b51')
d.rectangle((176, 3, 263, 14), fill='#b5a7bf')
d.line((176, 14, 263, 14), fill='#756180')


for x,y in [(3,3),(169,3),(3,228),(169,228),(261,3),(261,85)]:
    rivet(d,x,y)
# Arrow between the crafting inputs and result; leaves slot hit boxes untouched.
d.line([(237,54),(240,57),(237,60)], fill='#756180', width=1)
# Layered perimeter rails: steel backing, brass channel, and a light bevel.
for x in (0, 172):
    d.rectangle((x, 15, x+3, 226), fill='#424b45')
    d.line((x+1, 15, x+1, 226), fill='#b39759')
    d.line((x+2, 15, x+2, 226), fill='#e0c78d')
    for y in (24, 88, 145, 202):
        d.rectangle((x, y, x+3, y+5), fill='#686c5c')
        rivet(d, x, y+1)
# Clear the former wide joining bar; the common 4px rail is drawn below.
d.rectangle((172, 0, 175, 91), fill='#c9c3ad')
# Reinforced cross-member above the inventory; label remains on a quiet plate.
d.line((7, 138, 168, 138), fill='#424b45')
d.line((7, 139, 168, 139), fill='#b39759')
d.line((7, 140, 168, 140), fill='#eee0b9')
# One rail construction around the entire perimeter, including corner plates.
def horizontal_rail(x0, x1, y):
    d.rectangle((x0, y, x1, y+3), fill='#424b45')
    d.line((x0, y+1, x1, y+1), fill='#b39759')
    d.line((x0, y+2, x1, y+2), fill='#e0c78d')

def vertical_rail(x, y0, y1):
    d.rectangle((x, y0, x+3, y1), fill='#424b45')
    d.line((x+1, y0, x+1, y1), fill='#b39759')
    d.line((x+2, y0, x+2, y1), fill='#e0c78d')

horizontal_rail(0, 267, 0)
horizontal_rail(0, 175, 230)
# Clear the old vent strip before drawing the crafting bottom frame.
d.rectangle((180, 84, 267, 91), fill='#c9c3ad')
horizontal_rail(176, 267, 88)
vertical_rail(0, 0, 233)
vertical_rail(172, 0, 233)
vertical_rail(264, 0, 91)
for x,y in ((0,0),(172,0),(264,0),(0,230),(172,230),(264,88)):
    d.rectangle((x,y,x+3,y+3), fill='#686c5c')
    rivet(d,x,y)
image.save(OUT / 'terminal.png')

slot = Image.new('RGBA', (18,18), '#42473f')
s = ImageDraw.Draw(slot)
s.rectangle((1,1,17,17), fill='#eee5cb')
s.rectangle((1,1,16,16), fill='#747b6e')
s.line((1,1,16,1), fill='#555e52')
s.line((1,1,1,16), fill='#555e52')
slot.save(OUT / 'slot.png')
