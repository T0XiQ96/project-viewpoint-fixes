# Erzeugt preview.png (256x256) fuer ein Workshop-Item. Aufruf: python preview.py <Ausgabe> <Zeile1> [Zeile2] [Farbe r,g,b]
import sys
from PIL import Image, ImageDraw, ImageFont
out = sys.argv[1]; lines = sys.argv[2:4]; col = tuple(int(x) for x in (sys.argv[4] if len(sys.argv) > 4 else "60,120,200").split(","))
img = Image.new("RGB", (256, 256), (22, 24, 28))
d = ImageDraw.Draw(img)
for i in range(256):
    d.line([(0, i), (255, i)], fill=tuple(int(c * (0.35 + 0.65 * i / 255)) for c in col))
d.rectangle([10, 10, 245, 245], outline=(230, 230, 230), width=3)
try:
    big = ImageFont.truetype("arialbd.ttf", 30); small = ImageFont.truetype("arial.ttf", 20)
except OSError:
    big = small = ImageFont.load_default()
y = 70
for n, text in enumerate(lines):
    f = big if n == 0 else small
    for part in text.split("|"):
        w = d.textlength(part, font=f)
        d.text(((256 - w) / 2, y), part, font=f, fill=(255, 255, 255))
        y += 38 if n == 0 else 28
    y += 10
img.save(out)
print("ok", out)
