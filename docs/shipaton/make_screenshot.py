"""Convert a phone screenshot into the exact 1179x2556 PNG the Shipaton form wants.

The submission requires at least one screenshot at **1179 px wide by 2556 px high,
without a device frame**. Rather than stretching a 1080x2400 or 1440x3120 capture
(which distorts the UI by a couple of percent), this scales uniformly to the target
width and then centre-crops the height, so proportions stay intact.

Usage:
    python docs/shipaton/make_screenshot.py path/to/phone-screenshot.png
    # writes docs/shipaton/undo-screenshot-1179x2556.png

    # several at once (writes -2, -3, ... suffixes)
    python docs/shipaton/make_screenshot.py shot-a.png shot-b.png
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

TARGET_W = 1179
TARGET_H = 2556
OUT_DIR = Path(__file__).resolve().parent


def convert(source: Path, destination: Path) -> None:
    with Image.open(source) as original:
        image = original.convert("RGB")

    scale = TARGET_W / image.width
    scaled_h = round(image.height * scale)
    if scaled_h < TARGET_H:
        raise SystemExit(
            f"{source.name}: {image.width}x{image.height} is too wide/short — scaling to "
            f"{TARGET_W}px wide only gives {scaled_h}px of height, less than {TARGET_H}px. "
            "Capture a taller (portrait) screenshot."
        )

    image = image.resize((TARGET_W, scaled_h), Image.LANCZOS)

    top = (scaled_h - TARGET_H) // 2
    image = image.crop((0, top, TARGET_W, top + TARGET_H))

    assert image.size == (TARGET_W, TARGET_H), image.size
    image.save(destination, "PNG", optimize=True)
    print(f"{source.name} -> {destination.name} ({image.width}x{image.height})")


def main(argv: list[str]) -> None:
    if len(argv) < 2:
        raise SystemExit(__doc__)

    for index, raw in enumerate(argv[1:]):
        source = Path(raw)
        if not source.exists():
            raise SystemExit(f"not found: {source}")
        name = (
            "undo-screenshot-1179x2556.png"
            if index == 0
            else f"undo-screenshot-1179x2556-{index + 1}.png"
        )
        convert(source, OUT_DIR / name)


if __name__ == "__main__":
    main(sys.argv)
