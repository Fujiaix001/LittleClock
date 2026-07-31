# LittleClock 4.0.12 — UNUSABLE CHECKPOINT

Status: **Do not distribute or install as a usable release.**

Version 4.0.12 is intentionally preserved as a development checkpoint for the
independent clock-unit layout experiment. Continue the next repair from this
checkpoint instead of deleting or rewriting its history.

## Confirmed problems

- Moving independently scaled units with overlapping backdrops can produce
  corrupted rendering instead of normal translucent overlap.
- Clock text can intermittently disappear while a unit is moving.
- Transformed clock text can cover the settings screen and make its controls
  unusable.

## Preserved behavior under development

- The previous linked/group movement and scaling behavior remains available.
- Free-layout mode stores independent positions and scales for time, date, and
  weather.
- Free-layout mode attempts to provide one backdrop per display unit.

The local APK artifacts remain in `dist/`, which is intentionally excluded from
Git by the repository's existing `.gitignore` rules.
