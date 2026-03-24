# 2.2.0
Use stonecutter to manager multi-version.

For technical reason, there is no more merged jar will release. Please select the correct version for the loader you are using.

Full changelog see 1.9.0, here just for 2.x
### Change
- View culling default disabled because its efficiency is no good.
- Slightly refactor renderer, for:
### Fix
- Fix cloud block size cannot change.
- Fix No Cloud No Rain pos calculation incorrectly that makes rain particles jumped in/out when player moving across border of cloud cover zone.
- Fix cloud refresh lag when Y changed that cause cloud bottom missing when player fell from sky.
