package com.rimo.sfcr.register;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.network.ConfigSyncMessage;
import com.rimo.sfcr.network.RuntimeSyncMessage;
import com.rimo.sfcr.network.Network;
import com.rimo.sfcr.util.CloudRefreshSpeed;
import java.util.List;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Command {
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(literal("sfcr")
					.executes(content -> {
						content.getSource().sendMessage(Text.of("- - - - - SFCR Help Page - - - - -"));
						content.getSource().sendMessage(Text.of("/sfcr - Show this page"));
						content.getSource().sendMessage(Text.of("/sfcr sync - Sync with server instantly"));
						if (!content.getSource().hasPermissionLevel(2))
							return 1;
						content.getSource().sendMessage(Text.of("/sfcr statu - Show runtime config"));
						content.getSource().sendMessage(Text.of("/sfcr [enable|disable] - Toggle SFCR server activity"));
						content.getSource().sendMessage(Text.of("/sfcr [cloud|density|biome] - Edit config"));
						content.getSource().sendMessage(Text.of("/sfcr biome [list|add|remove] - Manage ignored biome"));
						content.getSource().sendMessage(Text.of("/sfcr reload - Reload config, then force sync to every client"));
						content.getSource().sendMessage(Text.of("/sfcr save - Save runtime config to file"));
						return 1;
					})
					.then(literal("sync")
							.executes(content -> {
								Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
								SFCReMod.LOGGER.info("[SFCRe] cb: Send sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendMessage(Text.of("Manual requesting sync..."));
								return 1;
							})
							.then(literal("full").executes(content -> {
								Network.CONFIG_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new ConfigSyncMessage(SFCReMod.COMMON_CONFIG));
								Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
								SFCReMod.LOGGER.info("[SFCRe] cb: Send full sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendMessage(Text.of("Manual requesting sync..."));
								return 1;
							}))
							.then(literal("time").requires(source -> source.hasPermissionLevel(2))
									.then(argument("sec", IntegerArgumentType.integer()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSecPerSync(content.getArgument("sec", Integer.class));
										content.getSource().sendMessage(Text.of("Sync time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Sync per second is " + SFCReMod.COMMON_CONFIG.getSecPerSync()));
										return 1;
									})
							)
							.then(literal("toAllPlayers").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
								for (ServerPlayerEntity player : content.getSource().getServer().getPlayerManager().getPlayerList()) {
									Network.CONFIG_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new ConfigSyncMessage(SFCReMod.COMMON_CONFIG));
									Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
									player.sendMessage(Text.of("[SFCRe] Force sync request came from server..."));
								}
								content.getSource().sendMessage(Text.of("Force sync complete!"));
								SFCReMod.LOGGER.info("[SFCRe] cb: Force sync running by " + content.getSource().getDisplayName().getString());
								return 1;
							}))
					)
					.then(literal("statu").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						content.getSource().sendMessage(Text.of("- - - - - SFCR Mod Statu - - - - -"));
                        content.getSource().sendMessage(Text.of("§eStatu:           §r" + SFCReMod.COMMON_CONFIG.isEnableMod()));
                        content.getSource().sendMessage(Text.of("§eCloud height:    §r" + SFCReMod.COMMON_CONFIG.getCloudHeight()));
						content.getSource().sendMessage(Text.of("§eCloud Block Size:§r" + SFCReMod.COMMON_CONFIG.getCloudBlockSize()));
						content.getSource().sendMessage(Text.of("§eCloud Thickness: §r" + SFCReMod.COMMON_CONFIG.getCloudLayerThickness()));
                        content.getSource().sendMessage(Text.of("§eSample Step:     §r" + SFCReMod.COMMON_CONFIG.getSampleSteps()));
						content.getSource().sendMessage(Text.of("§eDynamic Density: §r" + SFCReMod.COMMON_CONFIG.isEnableWeatherDensity()));
						content.getSource().sendMessage(Text.of("§eDensity Threshld:§r" + SFCReMod.COMMON_CONFIG.getDensityThreshold()));
						content.getSource().sendMessage(Text.of("§eThrshld Multiplr:§r" + SFCReMod.COMMON_CONFIG.getThresholdMultiplier()));
                        content.getSource().sendMessage(Text.of("§ePre-Detect Time: §r" + SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime() / 20));
                        content.getSource().sendMessage(Text.of("§eChanging Speed:  §r" + SFCReMod.COMMON_CONFIG.getNumFromSpeedEnum(SFCReMod.COMMON_CONFIG.getDensityChangingSpeed())));
                        content.getSource().sendMessage(Text.of("§eCommon Density:  §r" + SFCReMod.COMMON_CONFIG.getCloudDensityPercent()));
                        content.getSource().sendMessage(Text.of("§eRain Density:    §r" + SFCReMod.COMMON_CONFIG.getRainDensityPercent()));
                        content.getSource().sendMessage(Text.of("§eThunder Density: §r" + SFCReMod.COMMON_CONFIG.getThunderDensityPercent()));
						content.getSource().sendMessage(Text.of("§eSnow Area Dens.: §r" + SFCReMod.COMMON_CONFIG.getSnowDensity()));
						content.getSource().sendMessage(Text.of("§eRain Area Dens.: §r" + SFCReMod.COMMON_CONFIG.getRainDensity()));
						content.getSource().sendMessage(Text.of("§eOther Area Dens.:§r" + SFCReMod.COMMON_CONFIG.getNoneDensity()));
                        content.getSource().sendMessage(Text.of("§eUsing Chunk:     §r" + SFCReMod.COMMON_CONFIG.isBiomeDensityByChunk()));
                        content.getSource().sendMessage(Text.of("§eUsing Loaded Chk:§r" + SFCReMod.COMMON_CONFIG.isBiomeDensityUseLoadedChunk()));
                        content.getSource().sendMessage(Text.of("Type [/sfcr biome list] to check ignored biome list."));
						return 1;
					}))
					.then(literal("enable").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableMod(content.getArgument("e", Boolean.class));
								content.getSource().sendMessage(Text.of("SFCR statu changed!"));
								return 1;
							}))
					)
					.then(literal("debug").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableDebug(content.getArgument("e", Boolean.class));
								content.getSource().sendMessage(Text.of("Debug statu changed!"));
								return 1;
							}))
					)
					.then(literal("cloud").requires(source -> source.hasPermissionLevel(2))
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendMessage(Text.of("Cloud height changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Cloud height is " + SFCReMod.COMMON_CONFIG.getCloudHeight()));
										return 1;
									})
							)
							.then(literal("size")
									.then(argument("size", IntegerArgumentType.integer(1, 4)).executes(content -> {
										switch (content.getArgument("size", Integer.class)) {
											case 1 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(2);
											case 2 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(4);
											case 3 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(8);
											case 4 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(16);
										}
										content.getSource().sendMessage(Text.of("Cloud size changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Cloud size is " + SFCReMod.COMMON_CONFIG.getCloudBlockSize()));
										return 1;
									})
							)
							.then(literal("thickness")
									.then(argument("thickness", IntegerArgumentType.integer(8, 64)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudLayerThickness(content.getArgument("thickness", Integer.class));
										content.getSource().sendMessage(Text.of("Cloud thickness changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Cloud thickness is " + SFCReMod.COMMON_CONFIG.getCloudLayerThickness()));
										return 1;
									})
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendMessage(Text.of("Sample step changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Sample steps is " + SFCReMod.COMMON_CONFIG.getSampleSteps()));
										return 1;
									})
							)
					)
					.then(literal("density").requires(source -> source.hasPermissionLevel(2))
							.then(literal("enable")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setEnableWeatherDensity(content.getArgument("e", Boolean.class));
										content.getSource().sendMessage(Text.of("Density statu changed!"));
										return 1;
									}))
							)
							.then(literal("threshold")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setDensityThreshold(content.getArgument("num", Float.class));
										content.getSource().sendMessage(Text.of("Density threshold changed!"));
										return 1;
									}))
							)
							.then(literal("thresholdMultiplier")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThresholdMultiplier(content.getArgument("num", Float.class));
										content.getSource().sendMessage(Text.of("Threshold multiplier changed!"));
										return 1;
									}))
							)
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendMessage(Text.of("Pre-detect time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Pre-detect time is " + SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime()));
										return 1;
									})
							)
							.then(literal("changingspeed")
									.then(argument("changingspeed", IntegerArgumentType.integer(1, 5)).executes(content -> {
										switch (content.getArgument("changingspeed", Integer.class)) {
											case 1 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_SLOW);
											case 2 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.SLOW);
											case 3 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.NORMAL);
											case 4 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.FAST);
											case 5 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_FAST);
										}
										content.getSource().sendMessage(Text.of("Changing speed changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Density changing speed is " + SFCReMod.COMMON_CONFIG.getDensityChangingSpeed().toString()));
										return 1;
									})
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Common density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Common density is " + SFCReMod.COMMON_CONFIG.getCloudDensityPercent()));
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Rain density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Rain density is " + SFCReMod.COMMON_CONFIG.getRainDensityPercent()));
										return 1;
									})
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Thunder density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Thunder density is " + SFCReMod.COMMON_CONFIG.getThunderDensityPercent()));
										return 1;
									})
							)
					)
					.then(literal("biome").requires(source -> source.hasPermissionLevel(2))
							.then(literal("snow")
									.then(argument("snow", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSnowDensity(content.getArgument("snow", Integer.class));
										content.getSource().sendMessage(Text.of("Snow area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Snow area density is " + SFCReMod.COMMON_CONFIG.getSnowDensity()));
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("rain", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setRainDensity(content.getArgument("rain", Integer.class));
										content.getSource().sendMessage(Text.of("Rain area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Rain area density is " + SFCReMod.COMMON_CONFIG.getRainDensity()));
										return 1;
									})
							)
							.then(literal("none")
									.then(argument("none", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setNoneDensity(content.getArgument("none", Integer.class));
										content.getSource().sendMessage(Text.of("Nothing area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Nothing area density is " + SFCReMod.COMMON_CONFIG.getNoneDensity()));
										return 1;
									})
							)
							.then(literal("byChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityByChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendMessage(Text.of("Biome detect function changed!"));
										return 1;
									}))
							)
							.then(literal("byLoadedChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityUseLoadedChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendMessage(Text.of("Biome detect function changed!"));
										return 1;
									}))
							)
							.then(literal("list").executes(content -> {
								content.getSource().sendMessage(Text.of("Server Biome Filter List: "));
								for (String biome : SFCReMod.COMMON_CONFIG.getBiomeFilterList()) {
									content.getSource().sendMessage(Text.of("- " + biome));
								}
								return 1;
							}))
							.then(literal("add")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.add(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendMessage(Text.of("Biome added!"));
										return 1;
									}))
							)
							.then(literal("remove")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.remove(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendMessage(Text.of("Biome removed!"));
										return 1;
									}))
							)
					)
					.then(literal("reload").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG_HOLDER.load();
						SFCReMod.updateConfig();
						SFCReMod.LOGGER.info("[SFCRe] cb: Reload config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendMessage(Text.of("Reloading complete!"));
						return 1;
					}))
					.then(literal("save").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG_HOLDER.save();
						SFCReMod.updateConfig();
						SFCReMod.LOGGER.info("[SFCRe] cb: Save config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendMessage(Text.of("Config saving complete!"));
						return 1;
					}))
			);
		});
	}
}