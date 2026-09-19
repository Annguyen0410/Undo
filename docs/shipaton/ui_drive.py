#!/usr/bin/env python3
"""Drive the running emulator by visible label, for UI review screenshots.

Usage:
  python docs/shipaton/ui_drive.py dump                 # list labelled nodes
  python docs/shipaton/ui_drive.py tap "Journal"        # tap a node by text/desc
  python docs/shipaton/ui_drive.py shot 03-insights     # screencap into shots/
  python docs/shipaton/ui_drive.py scroll 1200          # swipe up by N px

This exists because the emulator is the only honest way to review this UI and
tapping by guessed coordinates is how you end up screenshotting the wrong
screen. Not part of the app; see docs/shipaton/README.md for the loop.
"""

import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SHOTS = ROOT / "shots"
ADB = Path(
    os.environ.get("ANDROID_HOME", r"C:\Users\nguye\AppData\Local\Android\Sdk")
) / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb")
DEVICE_DUMP = "/sdcard/ui.xml"


def adb(*args: str) -> str:
    env = {**os.environ, "MSYS_NO_PATHCONV": "1"}
    result = subprocess.run(
        [str(ADB), *args], capture_output=True, text=True, env=env, timeout=120
    )
    return result.stdout


def nodes() -> list[tuple[str, str, int, int]]:
    adb("shell", "uiautomator", "dump", DEVICE_DUMP)
    raw = adb("exec-out", "cat", DEVICE_DUMP)
    start = raw.find("<hierarchy")
    tree = ET.fromstring(raw[start:])
    out = []
    for node in tree.iter("node"):
        text = node.get("text") or ""
        desc = node.get("content-desc") or ""
        if not text and not desc:
            continue
        x1, y1, x2, y2 = (int(v) for v in re.findall(r"\d+", node.get("bounds")))
        out.append((text or desc, node.get("bounds"), (x1 + x2) // 2, (y1 + y2) // 2))
    return out


def main() -> None:
    action = sys.argv[1] if len(sys.argv) > 1 else "dump"
    if action == "dump":
        for label, bounds, cx, cy in nodes():
            print(f"{bounds:22} ({cx:4d},{cy:4d})  {label[:70]}")
        return

    if action == "tap":
        wanted = sys.argv[2]
        exact = [n for n in nodes() if n[0] == wanted]
        partial = [n for n in nodes() if wanted.lower() in n[0].lower()]
        match = (exact or partial)
        if not match:
            raise SystemExit(f"no node matching {wanted!r}")
        label, bounds, cx, cy = match[0]
        adb("shell", "input", "tap", str(cx), str(cy))
        print(f"tapped {label!r} at ({cx},{cy}) {bounds}")
        return

    if action == "shot":
        SHOTS.mkdir(exist_ok=True)
        name = sys.argv[2]
        delay = float(sys.argv[3]) if len(sys.argv) > 3 else 1.5
        time.sleep(delay)
        data = subprocess.run(
            [str(ADB), "exec-out", "screencap", "-p"],
            capture_output=True,
            env={**os.environ, "MSYS_NO_PATHCONV": "1"},
            timeout=120,
        ).stdout
        (SHOTS / f"{name}.png").write_bytes(data)
        print(f"saved shots/{name}.png ({len(data) // 1024} KB)")
        return

    if action == "scroll":
        distance = int(sys.argv[2]) if len(sys.argv) > 2 else 1200
        adb("shell", "input", "swipe", "540", "1800", "540", str(1800 - distance), "300")
        print(f"scrolled {distance}px")
        return

    raise SystemExit(f"unknown action {action!r}")


if __name__ == "__main__":
    main()
