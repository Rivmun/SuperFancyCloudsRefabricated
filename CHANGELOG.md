# 1.9.1.1
- Fix particle rain compatibility for ParticleRain v4-beta.8+.
- 

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
