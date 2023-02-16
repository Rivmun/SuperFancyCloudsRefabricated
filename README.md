## Refabricated

This is an unofficial fabric port of **[SuperFancyClouds](https://modrinth.com/mod/superfancyclouds)**. Everything goes to its author [ami-wishes](https://modrinth.com/user/ami-wishes).

Check the awesome core here: https://github.com/ami-wishes/SuperFancyClouds

# SuperFancyClouds
A client/server side mod makes your Minecraft sky full of more stereoscopic and dynamic clouds.

[![Download from Curseforge](https://cf.way2muchnoise.eu/full_820317_downloads%20on%20Curseforge.svg?badge_style=flat)](https://www.curseforge.com/minecraft/mc-mods/superfancyclouds-refabricated)  [![Download From Modrinth](https://img.shields.io/modrinth/dt/superfancyclouds-refabricated?color=4&label=Download%20from%20Modrinth&style=flat-square&logo=modrinth)](https://modrinth.com/mod/superfancyclouds-refabricated)

![](https://cdn.modrinth.com/data/Hoop89kN/images/42c3dc7fb8ebb87ff95516f2a0e02399c0494277.png)

## Beyond the Original

### Feature
- Clouds height / density / width / thickness and other contents is **highly configurable**;
- Clouds can detect current **weather** to change its density;
- Clouds can detect current **biome** to change its density;
- **Changable** refresh rate and sample steps;
- **Sync** cloud position and shape between clients in multiplayer/server (since 1.5.0);
- And more...

### Fix
- Clouds will not be culled in view boundary;
- Clouds will not rendering in other dimensions without Skylight;

## Mod relationship

### Dependence
- **Fabric API** is required;
- **Cloth Config** is required to manage config;
- **Mod Menu** is optional to access config screen on client.

### Compatibility
- **Sodium Extra** affect cloud height of SFCR correctly.
- **Raised Clouds** may same as above but I didn't test it.

### Incompatibility
Obviously incompatible with most mod who modified cloud render system. Include **Cull Clouds**, **Extended Clouds** and the others. If you put SFCR with it together, they will throw some issue even crash.

## Q&A

### Few cloud?

It's maybe normal due to cloud noise sampler and dynamic density feature. You can decrease `sample steps`, `biome affect`, and increase `common density` in config screen to let the sampler generating more clouds. (The density curve is not straight but like a stair. You may need to move larger range on density slider to make it effect.)

Fog may also cover the clouds. You can disable it to check the clouds amounts.

If you set a high value of biome affect and play in desert or savanna, there may not be a cloud in the sky. Its based on biome downfall value and times with cloud density.

### Version supports?

- **1.19.2** is the preferred supported version of this mod.
  The .jar file should work with 1.19/1.19.1 but I didn't test it.
- **1.19.3** and **1.18.2** is seperately released version but I didn't do a full test.
  Its file marked as its version suffix.
- **Backport** to another version is **impossible** due to the big version difference of Minecraft render system.

### Client or Server?

Since 1.5.0, SFCRe is **optional** for two side. This means that you can connect to any server with/without installing it, and any server installed SFCR allows you connect without it. But no function will provide from server if you do so.

On server, there is a command list for its administrators to control SFCR feature & config remotely. Simply type in "_/sfcr_" then enter in chat bar you'll see it.

Client will send a sync request every 30s (by default, can changed by command). Server may response it then send necessary data back.

### Modpacks?

Free to use under [MIT License](https://github.com/Rivmun/SuperFancyCloudsRefabricated/blob/1.19.2/LICENSE).
