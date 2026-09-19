"""Generate the Undo icon set from one drawing.

Renders the app's "midnight observatory" identity — a deep plum radial background,
a quiet star field, a lavender crescent moon with a soft bloom and one sage-teal
dot — into the two things that must never drift apart:

* the submission / store icon: ``undo-icon-1024.png`` and ``undo-icon-512.png``
* the Android launcher icon: ``drawable-*dpi/ic_launcher_{background,foreground,
  monochrome}.png`` (the adaptive icon layers referenced by
  ``mipmap-anydpi-v26/ic_launcher.xml``) plus the legacy ``mipmap-*dpi/ic_launcher
  .webp`` and ``ic_launcher_round.webp``

Every shape is expressed as a fraction of the canvas, so all sizes are the same
mark and the launcher icon can never disagree with the icon submitted to a store.

Usage:
    python docs/shipaton/make_icon.py
"""

from __future__ import annotations

import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT_DIR = Path(__file__).resolve().parent
ROOT = OUT_DIR.parents[1]
RES_DIR = ROOT / "app" / "src" / "main" / "res"

SIZE = 1024
CENTER = SIZE // 2

# Palette lifted from app/src/main/java/com/zhiend/regretnote/ui/theme/Color.kt
INNER = (43, 36, 96)  # UndoPrimaryContainer
OUTER = (9, 9, 15)  # MidnightBackground
MOON_TOP = (231, 226, 255)  # UndoOnPrimaryContainer
MOON_BOTTOM = (167, 157, 245)  # UndoPrimary
GLOW = (167, 157, 245)
ACCENT = (143, 191, 187)  # UndoSecondary (sage-teal)

# Composition geometry, as fractions of the canvas. The odd offsets are the
# original 1024px measurements kept exact (14/1024, 10/1024).
MOON_RADIUS = 0.255
MOON_CENTER = (0.5 - 14 / SIZE, 0.5 + 10 / SIZE)
DOT_CENTER = (0.715, 0.735)
DOT_RADIUS = 0.030
STAR_CLEAR_RADIUS = 0.30

# Adaptive icon: a 108dp layer, in pixels per density bucket.
ADAPTIVE_LAYERS = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
# Legacy (pre-API-26) launcher icon: 48dp, in pixels per density bucket.
LEGACY_ICONS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
# How much of the adaptive layer the mark covers. A launcher shows the inner 72dp
# and may mask down to a 66dp circle, so 0.78 keeps the moon and the dot inside
# every mask without the icon looking lost in the middle.
SAFE_SCALE = 0.78
# The maskable circle a launcher may apply, as a fraction of the layer.
SAFE_RADIUS = 0.5 * 66 / 108


def radial_background(size: int) -> Image.Image:
    """Plum glow in the centre falling off to near-black at the edges."""
    radial = (
        Image.radial_gradient("L")
        .resize((size, size), Image.BILINEAR)
        .point(lambda v: int(255 * (1.0 - v / 255.0) ** 1.35))
    )
    # Map the mask through two flat colours: OUTER underneath, INNER composited with
    # the radial mask on top.
    base = Image.new("RGB", (size, size), OUTER)
    inner = Image.new("RGB", (size, size), INNER)
    base.paste(inner, (0, 0), radial)
    return base


def star_field(size: int, clear: float = STAR_CLEAR_RADIUS) -> Image.Image:
    """Small, low-contrast stars — deliberately quiet, like CosmicBackground.

    Proportional to the canvas: the 1024px reference gets the full field, the
    launcher mipmaps get fewer, smaller stars (the rng is seeded, so a small
    canvas shows a subset of the same field, never a different one).
    """
    rng = random.Random(20260930)
    count = max(24, round(130 * (size / SIZE) ** 1.35))
    scale = max(size / SIZE, 0.55)
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x = rng.uniform(0, size)
        y = rng.uniform(0, size)
        # Keep the middle clear so the crescent stays the subject.
        if (x - size / 2) ** 2 + (y - size / 2) ** 2 < (size * clear) ** 2:
            continue
        r = rng.uniform(1.0, 3.4) * scale
        alpha = int(rng.uniform(26, 96))
        tint = rng.choice([(233, 230, 244), (183, 179, 202), (167, 157, 245)])
        draw.ellipse([x - r, y - r, x + r, y + r], fill=(*tint, alpha))
    return layer.filter(ImageFilter.GaussianBlur(0.6 * scale))


