package com.rimo.sfcr;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rimo.sfcr.config.Config;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;

import static com.rimo.sfcr.Common.CONFIG;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Server implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		//register a few command to get control remotely
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher
				.register(literal("sfcr").requires(source -> source.hasPermissionLevel(2))
					.then(literal("enable")
							.executes(context -> {
								context.getSource().sendMessage(Text.of("SFCR enable: " + CONFIG.isEnableMod()));
								return 1;
							})
							.then(argument("e", BoolArgumentType.bool()).executes(context -> {
								CONFIG.setEnableMod(context.getArgument("e", Boolean.class));
								Config.save(CONFIG);
								context.getSource().sendMessage(Text.of("Done!"));
								return 1;
							}))
					)
					.then(literal("debug")
							.executes(context -> {
								context.getSource().sendMessage(Text.of("SFCR debug: " + CONFIG.isEnableDebug()));
								return 1;
							})
							.then(argument("e", BoolArgumentType.bool()).executes(context -> {
								CONFIG.setEnableDebug(context.getArgument("e", Boolean.class));
								Config.save(CONFIG);
								context.getSource().sendMessage(Text.of("Done!"));
								return 1;
							}))
					)
					.then(literal("predetect")
							.executes(context -> {
								context.getSource().sendMessage(Text.of("Pre-detect time: " + CONFIG.getWeatherPreDetectTime() + "s"));
								return 1;
							})
							.then(argument("i", IntegerArgumentType.integer()).executes(context -> {
								CONFIG.setWeatherPreDetectTime(context.getArgument("i", Integer.class));
								Config.save(CONFIG);
								context.getSource().sendMessage(Text.of("Done!"));
								return 1;
							}))
					)
				)
		);
	}
}
