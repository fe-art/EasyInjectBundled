from PIL import Image, ImageDraw, ImageFont
import os, random

font_path = os.path.join(os.environ['TEMP'], 'FredokaOne-Regular.ttf')

W, H = 520, 130

img = Image.new('RGBA', (W, H), (0, 0, 0, 0))

for y in range(H):
    t = y / H
    r = int(45 + t * 10)
    g = int(25 + t * 10)
    b = int(70 - t * 20)
    for x in range(W):
        tx = x / W
        rr = r + int(tx * 15)
        img.putpixel((x, y), (rr, g, b, 240))

random.seed(77)
draw = ImageDraw.Draw(img)
for _ in range(40):
    x = random.randint(0, W-1)
    y = random.randint(0, H-1)
    s = random.choice([3, 4, 5, 6])
    a = random.randint(20, 60)
    c = random.choice([
        (120, 80, 180, a), (80, 200, 120, a), (200, 150, 80, a), (100, 160, 220, a)
    ])
    draw.rectangle([x, y, x+s, y+s], fill=c)

for x in range(W):
    t = x / W
    g_val = int(160 + t * 40)
    draw.rectangle([x, H-4, x, H-1], fill=(50, g_val, 80, 200))

try:
    title_font = ImageFont.truetype(font_path, 52)
    sub_font = ImageFont.truetype(font_path, 18)
except:
    title_font = ImageFont.load_default()
    sub_font = title_font

logo_x, logo_y = 18, 18
logo_s = 94
draw.rounded_rectangle([logo_x, logo_y, logo_x+logo_s, logo_y+logo_s], radius=16, fill=(76, 175, 80, 220))
draw.rectangle([logo_x+18, logo_y+16, logo_x+logo_s-18, logo_y+30], fill=(255,255,255,255))
draw.rectangle([logo_x+36, logo_y+30, logo_x+logo_s-36, logo_y+logo_s-16], fill=(255,255,255,255))

text = 'Toolscreen'
text_x = 130
text_y = 28

draw.text((text_x+2, text_y+2), text, font=title_font, fill=(0, 0, 0, 120))
draw.text((text_x, text_y), text, font=title_font, fill=(240, 235, 220, 255))

v_text = 'v1.2.8'
v_bbox = draw.textbbox((0, 0), v_text, font=sub_font)
v_w = v_bbox[2] - v_bbox[0] + 16
v_x = W - v_w - 16
v_y = 16
draw.rounded_rectangle([v_x, v_y, v_x+v_w, v_y+24], radius=12, fill=(76, 175, 80, 180))
draw.text((v_x+8, v_y+2), v_text, font=sub_font, fill=(255, 255, 255, 240))

out = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'icons', 'banner.png')
img.save(out)
print('Banner saved:', img.size)
