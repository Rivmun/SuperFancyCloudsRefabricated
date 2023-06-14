package com.rimo.sfcr.register;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.core.RuntimeData;
import com.rimo.sfcr.util.CloudRefreshSpeed;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Command {
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
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
								RuntimeData.sendRuntimeData(content.getSource().getPlayer(), content.getSource().getMinecraftServer());
								SFCReMain.LOGGER.info("[SFCRe] cb: Send sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendFeedback(Text.of("Manual requesting sync..."), false);
								return 1;
							})
							.then(literal("full").executes(content -> {
								SFCReMain.sendConfig(content.getSource().getPlayer(), content.getSource().getMinecraftServer());
								RuntimeData.sendRuntimeData(content.getSource().getPlayer(), content.getSource().getMinecraftServer());
								SFCReMain.LOGGER.info("[SFCRe] cb: Send full sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendFeedback(Text.of("Manual requesting sync..."), false);
								return 1;
							}))
							.then(literal("time").requires(source -> source.hasPermissionLevel(2))
									.then(argument("sec", IntegerArgumentType.integer()).executes(content -> {
										SFCReMain.config.setSecPerSync(content.getArgument("sec", Integer.class));
										content.getSource().sendFeedback(Text.of("Sync time changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Sync per second is " + SFCReMain.config.getSecPerSync()), false);
										return 1;
									})
							)
							.then(literal("toAllPlayers").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
								for (ServerPlayerEntity player : content.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
									SFCReMain.sendConfig(player, content.getSource().getMinecraftServer());
									RuntimeData.sendRuntimeData(player, content.getSource().getMinecraftServer());
									player.sendMessage(Text.of("[SFCRe] Force sync request came from server..."), false);
								}
								content.getSource().sendFeedback(Text.of("Force sync complete!"), false);
								SFCReMain.LOGGER.info("[SFCRe] cb: Force sync running by " + content.getSource().getDisplayName().getString());
								return 1;
							}))
					)
					.then(literal("statu").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						content.getSource().sendFeedback(Text.of("- - - - - SFCR Mod Statu - - - - -"), false);
                        content.getSource().sendFeedback(Text.of("§eStatu:           §r" + SFCReMain.config.isEnableMod()), false);
                        content.getSource().sendFeedback(Text.of("§eCloud height:    §r" + SFCReMain.config.getCloudHeight()), false);
                        content.getSource().sendFeedback(Text.of("§eSample Step:     §r" + SFCReMain.config.getSampleSteps()), false);
                        content.getSource().sendFeedback(Text.of("§ePre-Detect Time: §r" + SFCReMain.config.getWeatherPreDetectTime() / 20), false);
                        content.getSource().sendFeedback(Text.of("§eChanging Speed:  §r" + SFCReMain.config.getNumFromSpeedEnum(SFCReMain.config.getDensityChangingSpeed())), false);
                        content.getSource().sendFeedback(Text.of("§eCommon Density:  §r" + SFCReMain.config.getCloudDensityPercent()), false);
                        content.getSource().sendFeedback(Text.of("§eRain Density:    §r" + SFCReMain.config.getRainDensityPercent()), false);
                        content.getSource().sendFeedback(Text.of("§eThunder Density: §r" + SFCReMain.config.getThunderDensityPercent()), false);
                        content.getSource().sendFeedback(Text.of("§eBiome Affect:    §r" + SFCReMain.config.getBiomeDensityMultiplier()), false);
                        content.getSource().sendFeedback(Text.of("§eCloud Block Size:§r" + SFCReMain.config.getCloudBlockSize()), false);
                        content.getSource().sendFeedback(Text.of("§eUsing Chunk:     §r" + SFCReMain.config.isBiomeDensityByChunk()), false);
                        content.getSource().sendFeedback(Text.of("§eUsing Loaded Chk:§r" + SFCReMain.config.isBiomeDensityUseLoadedChunk()), false);
                        content.getSource().sendFeedback(Text.of("Type [/sfcr biome list] to check ignored biome list."), false);
						return 1;
					}))
					.then(literal("enable").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMain.config.setEnableMod(content.getArgument("e", Boolean.class));
								content.getSource().sendFeedback(Text.of("SFCR statu changed!"), false);
								return 1;
							}))
					)
					.then(literal("debug").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMain.config.setEnableDebug(content.getArgument("e", Boolean.class));
								content.getSource().sendFeedback(Text.of("Debug statu changed!"), false);
								return 1;
							}))
					)
					.then(literal("cloud").requires(source -> source.hasPermissionLevel(2))
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMain.config.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendFeedback(Text.of("Cloud height changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud height is " + SFCReMain.config.getCloudHeight()), false);
										return 1;
									})
							)
							.then(literal("size")
									.then(argument("size", IntegerArgumentType.integer(1, 4)).executes(content -> {
										switch (content.getArgument("size", Integer.class)) {
										case 1: SFCReMain.config.setCloudBlockSize(2); break;
										case 2: SFCReMain.config.setCloudBlockSize(4); break;
										case 3: SFCReMain.config.setCloudBlockSize(8); break;
										case 4: SFCReMain.config.setCloudBlockSize(16); break;
										}
										content.getSource().sendFeedback(Text.of("Cloud size changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud size is " + SFCReMain.config.getCloudBlockSize()), false);
										return 1;
									})
							)
							.then(literal("thickness")
									.then(argument("thickness", IntegerArgumentType.integer(8, 64)).executes(content -> {
										SFCReMain.config.setCloudLayerThickness(content.getArgument("thickness", Integer.class));
										content.getSource().sendFeedback(Text.of("Cloud thickness changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Cloud thickness is " + SFCReMain.config.getCloudLayerThickness()), false);
										return 1;
									})
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMain.config.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendFeedback(Text.of("Sample step changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Sample steps is " + SFCReMain.config.getSampleSteps()), false);
										return 1;
									})
							)
					)
					.then(literal("density").requires(source -> source.hasPermissionLevel(2))
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMain.config.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendFeedback(Text.of("Pre-detect time changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Pre-detect time is " + SFCReMain.config.getWeatherPreDetectTime()), false);
										return 1;
									})
							)
							.then(literal("changingspeed")
									.then(argument("changingspeed", IntegerArgumentType.integer(1, 5)).executes(content -> {
										switch (content.getArgument("changingspeed", Integer.class)) {
										case 1: SFCReMain.config.setDensityChangingSpeed(CloudRefreshSpeed.VERY_SLOW); break;
										case 2: SFCReMain.config.setDensityChangingSpeed(CloudRefreshSpeed.SLOW); break;
										case 3: SFCReMain.config.setDensityChangingSpeed(CloudRefreshSpeed.NORMAL); break;
										case 4: SFCReMain.config.setDensityChangingSpeed(CloudRefreshSpeed.FAST); break;
										case 5: SFCReMain.config.setDensityChangingSpeed(CloudRefreshSpeed.VERY_FAST); break;
										}
										content.getSource().sendFeedback(Text.of("Changing speed changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Density changing speed is " + SFCReMain.config.getDensityChangingSpeed().toString()), false);
										return 1;
									})
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Common density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Common density is " + SFCReMain.config.getCloudDensityPercent()), false);
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Rain density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Rain density is " + SFCReMain.config.getRainDensityPercent()), false);
										return 1;
									})
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Thunder density changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Thunder density is " + SFCReMain.config.getThunderDensityPercent()), false);
										return 1;
									})
							)
					)
					.then(literal("biome").requires(source -> source.hasPermissionLevel(2))
							.then(literal("multipler")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setBiomeDensityMultiplier(content.getArgument("percent", Integer.class));
										content.getSource().sendFeedback(Text.of("Biome affect changed!"), false);
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendFeedback(Text.of("Biome affect percent is " + SFCReMain.config.getBiomeDensityMultiplier()), false);
										return 1;
									})
							)
							.then(literal("byChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMain.config.setBiomeDensityByChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendFeedback(Text.of("Biome detect function changed!"), false);
										return 1;
									}))
							)
							.then(literal("byLoadedChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMain.config.setBiomeDensityUseLoadedChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendFeedback(Text.of("Biome detect function changed!"), false);
										return 1;
									}))
							)
							.then(literal("list").executes(content -> {
								content.getSource().sendFeedback(Text.of("Server Biome Filter List: "), false);
								for (String biome : SFCReMain.config.getBiomeFilterList()) {
									content.getSource().sendFeedback(Text.of("- " + biome), false);
								}
								return 1;
							}))
							.then(literal("add")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										List<String> list = SFCReMain.config.getBiomeFilterList();
										list.add(content.getArgument("id", String.class));
										SFCReMain.config.setBiomeFilterList(list);
										content.getSource().sendFeedback(Text.of("Biome added!"), false);
										return 1;
									}))
							)
							.then(literal("remove")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										List<String> list = SFCReMain.config.getBiomeFilterList();
										list.remove(content.getArgument("id", String.class));
										SFCReMain.config.setBiomeFilterList(list);
										content.getSource().sendFeedback(Text.of("Biome removed!"), false);
										return 1;
									}))
							)
					)
					.then(literal("reload").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMain.CONFIGHOLDER.load();
						SFCReMain.config = SFCReMain.CONFIGHOLDER.getConfig();
						SFCReMain.LOGGER.info("[SFCRe] cb: Reload config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendFeedback(Text.of("Reloading complete!"), false);
						return 1;
					}))
					.then(literal("save").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMain.CONFIGHOLDER.save();
						SFCReMain.LOGGER.info("[SFCRe] cb: Save config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendFeedback(Text.of("Config saving complete!"), false);
						return 1;
					}))
			);
		});
	}
}