def crescent_mask(size: int) -> Image.Image:
    """A right-opening crescent: a disc with an offset disc subtracted."""
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    radius = size * MOON_RADIUS
    cx, cy = size * MOON_CENTER[0], size * MOON_CENTER[1]
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=255)
    cut = radius * 0.94
    cut_cx, cut_cy = cx + radius * 0.46, cy - radius * 0.30
    draw.ellipse([cut_cx - cut, cut_cy - cut, cut_cx + cut, cut_cy + cut], fill=0)
    return mask


def vertical_gradient(
    size: int, top: tuple[int, int, int], bottom: tuple[int, int, int]
) -> Image.Image:
    grad = Image.new("RGB", (1, size))
    for y in range(size):
        t = y / (size - 1)
        grad.putpixel(
            (0, y),
            tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)),
        )
    return grad.resize((size, size), Image.BILINEAR)


def dot_layer(size: int) -> Image.Image:
    """A single sage-teal dot — "the one thing you'd change"."""
    dot = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(dot)
    r = size * DOT_RADIUS
    cx, cy = size * DOT_CENTER[0], size * DOT_CENTER[1]
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*ACCENT, 235))
    return dot.filter(ImageFilter.GaussianBlur(max(1.2 * size / SIZE, 0.4)))


def render_glyph(size: int) -> Image.Image:
    """The moon, its bloom and the sage dot on a transparent canvas."""
    glyph = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = crescent_mask(size)

    # Soft bloom behind the moon.
    for blur, alpha in ((size * 0.09, 42), (size * 0.045, 86)):
        bloom = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        bloom.paste(Image.new("RGBA", (size, size), (*GLOW, alpha)), (0, 0), mask)
        glyph.alpha_composite(bloom.filter(ImageFilter.GaussianBlur(blur)))

    # The moon itself, with a vertical light gradient.
    moon = vertical_gradient(size, MOON_TOP, MOON_BOTTOM).convert("RGBA")
    glyph.paste(moon, (0, 0), mask)

    glyph.alpha_composite(dot_layer(size))
    return glyph


