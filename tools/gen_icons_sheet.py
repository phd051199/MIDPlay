#!/usr/bin/env python3
"""Pack the menu/player icons into one sprite sheet: res/icons.png.

J2ME loads this sheet ONCE (Configuration.loadIcons) and cuts regions out with
Image.createImage(sheet, x, y, w, h, 0). The icon order, cell size and column
count here MUST match Configuration.java's region() calls — keep them in sync.

Layout: 4 columns, 42px square cell, each icon pasted at the top-left of its
cell. Every icon is <= 42px on both axes, so each native-size region fits its
cell. Run after changing any source icon in assets/icons/:

    python3 tools/gen_icons_sheet.py
"""
import sys
from pathlib import Path
from PIL import Image

# (field, source file, expected w, expected h) — order == Configuration region index.
ICONS = [
    ("folder",     "FolderSound.png",      42, 40),
    ("music",      "MusicDoubleNote.png",  36, 36),
    ("search",     "Magnifier.png",        36, 33),
    ("favorite",   "Heart.png",            36, 36),
    ("playlist",   "Album.png",            36, 36),
    ("settings",   "Setting.png",          36, 36),
    ("info",       "Information.png",      36, 36),
    ("recent",     "Recent.png",           36, 36),
    ("equalizer",  "Equalizer.png",        36, 36),
    ("play",       "Play.png",             32, 32),
    ("pause",      "Pause.png",            32, 32),
    ("next",       "Next.png",             32, 32),
    ("prev",       "Previous.png",         32, 32),
    ("repeat",     "Repeat.png",           20, 20),
    ("repeatOne",  "RepeatOne.png",        20, 20),
    ("shuffle",    "Shuffle.png",          20, 20),
]

CELL = 42
COLS = 4
ROWS = (len(ICONS) + COLS - 1) // COLS

SRC = Path(__file__).resolve().parent.parent / "assets" / "icons"
OUT = Path(__file__).resolve().parent.parent / "res" / "icons.png"


def main() -> int:
    sheet = Image.new("RGBA", (COLS * CELL, ROWS * CELL), (0, 0, 0, 0))
    print(f"index field        col row   x   y    w   h   file")
    for i, (field, fname, w, h) in enumerate(ICONS):
        src = Image.open(SRC / fname).convert("RGBA")
        if src.size != (w, h):
            print(f"  size mismatch {fname}: got {src.size}, expected ({w}, {h})", file=sys.stderr)
            return 1
        col, row = i % COLS, i // COLS
        x, y = col * CELL, row * CELL
        sheet.paste(src, (x, y))
        print(f"{i:5d} {field:11s} {col:3d} {row:3d} {x:3d} {y:3d} {w:4d} {h:4d}   {fname}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    print(f"\nwrote {OUT}  ({OUT.stat().st_size} bytes, {sheet.size[0]}x{sheet.size[1]})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
