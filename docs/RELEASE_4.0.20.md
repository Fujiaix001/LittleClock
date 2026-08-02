# LittleClock 4.0.20

## Changes

- Separates clock arrangement from scaling behavior.
- Adds `原始排版（綁定）`, which uses the original vertical panel and moves
  all three displays together.
- Keeps two effects inside the original layout: `整體綁定` and
  `自動避讓（原始效果）`.
- Adds `自由排版（自行調整）`, where time, date, and weather can each be
  moved and resized without automatic repositioning.
- Migrates the 4.0.13-4.0.19 preferences into the new two-layer model while
  preserving separate original-panel and free-layout positions.

## Verification checklist

- Original linked layout scales and moves as one panel.
- Original avoidance layout changes the selected size and lets the vertical
  panel naturally separate the rows.
- Free layout keeps independent positions and never runs automatic avoidance.
- Reset restores the original linked layout and default sizes/positions.
