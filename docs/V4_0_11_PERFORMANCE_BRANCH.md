# 4.0.11 performance branch

Branch: `codex/v4.0.11-performance`

## Status: DEPRECATED — DO NOT INSTALL

This branch is retained for history only. Its independent scaling still draws
the enlarged text inside the original layout-sized frame, so scaled glyphs can
be clipped. Do not use its APKs as a release or as a performance baseline.

The main branch was not changed by this deprecation.

This branch deliberately keeps LittleClock at version 4.0.11. It is a focused,
reusable performance baseline rather than a new product release.

## Change

The photo pan remains the same 43-second Ken Burns-style movement, but full
photo matrix updates are throttled:

- Android 4.4 and newer: at most 15 FPS (the original intended 67 ms cadence).
- Android 4.2/4.3: at most 10 FPS, appropriate for the very slow movement and
  lower GPU/CPU headroom.
- The final animation frame is always applied.

## Why this is safe to reuse

Only calls that update the `ImageView` matrix are limited. Photo decoding,
slideshow scheduling, clock updates, gesture handling, and all visual layout
behavior are unchanged. Low-power mode still disables photo panning entirely.

Later feature branches can take this commit directly without inheriting the
4.0.12 free-layout experiment.
