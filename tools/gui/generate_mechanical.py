"""Generate a tileable mechanical panel for variable-size CESG screens."""
from generate_gateway import frame
from generate_terminal import OUT
image, _ = frame(32)
# A header-free tile: copy uninterrupted body into the interior.
for y in range(4, 28):
    for x in range(4, 172):
        image.putpixel((x,y), (194,188,167,255) if (x*13+y*7)%47 == 0 else (201,195,173,255))
# Retain the right rail and corners while reducing to a 32px nine-slice tile.
from PIL import Image
out = Image.new('RGBA', (32,32))
out.paste(image.crop((0,0,28,32)),(0,0))
out.paste(image.crop((172,0,176,32)),(28,0))
out.save(OUT / 'mechanical_panel.png')
