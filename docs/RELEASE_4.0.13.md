# LittleClock 4.0.13 test release

Branch: `codex/v4.0.13`

Version 4.0.13 starts from the last usable 4.0.9 codebase. It does not include
the deprecated 4.0.11 scaling implementation or the unusable 4.0.12 free-layout
experiment.

## Changes

- Caps full-photo matrix updates at approximately 15 FPS while preserving the
  original pan duration and final frame.
- Keeps low-power mode behavior unchanged: photo panning remains disabled.
- Adds optional independent sizes for time, date, and weather.
- Fixes the 4.0.11 clipping problem by changing the components' measured text
  and icon sizes instead of scaling their drawing inside stale layout bounds.
- Reads scale preferences saved by both 4.0.11 and 4.0.12.

## Device test checklist

- Verify linked scaling still resizes the complete clock group.
- Disable "綁定時間、日期、天氣大小" and resize each visible component.
- Confirm enlarged glyphs and weather icons are not clipped.
- Rotate the device and confirm portrait and landscape sizes remain independent.
- Open Settings and return; confirm sizes persist and do not cover Settings.
- Enter and leave Pomodoro focus mode; confirm normal clock sizes are restored.
- With low-power mode off, confirm the slow photo pan completes normally.
