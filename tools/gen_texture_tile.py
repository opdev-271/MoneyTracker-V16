"""Regenerates app/src/main/res/drawable-nodpi/tex_grain.png.

The tile is the light theme's paper grain, tiled by @drawable/bg_page. It is pure
white with the speckle living entirely in the alpha channel, so the drawable can
tint it per theme (SRC_IN keeps alpha and multiplies by the tint's own alpha) and
dial it down to nothing in dark mode without shipping a second asset.

It lives in drawable-nodpi deliberately: any density bucket would let Android scale
the bitmap, which blurs per-pixel grain into a flat grey wash on high-density
screens. nodpi repeats it at exactly 1:1 pixels.

Run from the repo root:  python3 tools/gen_texture_tile.py
Requires Pillow. The seed is fixed so re-running produces an identical file.
"""
import random
from PIL import Image

OUT = "app/src/main/res/drawable-nodpi/tex_grain.png"
SIZE = 128
SEED = 20260809

# Alpha below this stays fully transparent; the rest is rescaled back up to 0-255.
# Biasing towards transparent makes the grain read as sparse specks instead of an
# even wash that would just darken the surface uniformly.
CUTOFF = 150


def main():
    random.seed(SEED)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            a = random.randint(0, 255)
            a = 0 if a < CUTOFF else int((a - CUTOFF) / (255 - CUTOFF) * 255)
            px[x, y] = (255, 255, 255, a)
    img.save(OUT)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
