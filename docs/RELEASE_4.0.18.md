# LittleClock 4.0.18

## Changes

- Changes the linked group-size mode into a true group layout mode: the time,
  date, and weather blocks keep their relative spacing and can only be moved
  together.
- Keeps the existing overlap and automatic-avoidance size modes unchanged.
- Adds an optional weather setting to display only the most local part of a
  comma-separated location name.

## Verification checklist

- In `群組放大（保持間隔、一起移動）`, long-pressing any visible clock block
  moves all visible blocks together.
- Pinch-to-zoom in group mode scales the parent clock surface without changing
  the spacing between blocks.
- The overlap and automatic-avoidance modes still allow their previous
  individual movement/size behavior.
- `地名只顯示最小單位` changes `Xinyi District, Taipei City, Taiwan` to
  `Xinyi District` without changing the saved weather coordinates.
