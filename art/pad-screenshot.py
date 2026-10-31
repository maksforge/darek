#!/usr/bin/env python3
"""Pad a device screenshot out to Play's 9:16 phone-screenshot ratio.

Phones are taller than the 9:16 Play documents for phone screenshots (a 1220x2712 phone is 2.22:1).
Rather than crop the UI or squash it, this centres the shot on a 9:16 canvas filled with the
screenshot's own edge colour, so the padding reads as background rather than as letterboxing.

    python3 art/pad-screenshot.py <in.png> <out.png>
    python3 art/pad-screenshot.py <in.png> <out.png> --crop-status-bar [--status-bar-px N]

Prefer emptying the status bar at capture time: `screenshots.py` turns on SystemUI demo mode
(`set_demo_status_bar` in `scripts/edge-to-edge.py`), which keeps the shot the shape of a phone.
`--crop-status-bar` is for a ROM that ignores demo mode, such as MIUI 12.5, where the bar would
otherwise show Do Not Disturb's crossed bell, notification icons and a network-speed readout.
"""

import argparse
from collections import Counter

from PIL import Image

RATIO_W, RATIO_H = 9, 16

# A device constant, not a Play one: the portrait `statusBars` inset `scripts/edge-to-edge.py` measured
# on a 1220x2712, 480dpi phone, where it also covers the punch-hole cutout.
STATUS_BAR_PX = 130


def edge_colour(img):
    """Most common pixel down the left and right edges — the app's background, not the
    status bar or the nav bar, both of which only touch the top and bottom."""
    px = img.load()
    w, h = img.size
    counts = Counter()
    for y in range(h // 8, h - h // 8):
        counts[px[0, y]] += 1
        counts[px[w - 1, y]] += 1
    return counts.most_common(1)[0][0]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("src")
    parser.add_argument("dst")
    parser.add_argument(
        "--crop-status-bar",
        action="store_true",
        help="drop the top --status-bar-px pixels (status bar and cutout) before padding",
    )
    parser.add_argument(
        "--status-bar-px",
        type=int,
        default=STATUS_BAR_PX,
        help="the phone's portrait statusBars inset (default: %(default)s); "
        "the test Redmi Note 8 Pro reads 76 in `dumpsys window`",
    )
    args = parser.parse_args()

    src, dst = args.src, args.dst
    img = Image.open(src).convert("RGB")
    if args.crop_status_bar:
        img = img.crop((0, args.status_bar_px, img.size[0], img.size[1]))
    w, h = img.size

    target_w = max(w, round(h * RATIO_W / RATIO_H))
    target_h = max(h, round(w * RATIO_H / RATIO_W))
    # Only one dimension actually grows; the other is already at or past the ratio.
    if target_w / target_h > RATIO_W / RATIO_H:
        target_h = round(target_w * RATIO_H / RATIO_W)
    else:
        target_w = round(target_h * RATIO_W / RATIO_H)

    canvas = Image.new("RGB", (target_w, target_h), edge_colour(img))
    canvas.paste(img, ((target_w - w) // 2, (target_h - h) // 2))
    canvas.save(dst, "PNG", optimize=True)

    ratio = target_w / target_h
    cropped = f" (status bar cropped, -{args.status_bar_px}px)" if args.crop_status_bar else ""
    print(f"{dst}  {w}x{h} -> {target_w}x{target_h}  ({ratio:.4f}, 9:16 = {RATIO_W / RATIO_H:.4f}){cropped}")


if __name__ == "__main__":
    main()
