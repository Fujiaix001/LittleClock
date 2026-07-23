#!/usr/bin/env python3
"""Generate legacy and adaptive Android launcher resources from the approved icon."""

from pathlib import Path
from collections import deque
import sys

from PIL import Image


LEGACY = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def exterior_mask(image: Image.Image) -> list[bool]:
    width, height = image.size
    pixels = image.load()
    seen = [False] * (width * height)
    queue: deque[tuple[int, int]] = deque()

    def is_background(x: int, y: int) -> bool:
        r, g, b, _ = pixels[x, y]
        return min(r, g, b) > 145 and max(r, g, b) - min(r, g, b) < 35

    for x in range(width):
        if is_background(x, 0):
            queue.append((x, 0))
        if is_background(x, height - 1):
            queue.append((x, height - 1))
    for y in range(height):
        if is_background(0, y):
            queue.append((0, y))
        if is_background(width - 1, y):
            queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if seen[index] or not is_background(x, y):
            continue
        seen[index] = True
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))
    return seen


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: prepare_icon.py SOURCE_PNG APP_SRC_MAIN")
    source = Path(sys.argv[1])
    app_main = Path(sys.argv[2])
    original = Image.open(source).convert("RGBA")

    # Crop around the approved rounded-square artwork, then make the outside transparent.
    dark_bbox = Image.new(
        "L", original.size, 0
    )
    source_pixels = original.load()
    mask_pixels = dark_bbox.load()
    for y in range(original.height):
        for x in range(original.width):
            r, g, b, _ = source_pixels[x, y]
            if r < 100 and g < 120 and b < 140:
                mask_pixels[x, y] = 255
    bbox = dark_bbox.getbbox()
    if bbox is None:
        raise RuntimeError("cannot find icon background")
    left, top, right, bottom = bbox
    side = max(right - left, bottom - top)
    cx = (left + right) // 2
    cy = (top + bottom) // 2
    crop_box = (cx - side // 2, cy - side // 2, cx - side // 2 + side, cy - side // 2 + side)
    icon = original.crop(crop_box)

    outside = exterior_mask(icon)
    icon_pixels = icon.load()
    for y in range(icon.height):
        for x in range(icon.width):
            if outside[y * icon.width + x]:
                r, g, b, _ = icon_pixels[x, y]
                softness = max(0, min(255, (170 - min(r, g, b)) * 12))
                icon_pixels[x, y] = (r, g, b, softness)

    for density, size in LEGACY.items():
        directory = app_main / "res" / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        icon.resize((size, size), Image.Resampling.LANCZOS).save(directory / "ic_launcher.png", optimize=True)

    # Adaptive foreground retains only the cyan picture mark and warm-white time.
    foreground = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    fg_pixels = foreground.load()
    icon_pixels = icon.load()
    width, height = icon.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = icon_pixels[x, y]
            cyan = g > 125 and b > 115 and g > r * 1.45
            clock_area = width * 0.25 < x < width * 0.78 and height * 0.25 < y < height * 0.50
            warm_white = clock_area and min(r, g, b) > 125 and max(r, g, b) - min(r, g, b) < 55
            if cyan or warm_white:
                fg_pixels[x, y] = (r, g, b, a)

    adaptive_sizes = {density: size * 9 // 4 for density, size in LEGACY.items()}
    for density, size in adaptive_sizes.items():
        directory = app_main / "res" / f"drawable-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        foreground.resize((size, size), Image.Resampling.LANCZOS).save(
            directory / "ic_launcher_foreground.png", optimize=True
        )
        resized = foreground.resize((size, size), Image.Resampling.LANCZOS)
        mono = Image.new("RGBA", resized.size, (255, 255, 255, 0))
        mono.putalpha(resized.getchannel("A"))
        mono.save(directory / "ic_launcher_monochrome.png", optimize=True)


if __name__ == "__main__":
    main()