def render_monochrome(size: int, scale: float = SAFE_SCALE) -> Image.Image:
    """Themed-icon layer: crescent and dot as a plain white silhouette.

    The bloom and the star field are left out on purpose — Android keeps only the
    alpha channel of this layer, and a blurred halo would tint into a grey smudge.
    """
    inner = max(1, round(size * scale))
    glyph = Image.new("RGBA", (inner, inner), (0, 0, 0, 0))
    white = Image.new("RGBA", (inner, inner), (255, 255, 255, 255))
    glyph.paste(white, (0, 0), crescent_mask(inner))
    draw = ImageDraw.Draw(glyph)
    r = inner * DOT_RADIUS
    cx, cy = inner * DOT_CENTER[0], inner * DOT_CENTER[1]
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 255))

    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    layer.alpha_composite(glyph, ((size - inner) // 2,) * 2)
    return layer


def render_full(size: int) -> Image.Image:
    """The complete mark: radial background, star field, then the moon."""
    canvas = radial_background(size).convert("RGBA")
    canvas.alpha_composite(star_field(size))
    canvas.alpha_composite(render_glyph(size))
    return canvas


def _supersampled_mask(size: int, shape: str, radius: float = 0.22, ss: int = 4) -> Image.Image:
    """Antialiased legacy launcher mask, drawn 4x then downsampled."""
    big = Image.new("L", (size * ss, size * ss), 0)
    draw = ImageDraw.Draw(big)
    box = [0, 0, size * ss - 1, size * ss - 1]
    if shape == "circle":
        draw.ellipse(box, fill=255)
    else:
        draw.rounded_rectangle(box, radius=size * radius * ss, fill=255)
    return big.resize((size, size), Image.LANCZOS)


def _masked(source: Image.Image, mask: Image.Image) -> Image.Image:
    out = Image.new("RGBA", source.size, (0, 0, 0, 0))
    out.paste(source, (0, 0), mask)
    return out


def adaptive_layers(size: int) -> list[tuple[str, Image.Image]]:
    """(file stem, image) for one density bucket of the adaptive icon."""
    # Background: full-bleed gradient and stars. Its star field keeps a narrower
    # clear zone than the 1024px reference, because only the inner 72dp is shown
    # and the mark covers the middle of it.
    background = radial_background(size).convert("RGBA")
    background.alpha_composite(star_field(size, clear=STAR_CLEAR_RADIUS * SAFE_SCALE))

    # Foreground: the mark scaled into the safe circle, centred on the layer.
    inner = max(1, round(size * SAFE_SCALE))
    foreground = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    foreground.alpha_composite(render_glyph(inner), ((size - inner) // 2,) * 2)

    return [
        ("ic_launcher_background", background),
        ("ic_launcher_foreground", foreground),
        ("ic_launcher_monochrome", render_monochrome(size)),
    ]


def write_launcher_icons() -> list[Path]:
    """Regenerate every launcher asset under app/src/main/res."""
    written: list[Path] = []

    for density, size in ADAPTIVE_LAYERS.items():
        out = RES_DIR / f"drawable-{density}"
        out.mkdir(parents=True, exist_ok=True)
        for stem, image in adaptive_layers(size):
            path = out / f"{stem}.png"
            image.save(path, "PNG", optimize=True)
            written.append(path)

    for density, size in LEGACY_ICONS.items():
        out = RES_DIR / f"mipmap-{density}"
        out.mkdir(parents=True, exist_ok=True)
        full = render_full(size)
        for stem, shape in (("ic_launcher", "square"), ("ic_launcher_round", "circle")):
            path = out / f"{stem}.webp"
            _masked(full, _supersampled_mask(size, shape)).save(
                path, "WEBP", lossless=True, quality=100, method=4
            )
            written.append(path)

    return written


def verify_launcher_icons() -> None:
    """Check the generated assets by pixel, since this machine cannot run an emulator."""
    problems: list[str] = []

    def check(condition: bool, message: str) -> None:
        if not condition:
            problems.append(message)

    for density, size in ADAPTIVE_LAYERS.items():
        out = RES_DIR / f"drawable-{density}"
        background = Image.open(out / "ic_launcher_background.png").convert("RGBA")
        foreground = Image.open(out / "ic_launcher_foreground.png").convert("RGBA")
        monochrome = Image.open(out / "ic_launcher_monochrome.png").convert("RGBA")

        check(
            background.size == foreground.size == monochrome.size == (size, size),
            f"{density}: unexpected layer size",
        )
        check(background.getchannel("A").getextrema() == (255, 255), f"{density}: background is not opaque")

        alpha = foreground.getchannel("A")
        for x, y in ((0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)):
            check(alpha.getpixel((x, y)) == 0, f"{density}: foreground corner is not transparent")
        # Nothing but the faintest bloom may sit outside the maskable 66dp circle.
        bleed = max(
            alpha.getpixel((x, y))
            for x in range(size)
            for y in range(size)
            if (x - size / 2) ** 2 + (y - size / 2) ** 2 > (size * SAFE_RADIUS * 1.02) ** 2
        )
        check(bleed <= 24, f"{density}: foreground bleeds {bleed}/255 outside the 66dp safe circle")
        check(alpha.getextrema()[1] == 255, f"{density}: foreground has no opaque pixels")
        check(monochrome.getchannel("A").getextrema() == (0, 255), f"{density}: monochrome is empty")
        # Where it is opaque it must be pure white, so the system tint stays clean.
        for channel in ("R", "G", "B"):
            difference = ImageChops.difference(monochrome.getchannel(channel), monochrome.getchannel("A"))
            check(difference.getextrema() == (0, 0), f"{density}: monochrome silhouette is not white")

    for density, size in LEGACY_ICONS.items():
        for stem in ("ic_launcher", "ic_launcher_round"):
            icon = Image.open(RES_DIR / f"mipmap-{density}" / f"{stem}.webp").convert("RGBA")
            check(icon.size == (size, size), f"{density}/{stem}: unexpected size")
            check(icon.getpixel((0, 0))[3] == 0, f"{density}/{stem}: corner is not transparent")
            check(icon.getpixel((size // 2, size // 2))[3] == 255, f"{density}/{stem}: centre is not opaque")
            bright = sum(icon.convert("L").histogram()[121:])
            share = bright / (size * size)
            check(0.02 < share < 0.35, f"{density}/{stem}: moon covers {share:.1%} of the icon")

    if problems:
        raise SystemExit("launcher icon checks failed:\n  " + "\n  ".join(problems))
    print("launcher icon checks: OK")


def main() -> None:
    canvas = render_full(SIZE)

    flat = Image.new("RGB", (SIZE, SIZE), OUTER)
    flat.paste(canvas, (0, 0), canvas)

    icon_1024 = OUT_DIR / "undo-icon-1024.png"
    icon_512 = OUT_DIR / "undo-icon-512.png"
    flat.save(icon_1024, "PNG", optimize=True)
    flat.resize((512, 512), Image.LANCZOS).save(icon_512, "PNG", optimize=True)

    written = [icon_1024, icon_512] + write_launcher_icons()

    for path in written:
        with Image.open(path) as check:
            label = f"{path.relative_to(ROOT)}"
            print(f"{label:<58s} {check.size[0]:>4d}x{check.size[1]:<4d} {check.mode}")

    verify_launcher_icons()


if __name__ == "__main__":
    main()
