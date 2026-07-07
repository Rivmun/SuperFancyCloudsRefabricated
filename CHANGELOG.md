# 2.9.4.1
- Re-add isBiomeUseLoadedChunk option.
- Now clouds can be set to fully(?) opaque.
  - Default cloudColor now set to 0xB2FFFFFF (as same as vanilla).
  - ?: vanilla always have a little transparency in 2.0 that IDK why...
- Improved (but not fully fix) NCNRLogically performance, again...
- Improved debug performance.
  - Add '/sfcr debug api' command for dedicated server to debug NCNRLogically performance remotely.
- Internal biomeDetect improved.
- Internal mixin improved.

# 2.9.4
- Fix compatibility for Iris. Now 2.0 can work with shaderpacks.
  - Remember switch cloudType to 'Vanilla' in IrisConfig -> ShaderPackSettings -> Sky/Atmosphere -> Clouds.
  - Note that specific display effect based on specific shaderpacks.
- Fix NCNRLogically function potentially stuck server tick.
- Fix a potentially NPE in mixinPlugin.
- Add isCloud api function.

# 2.9.3.1
- Support mc26.2.
- Fix an issue that rebuild interval becomes shorter when fps is extremely high (thanks to Vulkan :P).
- Fix DhRenderRangeMulti not functioning.

# 2.9.3
Critical fixes about network.
- Fix network issue between client-dedicatedServer which is installed SFCR both.
  - Fix dimension payload register that causing client cannot log in.
  - Fix dedicated server crash when boot on fabric.
- Fix custom dimension feature no functioning when changed dimension on fabric.
- Fix server command will override by client command when connect to a server on fabric.
  - client command `/sfcr` now rename to `/sfcrconfig` to prevent that issue.
- Fix config will meaningless reload when open createNewWorld screen & connectingServer screen.
- Fix server config forgot to save after server command callback.
- Remove biomeDetectUseLoadedChunk option cuz it code already remove.
- Slightly improved threshold calculation.
- Mod icon update & other small fix.

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
- Fix biomeDetectUseChunk calculating out a wrong position.
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
