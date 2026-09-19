"""Temporary: render the generated icons through the real launcher masks.

Usage:
    python docs/shipaton/icon_preview.py && open docs/shipaton/icon-preview.html
"""

from __future__ import annotations

import base64
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent
ROOT = OUT_DIR.parents[1]
RES = ROOT / "app" / "src" / "main" / "res"


def data_uri(path: Path) -> str:
    mime = "image/webp" if path.suffix == ".webp" else "image/png"
    return f"data:{mime};base64,{base64.b64encode(path.read_bytes()).decode()}"


bg = data_uri(RES / "drawable-xxxhdpi" / "ic_launcher_background.png")
fg = data_uri(RES / "drawable-xxxhdpi" / "ic_launcher_foreground.png")
mono = data_uri(RES / "drawable-xxxhdpi" / "ic_launcher_monochrome.png")
legacy = data_uri(RES / "mipmap-xxxhdpi" / "ic_launcher.webp")
legacy_round = data_uri(RES / "mipmap-xxxhdpi" / "ic_launcher_round.webp")
submission = data_uri(OUT_DIR / "undo-icon-1024.png")

HTML = f"""<!doctype html>
<meta charset="utf-8">
<title>Undo icon preview</title>
<style>
  body {{ background:#101018; color:#e7e6f0; font:14px system-ui, sans-serif; padding:24px; }}
  h2 {{ font-size:13px; font-weight:600; letter-spacing:.08em; text-transform:uppercase; color:#a79df5; margin:28px 0 10px; }}
  .row {{ display:flex; gap:28px; align-items:flex-end; flex-wrap:wrap; }}
  .item {{ text-align:center; font-size:11px; color:#8b88a8; }}
  .layer {{ position:relative; overflow:hidden; }}
  .layer img {{ position:absolute; }}
  .checker {{ background:repeating-conic-gradient(#1b1b26 0 25%, #24242f 0 50%) 0 0/16px 16px; }}
</style>

<h2>Adaptive icon — circle mask (Pixel) / 72dp of 108dp</h2>
<div class="row" id="row1"></div>

<h2>Adaptive icon — squircle mask / 72dp of 108dp</h2>
<div class="row" id="row2"></div>

<h2>Legacy mipmaps at true launcher sizes (48dp / 72dp)</h2>
<div class="row" id="row3"></div>

<h2>Themed icon (Android 13+, monochrome tinted by system) and the submission icon</h2>
<div class="row" id="row4"></div>

<script>
const BG = "{bg}", FG = "{fg}", MONO = "{mono}", LEGACY = "{legacy}",
      ROUND = "{legacy_round}", SUB = "{submission}";

// A launcher shows the inner 72dp of the 108dp layer, then applies its own mask.
function adaptive(size, mask, label) {{
  const visible = size * 72 / 108;
  const el = document.createElement('div');
  el.className = 'item';
  el.innerHTML = `<div class="layer" style="width:${{visible}}px;height:${{visible}}px;border-radius:${{mask}}">
      <img src="${{BG}}" style="width:${{size}}px;height:${{size}}px;left:${{-(size - visible) / 2}}px;top:${{-(size - visible) / 2}}px">
      <img src="${{FG}}" style="width:${{size}}px;height:${{size}}px;left:${{-(size - visible) / 2}}px;top:${{-(size - visible) / 2}}px">
    </div><div>${{label}}</div>`;
  return el;
}}

function plain(src, size, label, css = '') {{
  const el = document.createElement('div');
  el.className = 'item';
  el.innerHTML = `<img src="${{src}}" style="width:${{size}}px;height:${{size}}px;${{css}}"><div>${{label}}</div>`;
  return el;
}}

const r1 = document.getElementById('row1'), r2 = document.getElementById('row2'),
      r3 = document.getElementById('row3'), r4 = document.getElementById('row4');

// Circle mask, as Android 13+ and most launchers draw it.
for (const s of [48, 96, 160, 260]) r1.append(adaptive(s, '50%', s + 'dp'));
// Squircle-ish mask, close to the Pixel launcher.
for (const s of [48, 96, 260]) r2.append(adaptive(s, '26%', s + 'dp'));

// Legacy icons are drawn as-is on old launchers (they are pre-masked here).
for (const s of [48, 72, 144]) {{
  r3.append(plain(LEGACY, s, 'ic_launcher ' + s + 'px'));
  r3.append(plain(ROUND, s, 'ic_launcher_round ' + s + 'px'));
}}

// Themed icon: the system keeps only the alpha channel and tints it.
const themed = document.createElement('div');
themed.className = 'item';
themed.innerHTML = `<div class="checker" style="padding:8px;border-radius:12px">
    <img src="${{MONO}}" style="width:120px;height:120px;filter:invert(1) sepia(1) saturate(0) brightness(0.85)">
  </div><div>monochrome layer, tinted</div>`;
r4.append(themed);
r4.append(plain(SUB, 240, 'undo-icon-1024.png', 'border-radius:22%'));
</script>
"""

path = OUT_DIR / "icon-preview.html"
path.write_text(HTML, encoding="utf-8")
print(f"{path.relative_to(ROOT)}  ({path.stat().st_size / 1024:.0f} KB)")
