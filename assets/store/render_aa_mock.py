#!/usr/bin/env python3
"""
Synthetic 1280×720 mock of the CarMenu list as Android Auto would render it.
Useful as a Play screenshot when the user doesn't have a real DHU capture.

Output: aa-mock-1280x720.png next to this file.
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "aa-mock-1280x720.png")
W, H = 1280, 720

# AA-ish dark theme palette (approximate — host has the real theme)
BG       = "#0F1115"
PANEL    = "#191B22"
DIVIDER  = "#2A2D35"
TEXT     = "#E8E8E8"
SUB      = "#9A9A9A"
ACCENT   = "#E8D9A8"
RED      = "#E25656"
BLUE     = "#3399FF"
YELLOW   = "#F2C84B"
GREEN    = "#3CB860"
ORANGE   = "#F08C32"
WHITE    = "#FFFFFF"


def font(sz, bold=False):
    try:
        path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold \
            else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        return ImageFont.truetype(path, sz)
    except OSError:
        return ImageFont.load_default()


def draw_compass(img, cx, cy, radius):
    d = ImageDraw.Draw(img)
    d.ellipse([cx-radius, cy-radius, cx+radius, cy+radius],
              outline=ACCENT, width=2)
    s = radius / 36.0
    outer = [(0,-36),(17,-17),(36,0),(17,17),(0,36),(-17,17),(-36,0),(-17,-17)]
    inner = [(4,-9),(9,-4),(9,4),(4,9),(-4,9),(-9,4),(-9,-4),(-4,-9)]

    def pt(p): return (cx + p[0]*s, cy + p[1]*s)

    for i in range(8):
        o = outer[i]
        r_inner = inner[i]; l_inner = inner[(i-1) % 8]
        d.polygon([pt(o), pt(r_inner), (cx, cy)], fill="#F4E4BC")
        d.polygon([pt(o), pt(l_inner), (cx, cy)], fill="#B8A266")


def home_icon(d, cx, cy, color):
    # House silhouette
    d.polygon([(cx-22, cy-2), (cx, cy-22), (cx+22, cy-2),
               (cx+16, cy-2), (cx+16, cy+18),
               (cx-16, cy+18), (cx-16, cy-2)], fill=color)


def briefcase_icon(d, cx, cy, color):
    d.rectangle([cx-22, cy-10, cx+22, cy+18], fill=color)
    d.rectangle([cx-8, cy-18, cx+8, cy-8], outline=color, width=3)


def circle_icon(d, cx, cy, fill, ring=None):
    d.ellipse([cx-22, cy-22, cx+22, cy+22], fill=fill)
    if ring:
        d.ellipse([cx-12, cy-12, cx+12, cy+12], fill=ring)


def refresh_icon(d, cx, cy, color):
    # Simple circular arrow
    d.arc([cx-20, cy-20, cx+20, cy+20], start=30, end=300,
          fill=color, width=4)
    # Arrowhead
    d.polygon([(cx+18, cy-12), (cx+8, cy-20), (cx+20, cy-2)], fill=color)


def row(img, y, icon_drawer, title, subtitle):
    d = ImageDraw.Draw(img)
    # Row divider
    d.line([(80, y+78), (W-80, y+78)], fill=DIVIDER, width=1)
    icon_drawer(d, 120, y+38)
    d.text((180, y+10), title, font=font(28, bold=True), fill=TEXT)
    if subtitle:
        d.text((180, y+45), subtitle, font=font(22), fill=SUB)


def main():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)

    # ── Top bar (app icon + title) ──────────────────────────────────────
    d.rectangle([(0, 0), (W, 80)], fill=PANEL)
    draw_compass(img, 40, 40, 20)
    d.text((78, 26), "Where to?", font=font(28, bold=True), fill=TEXT)
    # Right side: status pill
    d.rounded_rectangle([(W-180, 22), (W-30, 58)],
                        radius=18, fill="#1F2F1A", outline=GREEN, width=1)
    d.text((W-160, 28), "● connected", font=font(20), fill=GREEN)

    # ── Rows ────────────────────────────────────────────────────────────
    rows = [
        (lambda d, x, y: home_icon(d, x, y, RED),     "Cafe Aroma",     "Old town square"),
        (lambda d, x, y: briefcase_icon(d, x, y, BLUE),"Public library", "Main branch"),
        (lambda d, x, y: circle_icon(d, x, y, GREEN, WHITE),
                                                       "Fast charger",   "Highway rest stop"),
        (lambda d, x, y: circle_icon(d, x, y, ORANGE, WHITE),
                                                       "Lunch spot",     "Downtown"),
        (lambda d, x, y: refresh_icon(d, x, y, YELLOW), "live GPS", None),
    ]
    for i, (icon, title, sub) in enumerate(rows):
        row(img, 100 + i*100, icon, title, sub)

    img.save(OUT, "PNG")
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
