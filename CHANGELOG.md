# 2.9.2
Dozens of fixes & optimizations here.
### Compatibility
- Refactor Distant Horizons Compat to apply bottomDim, alpha etc.
  - Fix cloudLayer flicker when mesh changing for DH 3.0.
  - Fix DH cloud no removed but frozen when SFCR render is disabled.
  - Add an option for threaded remeshing DH cloud.
- Update ParticleRain compat version to  v4-beta.8+.
- Now terrainDodge no longer detect `SKY_LIGHT` but `block.isAir()`.
- Remove arch-api dependencies cuz' it no update, still...
### Function
- Fix terrainDodge calculating out a wrong position.
- Fix biomeDetectUseLoadedChunk calculating out a wrong position.
### Improves & Bugfixes
- Improved usability of custom dimension, now you can create/delete custom dimension config file in game.
    - Optimize config IO.
    - Move config to subfolder `.minecraft/config/sfcr/`. Old config will be moved automatically when loaded.
    - Remove unshared config from custom dimension config file.
    - Add an option to delete current custom config file when exiting configScreen.
- SFCR packet will not send to player who wasn't have SFCR installed.
- Fix weather density has stop to update when 'enableServer' is disabled.
- Bump version to .9 to reduce confuse.

# 2.9.1
- remove arch-api dependencies cuz' it no update yet.
- disabled DHCompat cuz' it no update yet.
- update ParticleRain compat version to v4-beta.8+.
- bump version to reduce confuse.

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
