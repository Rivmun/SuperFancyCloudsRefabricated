package com.rimo.sfcr;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rimo.sfcr.config.Config;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import static com.rimo.sfcr.Common.CONFIG;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Server implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		//register a few command to get control remotely
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher
				.register(literal("sfcr").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(literal("enable")
							.executes(context -> {
								context.getSource().sendSystemMessage(Component.nullToEmpty("SFCR enable: " + CONFIG.isEnableMod()));
								return 1;
							})
							.then(argument("e", BoolArgumentType.bool()).executes(context -> {
								CONFIG.setEnableMod(context.getArgument("e", Boolean.class));
								Config.save(CONFIG);
								context.getSource().sendSystemMessage(Component.nullToEmpty("Done!"));
								return 1;
							}))
					)
					.then(literal("debug")
							.executes(context -> {
								context.getSource().sendSystemMessage(Component.nullToEmpty("SFCR debug: " + CONFIG.isEnableDebug()));
								return 1;
							})
							.then(argument("e", BoolArgumentType.bool()).executes(context -> {
								CONFIG.setEnableDebug(context.getArgument("e", Boolean.class));
								Config.save(CONFIG);
								context.getSource().sendSystemMessage(Component.nullToEmpty("Done!"));
								return 1;
							}))
					)
					.then(literal("predetect")
							.executes(context -> {
								context.getSource().sendSystemMessage(Component.nullToEmpty("Pre-detect time: " + CONFIG.getWeatherPreDetectTime() + "s"));
								return 1;
							})
							.then(argument("i", IntegerArgumentType.integer()).executes(context -> {
								CONFIG.setWeatherPreDetectTime(context.getArgument("i", Integer.class));
								Config.save(CONFIG);
								context.getSource().sendSystemMessage(Component.nullToEmpty("Done!"));
								return 1;
							}))
					)
				)
		);
	}
}
