# 1.9.2
Dozens of fixes and optimizations here.
### Compatibility Fixes
- Upgrade compatibility of ParticleRain for v4-beta.8+.
- Refactor DHCompat renderer, to allow alpha, bottomDim and others.
  - Fix cloudLayer flicker when mesh changing for DH 3.0.
  - Fix DH cloud no removed but frozen when SFCR render is disabled.
  - Add an option for threaded remeshing DH cloud.
- Fix cloud disappear when work with 'Round Robin Chunk Relighting' from Nostalgic Tweaks at night or raining.
  - Now terrain dodge detect blockState.isAir instead of SKY_LIGHT == 15.
### Function Fixes
- Fix terrain dodge calculating out a wrong position.
- Fix biome detect calculating out a wrong position.
- Fix view culling incorrectly applied to inner faces or face which is too close to camera.
### Improves & Bugfixes
- Improved usability of custom dimension, now you can create/delete custom dimension config file in game.
  - Optimize config IO.
  - Move config to subfolder '.minecraft/config/sfcr/'. Old config will be moved automatically when loaded.
  - Remove unshared config from custom dimension config file.
  - Add an option to delete current custom config file when exiting configScreen.
- Add an option for forcibly rendering when cloudHeight is not 'Follow Vanilla', works like ~v1.7.
  - Default cloudHeight now set to 'Follow Vanilla'.
- Remove redundant precipitation density config for mc < 1.20. (they use downfall directly)
- Refactor face compressor to reduce memory use.
- SFCR packet will not send to player who wasn't have SFCR installed.
- Slightly improved view culling performance.
- Now cloudHeight step by cloudBlockHeight in configScreen.
- Fix weather density has stop to update when 'enableServer' is disabled.
- Fix renderDistance only save to default config file when renderDistanceFitToView is enabled.
- Fix concurrent violate crash when smooth change is enabled.
- Fix smooth change not work. (but it's still very...ugly...)

# 1.9.1
### Fix
- Fix start up crash on Forge side causing by MixinExtras missing when your modpacks have multiple copy integrated.
- Fix cloud color can set to fully transparency that let cloud seems to disappear.
- Fix NCNR mixin for mc < 1.20.
- (Hopefully) Fix mesh rebuild delay when only Y changed that causing cloud bottom face disappear when you fell from sky.
- Fix a missing Mixin for NCNR logically function.
### Known Issue
- Due to Mixin priority issue, NCNR function may override by mods that modified renderSnowAndRain & tickRain, such as SereneSeasons. This's may not to be fixed.

# 1.9.0
##### 2026-3-21
Merge all repositories of version 1.x into Stonecutter - Arch-loom build system. It may reduce the difference in features and patches between these versions. (while also reducing the effort of porting updates. Well, that's more important...)

For technical reason, there is no more merged jar will release. Please select the correct version for the loader you are using.

On Forge side, we embedded Llamalad7 's MixinExtras that makes mod jar *slightly* bigger.
### New
- Compat to serene season and fabric seasons. (close #118 )
- Add No Cloud No Rain in logically (experimental).
- Add client side command `/sfcr` to open configScreen (exclude 1.16.5).
    - Rename server side command `/sfcr` to `/sfcr help`.
- Add some debug feature for mixin and sampler.
### Change
- Refactor sampler.
- Arrange configScreen structure.
- Other internal fixes and improved.
### Fix
- Fix server side updater register to wrong event.
- Fix `/sfcr enable` command apply to wrong option.
- Fix leaves block still gen dripping water particle when NCNR enable. 
- Fix DH Compat cloud no applying blush correctly.
- fix pre-detect sometimes return fake thunder when weather is clear, that causes cloud density to increase abnormally then suddenly return to normal.
