"""Generate the Bridge panel using the approved terminal's pixel-art materials."""
from PIL import Image, ImageDraw
from generate_terminal import OUT, panel, rivet

image = Image.new('RGBA', (176, 220))
panel(image, (0, 0, 175, 219))
d = ImageDraw.Draw(image)
d.rectangle((4, 3, 171, 17), fill='#a89b79')
d.line((4, 17, 171, 17), fill='#756b51')
# Quiet section plates leave labels and existing hit targets unobstructed.
for y in (19, 73):
    d.rectangle((6, y, 169, y+12), fill='#b5a7bf')
    d.line((6, y+12, 169, y+12), fill='#756180')
for y in (124,):
    d.line((7, y, 168, y), fill='#424b45')
    d.line((7, y+1, 168, y+1), fill='#b39759')
    d.line((7, y+2, 168, y+2), fill='#eee0b9')
for x in (0, 172):
    d.rectangle((x, 0, x+3, 219), fill='#424b45')
    d.line((x+1, 0, x+1, 219), fill='#b39759')
    d.line((x+2, 0, x+2, 219), fill='#e0c78d')
for y in (0, 216):
    d.rectangle((0, y, 175, y+3), fill='#424b45')
    d.line((0, y+1, 175, y+1), fill='#b39759')
    d.line((0, y+2, 175, y+2), fill='#e0c78d')
for x,y in ((0,0),(172,0),(0,216),(172,216)):
    d.rectangle((x,y,x+3,y+3), fill='#686c5c')
    rivet(d,x,y)
image.save(OUT / 'bridge.png')
