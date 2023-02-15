package com.rimo.sfcr.register;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rimo.sfcr.SFCReClient;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.SFCReRuntimeData;
import com.rimo.sfcr.config.CloudRefreshSpeed;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Command {
	@Environment(EnvType.CLIENT)
	public static void registerClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
			dispatcher.register(ClientCommandManager.literal("sfcr")
					.then(ClientCommandManager.literal("sync")
							.then(ClientCommandManager.literal("full")).executes(content -> {
								content.getSource().sendFeedback(Text.translatable("text.sfcr.command.sync_request"));
								SFCReClient.sendSyncRequest(true);
								return 1;
							})
							.executes(content -> {
								content.getSource().sendFeedback(Text.translatable("text.sfcr.command.sync_request"));
								SFCReClient.sendSyncRequest(false);
								return 1;
							})
					)
			);
		});
	}
	
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(literal("sfcr").requires(source -> source.hasPermissionLevel(2))
					.executes(content -> {
						content.getSource().sendMessage(Text.of("- - - - - SFCR Help Page - - - - -"));
						content.getSource().sendMessage(Text.of("/sfcr - Show this page"));
						content.getSource().sendMessage(Text.of("/sfcr statu - Show runtime config"));
						content.getSource().sendMessage(Text.of("/sfcr [enable|disable] - Toggle SFCR server activity"));
						content.getSource().sendMessage(Text.of("/sfcr [cloud|density] - Edit config"));
						content.getSource().sendMessage(Text.of("/sfcr biome [list|add|remove] - Manage ignored biome"));
						content.getSource().sendMessage(Text.of("/sfcr reload - Reload config, then force sync to every client"));
						content.getSource().sendMessage(Text.of("/sfcr save - Save runtime config to file"));
						return 1;
					})
					.then(literal("statu").executes(content -> {
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
					.then(literal("enable").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						SFCReMain.config.setEnableMod(true);
						content.getSource().sendMessage(Text.of("SFCR enabled!"));
						return 1;
					}))
					.then(literal("disable").requires(source -> source.hasPermissionLevel(2)).executes(content -> {
						SFCReMain.config.setEnableMod(false);
						content.getSource().sendMessage(Text.of("SFCR disabled!"));
						return 1;
					}))
					.then(literal("cloud")
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMain.config.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendMessage(Text.of("Cloud height changed!"));
										return 1;
									}))
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMain.config.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendMessage(Text.of("Sample step changed!"));
										return 1;
									}))
							)
					)
					.then(literal("density")
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMain.config.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendMessage(Text.of("Pre-detect time changed!"));
										return 1;
									}))
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
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Common density changed!"));
										return 1;
									}))
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Rain density changed!"));
										return 1;
									}))
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
										SFCReMain.config.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendMessage(Text.of("Thunder density changed!"));
										return 1;
									}))
							)
					)
					.then(literal("biome")
							.then(argument("percent", IntegerArgumentType.integer(0,100)).executes(content -> {
								SFCReMain.config.setBiomeDensityMultipler(content.getArgument("percent", Integer.class));
								content.getSource().sendMessage(Text.of("Biome affect changed!"));
								return 1;
							}))
							.then(literal("list").executes(content -> {
								content.getSource().sendMessage(Text.of("Server Biome Filter List: "));
								for (String biome : SFCReMain.config.getBiomeFilterList()) {
									content.getSource().sendMessage(Text.of(biome));
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
					.then(literal("reload").executes(content -> {
						SFCReMain.CONFIGHOLDER.load();
						SFCReMain.config = SFCReMain.CONFIGHOLDER.getConfig();
						for (ServerPlayerEntity player : content.getSource().getServer().getPlayerManager().getPlayerList()) {
							SFCReMain.sendConfig(player, content.getSource().getServer());
							SFCReRuntimeData.sendInitialData(player, content.getSource().getServer());
							player.sendMessage(Text.of("[SFCRe] Force sync request came from server..."));
						}
						content.getSource().sendMessage(Text.of("Reloading & force sync complete!"));
						return 1;
					}))
					.then(literal("save").executes(content -> {
						SFCReMain.CONFIGHOLDER.save();
						content.getSource().sendMessage(Text.of("Config saving complete!"));
						return 1;
					}))
			);
		});
	}
}
