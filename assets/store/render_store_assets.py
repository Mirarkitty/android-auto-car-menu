#!/usr/bin/env python3
"""
Render the Play Store visual assets from the compass-rose icon design.

Outputs:
- icon-512.png        512×512 app icon (Play listing)
- feature-1024x500.png Feature graphic for the listing header

Re-run any time the icon design changes:
    python3 render_store_assets.py
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT_DIR = os.path.dirname(os.path.abspath(__file__))

NAVY        = "#0E2538"
NAVY_DARK   = "#08182A"
NAVY_LIGHT  = "#1A3A55"
LIGHT       = "#F4E4BC"
SHADOW      = "#B8A266"
CENTER_DOT  = NAVY


def draw_compass(img: Image.Image, cx: float, cy: float, radius: float,
                 ring_stroke: float, draw_center_dot: bool = True):
    """Draw the 8-point compass rose centered at (cx, cy) with outer radius."""
    d = ImageDraw.Draw(img, "RGBA")

    # Outer ring
    d.ellipse([cx-radius, cy-radius, cx+radius, cy+radius],
              outline=LIGHT, width=int(ring_stroke))

    # Same proportions as the adaptive icon (108-unit viewport):
    # outer points at r=36, intercardinals at 24, inner notches at ~10.
    # Pre-compute scaled vertex pairs.
    s = radius / 36.0
    outer = [(0,-36),(17,-17),(36,0),(17,17),(0,36),(-17,17),(-36,0),(-17,-17)]
    inner = [(4,-9),(9,-4),(9,4),(4,9),(-4,9),(-9,4),(-9,-4),(-4,-9)]

    def pt(p):
        return (cx + p[0]*s, cy + p[1]*s)

    for i in range(8):
        o = outer[i]
        r_inner = inner[i]
        l_inner = inner[(i-1) % 8]
        # Light half (right side, clockwise)
        d.polygon([pt(o), pt(r_inner), (cx, cy)], fill=LIGHT)
        # Shadow half (left side)
        d.polygon([pt(o), pt(l_inner), (cx, cy)], fill=SHADOW)

    if draw_center_dot:
        rdot = 3 * s
        d.ellipse([cx-rdot, cy-rdot, cx+rdot, cy+rdot], fill=CENTER_DOT)

    # North marker (small bright triangle at the tip of N)
    tip = 17 * s
    d.polygon([(cx, cy-tip-1*s), (cx+3*s, cy-tip+5*s),
               (cx-3*s, cy-tip+5*s)], fill=LIGHT)


def make_radial_bg(size: int) -> Image.Image:
    """Solid navy + radial highlight at center."""
    bg = Image.new("RGBA", (size, size), NAVY)
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    cx = cy = size / 2
    max_r = size * 0.55
    steps = int(max_r)
    for r in range(steps, 0, -1):
        alpha = int(34 * (1 - r/max_r))
        if alpha <= 0: continue
        d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(255, 255, 255, alpha))
    return Image.alpha_composite(bg.convert("RGBA"), overlay)


# ── 512×512 app icon ────────────────────────────────────────────────────
def make_icon_512():
    img = make_radial_bg(512)
    # In the adaptive icon, the 8-point star reaches r=36 in a 108-unit
    # viewport — 36/108 = 1/3 of the side. Scale to 512: radius ≈ 170.
    draw_compass(img, 256, 256, radius=170, ring_stroke=6)
    out = os.path.join(OUT_DIR, "icon-512.png")
    img.save(out, "PNG")
    print(f"wrote {out}")


# ── 1024×500 feature graphic ───────────────────────────────────────────
def make_feature():
    W, H = 1024, 500
    # Horizontal navy gradient (light at center-left, dark at edges).
    img = Image.new("RGBA", (W, H), NAVY)
    grad = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    cx_grad = W * 0.30
    cy_grad = H * 0.50
    max_r = H * 1.4
    for r in range(int(max_r), 0, -2):
        alpha = int(28 * (1 - r/max_r))
        if alpha <= 0: continue
        gd.ellipse([cx_grad-r, cy_grad-r, cx_grad+r, cy_grad+r],
                   fill=(255, 255, 255, alpha))
    img = Image.alpha_composite(img.convert("RGBA"), grad)

    # Compass rose on the left third
    draw_compass(img, 175, H/2, radius=160, ring_stroke=4)

    # App name + tagline on the right
    d = ImageDraw.Draw(img)
    try:
        # Try a few common system fonts; fallback to default.
        font_name = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 88)
        font_tag = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 34)
    except OSError:
        font_name = ImageFont.load_default()
        font_tag = ImageFont.load_default()

    d.text((400, 150), "CarMenu", font=font_name, fill=LIGHT)
    d.text((400, 270),
           "Your server. Your destinations.",
           font=font_tag, fill="#D8C690")
    d.text((400, 320),
           "Minimal Android Auto, server-driven.",
           font=font_tag, fill="#A89464")

    out = os.path.join(OUT_DIR, "feature-1024x500.png")
    img.convert("RGB").save(out, "PNG")
    print(f"wrote {out}")


if __name__ == "__main__":
    make_icon_512()
    make_feature()
