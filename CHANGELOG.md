# 1.9.0
##### 2026-3-21
### New
- Compat to serene season and fabric seasons. (close #118 )
- Add NCNR in logically.
- Add client side command `/sfcr` to open configScreen (exclude 1.16.5).
    - Rename server side command `/sfcr` to `/sfcr help`.
- Add debug of mixin and sampler
### Change
- Refactor sampler.
- Arrange configScreen structure.
- Other internal fixes and improved.
### Fix
- Fix server side updater register to wrong event.
- Fix `/sfcr enable` command apply to wrong config.
- Fix leaves block still gen dripping water particle when NCNR enable. 
- Fix DH Compat cloud no applying blush correctly.

Merged source code of 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.21.1 with Stonecutter - Arch-loom build system.

It may reduce the difference in features and patches between these versions. (while also reducing the effort of porting updates. Well, that's more important...)
