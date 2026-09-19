#!/usr/bin/env python3
"""Build docs/shipaton/ui-review.html from the PNGs in docs/shipaton/shots/.

The preview tab only serves a single HTML file, so every screenshot is inlined
as a base64 JPEG. Images are downscaled to REVIEW_WIDTH (the layout is
identical, only the pixel count drops) which keeps the page a few hundred KB
while staying readable.

Drop new PNGs into shots/ and re-run. Files sort by name, so prefix them
(e.g. 01-home.png, 02-journal.png) to control the order.
"""

import argparse
import base64
import io
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
SHOTS = ROOT / "shots"
OUT = ROOT / "ui-review.html"
REVIEW_WIDTH = 560
QUALITY = 92


def load(path: Path, width: int) -> str:
    image = Image.open(path).convert("RGB")
    if image.width != width:
        height = round(image.height * width / image.width)
        image = image.resize((width, height), Image.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=QUALITY, optimize=True, progressive=True)
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def label(path: Path) -> str:
    return path.stem.replace("-", " ").replace("_", " ")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only", default=None, help="render only files whose name contains this text")
    parser.add_argument("--width", type=int, default=REVIEW_WIDTH, help="rendered width of each shot in CSS px")
    args = parser.parse_args()

    shots = sorted(p for p in SHOTS.glob("*.png") if not p.name.startswith("_"))
    if args.only:
        shots = [p for p in shots if args.only in p.name]
    if not shots:
        raise SystemExit(f"no screenshots in {SHOTS}")
    cards = "\n".join(
        f'<figure><figcaption>{label(p)}</figcaption>'
        f'<img src="data:image/jpeg;base64,{load(p, args.width * 2)}" alt="{label(p)}"'
        f' style="width:{args.width}px">'
        f'</figure>'
        for p in shots
    )
    OUT.write_text(
        """<!doctype html>
<meta charset="utf-8">
<title>Undo — UI review</title>
<style>
  :root { color-scheme: dark; }
  body { margin:0; background:#0b0a10; color:#e9e6f4; font:14px/1.4 system-ui, sans-serif; }
  h1 { font-weight:600; font-size:17px; margin:18px 20px 4px; }
  p.lead { margin:0 20px 14px; color:#b9b2d6; font-size:13px; }
  .row { display:flex; gap:16px; padding:0 20px 28px; flex-wrap:wrap; align-items:flex-start; }
  figure { margin:0; }
  figcaption { font-size:11px; color:#b9b2d6; margin-bottom:5px; letter-spacing:.5px; text-transform:uppercase; }
  img { display:block; height:auto; border-radius:12px; border:1px solid #2a2438; }
</style>
<h1>Undo — emulator screenshots</h1>
<p class="lead">Same device (1080&times;2400 @ 420dpi, Pixel-class) for every shot, downscaled for review. Layout changes are directly comparable.</p>
<div class="row">
""" + cards + """
</div>
""",
        encoding="utf-8",
    )
    print(f"wrote {OUT.name} ({OUT.stat().st_size // 1024} KB) with {len(shots)} shot(s): "
          + ", ".join(p.name for p in shots))


if __name__ == "__main__":
    main()
