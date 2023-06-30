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
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Command {
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationEvent.EVENT.register((dispatcher, selection) -> {
			dispatcher.register(literal("sfcr")
					.executes(content -> {
						content.getSource().sendFeedback(Text.of("- - - - - SFCR Help Page - - - - -"), false);
						content.getSource().sendFeedback(Text.of("/sfcr - Show this page"), false);
						content.getSource().sendFeedback(Text.of("/sfcr sync - Sync with server instantly"), false);
						if (!content.getSource().hasPermissionLevel(2))
							return 1;
						content.getSource().sendFeedback(Text.of("/sfcr statu - Show runtime config"), false);
						content.getSource().sendFeedback(Text.of("/sfcr [enable|disable] - Toggle SFCR server activity"), false);
						content.getSource().sendFeedback(Text.of("/sfcr [cloud|density|biome] - Edit config"), false);
						content.getSource().sendFeedback(Text.of("/sfcr biome [list|add|remove] - Manage ignored biome"), false);
						content.getSource().sendFeedback(Text.of("/sfcr reload - Reload config, then force sync to every client"), false);
						content.getSource().sendFeedback(Text.of("/sfcr save - Save runtime config to file"), false);
						return 1;
					})
					.then(literal("sync")
							.executes(content -> {
								Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
								SFCReMod.LOGGER.info("[SFCRe] cb: Send sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendFeedback(Text.of("Manual requesting sync..."), false);
								return 1;
							})
							.then(literal("full").executes(content -> {
								Network.CONFIG_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new ConfigSyncMessage(SFCReMod.COMMON_CONFIG));
								Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
								SFCReMod.LOGGER.info("[SFCRe] cb: Send full sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendFeedback(Text.of("Manual requesting sync..."), false);
								return 1;
							}))
							.then(literal("time").requires(source -> source.hasPermissionLevel(2))
									.then(argument("sec", IntegerArgumentType.integer()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSecPerSync(content.getArgument("sec", Integer.class));
										content.getSource().sendFeedback(Text.of("Sync time changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Sync per second is " + SFCReMod.COMMON_CONFIG.getSecPerSync()), false);
										return 1;
									})
							)
							.then(literal("toAllPlayers").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
								for (ServerPlayerEntity player : content.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
									Network.CONFIG_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new ConfigSyncMessage(SFCReMod.COMMON_CONFIG));
									Network.RUNTIME_CHANNEL.sendToPlayer(content.getSource().getPlayer(), new RuntimeSyncMessage(SFCReMod.RUNTIME));
									player.sendMessage(Text.of("[SFCRe] Force sync request came from server..."), false);
								}
								content.getSource().sendFeedback(Text.of("Force sync complete!"), false);
								SFCReMod.LOGGER.info("[SFCRe] cb: Force sync running by " + content.getSource().getDisplayName().getString());
								return 1;
							}))
					)
					.then(literal("statu").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						content.getSource().sendFeedback(Text.of("- - - - - SFCR Mod Statu - - - - -"), false);
						content.getSource().sendFeedback(Text.of("§eStatu:           §r" + SFCReMod.COMMON_CONFIG.isEnableMod()), false);
						content.getSource().sendFeedback(Text.of("§eCloud height:    §r" + SFCReMod.COMMON_CONFIG.getCloudHeight()), false);
						content.getSource().sendFeedback(Text.of("§eCloud Block Size:§r" + SFCReMod.COMMON_CONFIG.getCloudBlockSize()), false);
						content.getSource().sendFeedback(Text.of("§eCloud Thickness: §r" + SFCReMod.COMMON_CONFIG.getCloudLayerThickness()), false);
						content.getSource().sendFeedback(Text.of("§eSample Step:     §r" + SFCReMod.COMMON_CONFIG.getSampleSteps()), false);
						content.getSource().sendFeedback(Text.of("§eDynamic Density: §r" + SFCReMod.COMMON_CONFIG.isEnableWeatherDensity()), false);
						content.getSource().sendFeedback(Text.of("§eDensity Threshld:§r" + SFCReMod.COMMON_CONFIG.getDensityThreshold()), false);
						content.getSource().sendFeedback(Text.of("§eThrshld Multiplr:§r" + SFCReMod.COMMON_CONFIG.getThresholdMultiplier()), false);
						content.getSource().sendFeedback(Text.of("§ePre-Detect Time: §r" + SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime() / 20), false);
						content.getSource().sendFeedback(Text.of("§eChanging Speed:  §r" + SFCReMod.COMMON_CONFIG.getNumFromSpeedEnum(SFCReMod.COMMON_CONFIG.getDensityChangingSpeed())), false);
						content.getSource().sendFeedback(Text.of("§eCommon Density:  §r" + SFCReMod.COMMON_CONFIG.getCloudDensityPercent()), false);
						content.getSource().sendFeedback(Text.of("§eRain Density:    §r" + SFCReMod.COMMON_CONFIG.getRainDensityPercent()), false);
						content.getSource().sendFeedback(Text.of("§eThunder Density: §r" + SFCReMod.COMMON_CONFIG.getThunderDensityPercent()), false);
						content.getSource().sendFeedback(Text.of("§eBiome Affect:    §r" + SFCReMod.COMMON_CONFIG.getBiomeDensityMultiplier()), false);
						content.getSource().sendFeedback(Text.of("§eUsing Chunk:     §r" + SFCReMod.COMMON_CONFIG.isBiomeDensityByChunk()), false);
						content.getSource().sendFeedback(Text.of("§eUsing Loaded Chk:§r" + SFCReMod.COMMON_CONFIG.isBiomeDensityUseLoadedChunk()), false);
						content.getSource().sendFeedback(Text.of("Type [/sfcr biome list] to check ignored biome list."), false);
						return 1;
					}))
					.then(literal("enable").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableMod(content.getArgument("e", Boolean.class));
								content.getSource().sendFeedback(Text.of("SFCR statu changed!"), false);
								return 1;
							}))
					)
					.then(literal("debug").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableDebug(content.getArgument("e", Boolean.class));
								content.getSource().sendFeedback(Text.of("Debug statu changed!"), false);
								return 1;
							}))
					)
					.then(literal("cloud").requires(source -> source.hasPermissionLevel(2))
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendFeedback(Text.of("Cloud height changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud height is " + SFCReMod.COMMON_CONFIG.getCloudHeight()), false);
										return 1;
									})
							)
							.then(literal("size")
									.then(argument("size", IntegerArgumentType.integer(1, 4)).executes(content -> {
										switch (content.getArgument("size", Integer.class)) {
										case 1: SFCReMod.COMMON_CONFIG.setCloudBlockSize(2); break;
										case 2: SFCReMod.COMMON_CONFIG.setCloudBlockSize(4); break;
										case 3: SFCReMod.COMMON_CONFIG.setCloudBlockSize(8); break;
										case 4: SFCReMod.COMMON_CONFIG.setCloudBlockSize(16); break;
										}
										content.getSource().sendFeedback(Text.of("Cloud size changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud size is " + SFCReMod.COMMON_CONFIG.getCloudBlockSize()), false);
										return 1;
									})
							)
							.then(literal("thickness")
									.then(argument("thickness", IntegerArgumentType.integer(8, 64)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudLayerThickness(content.getArgument("thickness", Integer.class));
										content.getSource().sendFeedback(Text.of("Cloud thickness changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud thickness is " + SFCReMod.COMMON_CONFIG.getCloudLayerThickness()), false);
										return 1;
									})
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendFeedback(Text.of("Sample step changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Sample steps is " + SFCReMod.COMMON_CONFIG.getSampleSteps()), false);
										return 1;
									})
							)
					)
					.then(literal("density").requires(source -> source.hasPermissionLevel(2))
							.then(literal("enable")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setEnableWeatherDensity(content.getArgument("e", Boolean.class));
										content.getSource().sendFeedback(Text.of("Density statu changed!"), false);
										return 1;
									}))
							)
							.then(literal("threshold")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setDensityThreshold(content.getArgument("num", Float.class));
										content.getSource().sendFeedback(Text.of("Density threshold changed!"), false);
										return 1;
									}))
							)
							.then(literal("thresholdMultiplier")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThresholdMultiplier(content.getArgument("num", Float.class));
										content.getSource().sendFeedback(Text.of("Threshold multiplier changed!"), false);
										return 1;
									}))
							)
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendFeedback(Text.of("Pre-detect time changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Pre-detect time is " + SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime()), false);
										return 1;
									})
							)
							.then(literal("changingspeed")
									.then(argument("changingspeed", IntegerArgumentType.integer(1, 5)).executes(content -> {
										switch (content.getArgument("changingspeed", Integer.class)) {
										case 1: SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_SLOW); break;
										case 2: SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.SLOW); break;
										case 3: SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.NORMAL); break;
										case 4: SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.FAST); break;
										case 5: SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_FAST); break;
										}
										content.getSource().sendFeedback(Text.of("Changing speed changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Density changing speed is " + SFCReMod.COMMON_CONFIG.getDensityChangingSpeed().toString()), false);
										return 1;
									})
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Common density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Common density is " + SFCReMod.COMMON_CONFIG.getCloudDensityPercent()), false);
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Rain density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Rain density is " + SFCReMod.COMMON_CONFIG.getRainDensityPercent()), false);
										return 1;
									})
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Thunder density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Thunder density is " + SFCReMod.COMMON_CONFIG.getThunderDensityPercent()), false);
										return 1;
									})
							)
					)
					.then(literal("biome").requires(source -> source.hasPermissionLevel(2))
							.then(literal("multipler")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityMultiplier(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Biome affect changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Biome affect percent is " + SFCReMod.COMMON_CONFIG.getBiomeDensityMultiplier()), false);
										return 1;
									})
							)
							.then(literal("byChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityByChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendFeedback(Text.of("Biome detect function changed!"), false);
										return 1;
									}))
							)
							.then(literal("byLoadedChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityUseLoadedChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendFeedback(Text.of("Biome detect function changed!"), false);
										return 1;
									}))
							)
							.then(literal("list").executes(content -> {
								content.getSource().sendFeedback(Text.of("Server Biome Filter List: "), false);
								for (String biome : SFCReMod.COMMON_CONFIG.getBiomeFilterList()) {
									content.getSource().sendFeedback(Text.of("- " + biome), false);
								}
								return 1;
							}))
							.then(literal("add")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										List<String> list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.add(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendFeedback(Text.of("Biome added!"), false);
										return 1;
									}))
							)
							.then(literal("remove")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										List<String> list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.remove(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendFeedback(Text.of("Biome removed!"), false);
										return 1;
									}))
							)
					)
					.then(literal("reload").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG_HOLDER.load();
						SFCReMod.LOGGER.info("[SFCRe] cb: Reload config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendFeedback(Text.of("Reloading complete!"), false);
						return 1;
					}))
					.then(literal("save").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG_HOLDER.save();
						SFCReMod.LOGGER.info("[SFCRe] cb: Save config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendFeedback(Text.of("Config saving complete!"), false);
						return 1;
					}))
			);
		});
	}
}
