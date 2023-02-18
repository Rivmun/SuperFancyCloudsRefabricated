package com.rimo.sfcr.register;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.SFCReRuntimeData;
import com.rimo.sfcr.config.CloudRefreshSpeed;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Command {
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(literal("sfcr")
					.executes(content -> {
						content.getSource().sendMessage(Text.of("- - - - - SFCR Help Page - - - - -"));
						content.getSource().sendMessage(Text.of("/sfcr - Show this page"));
						content.getSource().sendMessage(Text.of("/sfcr sync - Sync with server instantly"));
						if (!content.getSource().hasPermissionLevel(2))
							return 1;
						content.getSource().sendMessage(Text.of("/sfcr statu - Show runtime config"));
						content.getSource().sendMessage(Text.of("/sfcr [enable|disable] - Toggle SFCR server activity"));
						content.getSource().sendMessage(Text.of("/sfcr [cloud|density] - Edit config"));
						content.getSource().sendMessage(Text.of("/sfcr biome [list|add|remove] - Manage ignored biome"));
						content.getSource().sendMessage(Text.of("/sfcr reload - Reload config, then force sync to every client"));
						content.getSource().sendMessage(Text.of("/sfcr save - Save runtime config to file"));
						return 1;
					})
					.then(literal("sync")
							.executes(content -> {
								SFCReRuntimeData.sendRuntimeData(content.getSource().getPlayer(), content.getSource().getServer());
								SFCReMain.LOGGER.info("[SFCRe] cb: Send sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendMessage(Text.of("Manual requesting sync..."));
								return 1;
							})
							.then(literal("full").executes(content -> {
								SFCReMain.sendConfig(content.getSource().getPlayer(), content.getSource().getServer());
								SFCReRuntimeData.sendRuntimeData(content.getSource().getPlayer(), content.getSource().getServer());
								SFCReMain.LOGGER.info("[SFCRe] cb: Send full sync data to " + content.getSource().getDisplayName().getString());
								content.getSource().sendMessage(Text.of("Manual requesting sync..."));
								return 1;
							}))
							.then(literal("time").requires(source -> source.hasPermissionLevel(2))
									.then(argument("sec", IntegerArgumentType.integer()).executes(content -> {
										SFCReMain.config.setSecPerSync(content.getArgument("sec", Integer.class));
										content.getSource().sendMessage(Text.of("Sync time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Sync per second is " + SFCReMain.config.getSecPerSync()));
										return 1;
									})
							)
							.then(literal("toAllPlayers").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
								for (ServerPlayerEntity player : content.getSource().getServer().getPlayerManager().getPlayerList()) {
									SFCReMain.sendConfig(player, content.getSource().getServer());
									SFCReRuntimeData.sendRuntimeData(player, content.getSource().getServer());
									player.sendMessage(Text.of("[SFCRe] Force sync request came from server..."));
								}
								content.getSource().sendMessage(Text.of("Force sync complete!"));
								SFCReMain.LOGGER.info("[SFCRe] cb: Force sync running by " + content.getSource().getDisplayName().getString());
								return 1;
							}))
					)
					.then(literal("statu").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						content.getSource().sendMessage(Text.of("- - - - - SFCR Mod Statu - - - - -"));
                        content.getSource().sendMessage(Text.of("§eStatu:           §r" + SFCReMain.config.isEnableMod()));
                        content.getSource().sendMessage(Text.of("§eCloud height:    §r" + SFCReMain.config.getCloudHeight()));
                        content.getSource().sendMessage(Text.of("§eSample Step:     §r" + SFCReMain.config.getSampleSteps()));
                        content.getSource().sendMessage(Text.of("§ePre-Detect Time: §r" + SFCReMain.config.getWeatherPreDetectTime() / 20));
                        content.getSource().sendMessage(Text.of("§eChanging Speed:  §r" + SFCReMain.config.getNumFromSpeedEnum(SFCReMain.config.getDensityChangingSpeed())));
                        content.getSource().sendMessage(Text.of("§eCommon Density:  §r" + SFCReMain.config.getCloudDensityPercent()));
                        content.getSource().sendMessage(Text.of("§eRain Density:    §r" + SFCReMain.config.getRainDensityPercent()));
                        content.getSource().sendMessage(Text.of("§eThunder Density: §r" + SFCReMain.config.getThunderDensityPercent()));
                        content.getSource().sendMessage(Text.of("§eBiome Affect:    §r" + SFCReMain.config.getBiomeDensityMultipler()));
                        content.getSource().sendMessage(Text.of("Type [/sfcr biome list] to check ignored biome list."));
						return 1;
					}))
					.then(literal("enable").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMain.config.setEnableMod(content.getArgument("e", Boolean.class));
								content.getSource().sendMessage(Text.of("SFCR statu changed!"));
								return 1;
							}))
					)
					.then(literal("debug").requires(source -> source.hasPermissionLevel(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMain.config.setEnableDebug(content.getArgument("e", Boolean.class));
								content.getSource().sendMessage(Text.of("Debug statu changed!"));
								return 1;
							}))
					)
					.then(literal("cloud").requires(source -> source.hasPermissionLevel(2))
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMain.config.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendMessage(Text.of("Cloud height changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Cloud height is " + SFCReMain.config.getCloudHeight()));
										return 1;
									})
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMain.config.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendMessage(Text.of("Sample step changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Sample steps is " + SFCReMain.config.getSampleSteps()));
										return 1;
									})
							)
					)
					.then(literal("density").requires(source -> source.hasPermissionLevel(2))
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMain.config.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendMessage(Text.of("Pre-detect time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Pre-detect time is " + SFCReMain.config.getWeatherPreDetectTime()));
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
										content.getSource().sendMessage(Text.of("Changing speed changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Density changing speed is " + SFCReMain.config.getDensityChangingSpeed().toString()));
										return 1;
									})
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Common density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Common density is " + SFCReMain.config.getCloudDensityPercent()));
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Rain density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Rain density is " + SFCReMain.config.getRainDensityPercent()));
										return 1;
									})
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Thunder density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendMessage(Text.of("Thunder density is " + SFCReMain.config.getThunderDensityPercent()));
										return 1;
									})
							)
					)
					.then(literal("biome").requires(source -> source.hasPermissionLevel(2))
							.then(literal("multipler")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setBiomeDensityMultipler(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Biome affect changed!"));
										return 1;
									}))
							)
							.executes(content -> {
								content.getSource().sendMessage(Text.of("Biome affect percent is " + SFCReMain.config.getBiomeDensityMultipler()));
								return 1;
							})
							.then(literal("list").executes(content -> {
								content.getSource().sendMessage(Text.of("Server Biome Filter List: "));
								for (String biome : SFCReMain.config.getBiomeFilterList()) {
									content.getSource().sendMessage(Text.of("- " + biome));
								}
								return 1;
							}))
							.then(literal("add")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMain.config.getBiomeFilterList();
										list.add(content.getArgument("id", String.class));
										SFCReMain.config.setBiomeFilterList(list);
										content.getSource().sendMessage(Text.of("Biome added!"));
										return 1;
									}))
							)
							.then(literal("remove")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMain.config.getBiomeFilterList();
										list.remove(content.getArgument("id", String.class));
										SFCReMain.config.setBiomeFilterList(list);
										content.getSource().sendMessage(Text.of("Biome removed!"));
										return 1;
									}))
							)
					)
					.then(literal("reload").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMain.CONFIGHOLDER.load();
						SFCReMain.config = SFCReMain.CONFIGHOLDER.getConfig();
						SFCReMain.LOGGER.info("[SFCRe] cb: Reload config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendMessage(Text.of("Reloading complete!"));
						return 1;
					}))
					.then(literal("save").requires(source -> source.hasPermissionLevel(4)).executes(content -> {
						SFCReMain.CONFIGHOLDER.save();
						SFCReMain.LOGGER.info("[SFCRe] cb: Save config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendMessage(Text.of("Config saving complete!"));
						return 1;
					}))
			);
		});
	}
}
