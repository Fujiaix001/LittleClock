# LittleClock 4.0.19

## Changes

- Changes the linked-size mode to use the original 4.0.13-style single,
  wrap-content vertical clock panel.
- In that mode, the whole panel is scaled and dragged as one object; the
  three display blocks cannot be moved independently.
- Keeps the overlap mode, automatic-avoidance mode, independent block
  positions, reset-layout setting, and minimal weather-location setting.
- Preserves independent block positions when switching away from the single
  panel mode, so changing modes does not erase the other layout.

## Verification checklist

- `整體放大（單一面板、一起移動）` keeps the original vertical spacing while
  scaling the complete panel.
- Long-pressing any visible item in that mode moves the complete panel.
- Switching to `固定位置（可重疊）` or `自動避讓（原始效果）` restores independent
  block behavior.
- The reset setting restores both the independent layout and the single-panel
  default position.
