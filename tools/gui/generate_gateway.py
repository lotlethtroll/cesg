"""Generate gateway configuration panels from the approved CESG materials."""
from PIL import Image, ImageDraw
from generate_terminal import OUT, panel, rivet

def frame(height):
    image = Image.new('RGBA', (176, height))
    panel(image, (0, 0, 175, height-1))
    d = ImageDraw.Draw(image)
    d.rectangle((4, 3, 171, 17), fill='#a89b79')
    d.line((4, 17, 171, 17), fill='#756b51')
    for x in (0, 172):
        d.rectangle((x, 0, x+3, height-1), fill='#424b45')
        d.line((x+1, 0, x+1, height-1), fill='#b39759')
        d.line((x+2, 0, x+2, height-1), fill='#e0c78d')
    for y in (0, height-4):
        d.rectangle((0, y, 175, y+3), fill='#424b45')
        d.line((0, y+1, 175, y+1), fill='#b39759')
        d.line((0, y+2, 175, y+2), fill='#e0c78d')
    for x,y in ((0,0),(172,0),(0,height-4),(172,height-4)):
        d.rectangle((x,y,x+3,y+3), fill='#686c5c')
        rivet(d,x,y)
    return image, d

image,d = frame(230)
d.rectangle((8,40,167,52), fill='#b5a7bf')
d.line((8,52,167,52), fill='#756180')
image.save(OUT / 'gateway_channels.png')

image,d = frame(156)
for y,color in ((59,'#424b45'),(60,'#b39759'),(61,'#eee0b9')):
    d.line((7,y,168,y), fill=color)
image.save(OUT / 'gateway_filter.png')
