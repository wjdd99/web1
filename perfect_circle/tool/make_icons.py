"""Generate launcher icons for the Perfect Circle app.

Creates square PNGs with a soft gradient background and an off-center ring,
then writes them into the standard Android mipmap buckets.
"""
from pathlib import Path

from PIL import Image, ImageDraw

SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BG_TOP = (63, 81, 181)
BG_BOTTOM = (91, 141, 239)
RING = (255, 255, 255, 235)
ACCENT = (255, 209, 102, 230)


def make_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), BG_TOP + (255,))
    for y in range(size):
        t = y / (size - 1)
        r = int(BG_TOP[0] * (1 - t) + BG_BOTTOM[0] * t)
        g = int(BG_TOP[1] * (1 - t) + BG_BOTTOM[1] * t)
        b = int(BG_TOP[2] * (1 - t) + BG_BOTTOM[2] * t)
        for x in range(size):
            img.putpixel((x, y), (r, g, b, 255))

    draw = ImageDraw.Draw(img)
    pad = int(size * 0.15)
    thick = max(2, int(size * 0.08))
    draw.ellipse(
        (pad, pad, size - pad, size - pad),
        outline=RING,
        width=thick,
    )
    dot_r = int(size * 0.06)
    cx = int(size * 0.74)
    cy = int(size * 0.26)
    draw.ellipse(
        (cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r),
        fill=ACCENT,
    )
    return img


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    for density, size in SIZES.items():
        out_dir = root / "android" / "app" / "src" / "main" / "res" / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        img = make_icon(size)
        img.save(out_dir / "ic_launcher.png", "PNG")
        print(f"wrote {out_dir/'ic_launcher.png'} ({size}x{size})")


if __name__ == "__main__":
    main()
