package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rimo.sfcr.config.Config;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import static com.rimo.sfcr.Common.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Environment(EnvType.SERVER)
public class DedicatedServer {
	public static void init() {
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> dispatcher
				.register(literal(MOD_ID)
						.then(literal("help")
							.requires(source -> source.hasPermission(2))
							.executes(context -> {
								context.getSource().sendSystemMessage(Component.nullToEmpty("- - - - - SFCR Help Page - - - - -"));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr help - Show this page."));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr status - Show current dimension's config."));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr service [true|false] - Set SFCR server activity."));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr logical [true|false] - Set whether NCNR function affect to logical behavior."));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr debug [true|false] - Set SFCR should output more log or not."));
								context.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr upload - upload your current config to server as current dimension specific config."));
								return 1;
							})
						)
						.then(literal("status")
								.requires(source -> source.hasPermission(2))
								.executes(context -> {
									String dimensionName = context.getSource().getLevel().dimension().location().toString();
									String configJson = getDimensionConfigJson(dimensionName);
									if (configJson == null) {
										context.getSource().sendSystemMessage(Component.nullToEmpty("§4[SFCRe] Got an error that config cache of " + dimensionName +
												" is not found, please re-enter dimension or reload server."));
										LOGGER.error("{} unable to print config for {} at {}. It shouldn't be happened...", MOD_ID, context.getSource().getTextName(), dimensionName);
										return 1;
									}
									if (configJson.isEmpty()) {
										context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] This dimension '" + dimensionName + "' has no config."));
										context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] Use '/sfcr upload' to upload your current config to server."));
										return 1;
									}
									context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] Dimension config of '" + dimensionName + "' are:"));
									context.getSource().sendSystemMessage(Component.nullToEmpty(configJson));
									return 1;
								})
						)
						.then(literal("service")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setEnableServer(context.getArgument("e", Boolean.class));
											context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] service status changed!"));
											return 1;
										})
								)
						)
						.then(literal("logical")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setCloudRainLogically(context.getArgument("e", Boolean.class));
											context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] NoCloudNoRain for logical side status changed!"));
											return 1;
										})
								)
						)
						.then(literal("debug")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setEnableDebug(context.getArgument("e", Boolean.class));
											context.getSource().sendSystemMessage(Component.nullToEmpty("[SFCRe] Debug status changed!"));
											return 1;
										})
								)
						)
						.then(literal("upload")
								.requires(source -> source.hasPermission(4))
								.executes(context -> {
									ServerPlayer player = context.getSource().getPlayer();
									if (player == null) {
										context.getSource().sendSystemMessage(Component.nullToEmpty("§4[SFCRe] Please cast it from client!"));
										return 1;
									}
									NetworkManager.sendToPlayer(player, PACKET_UPLOAD_REQUEST, new FriendlyByteBuf(Unpooled.buffer()));
									return 1;
								})
						)
				)
		);

		// Shared Config Receiver
		// allows server can get a new dimension config uploaded by player
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_DIMENSION, ((buf, context) -> {
			String name = buf.readUtf();
			String configJson = buf.readUtf();
			long l = buf.readVarLong();
			Player player = context.getPlayer();
			if (! player.createCommandSourceStack().hasPermission(4)) {  //check permission again
				player.sendSystemMessage(Component.nullToEmpty("§4[SFCRe] Your permission is not enough to upload config!"));
				LOGGER.warn("{} was refuse a configJson uploaded by {} because his/her permission check was fail. But why he/she can use 'upload' command?",
						MOD_ID, player.getName().getString());
				return;
			}
			Config config = new Config();
			try {
				config.fromString(configJson);
			} catch (JsonSyntaxException e) {
				player.sendSystemMessage(Component.nullToEmpty("§4[SFCRe] You upload a config that server cannot read, please check your mod version!"));
				LOGGER.error("{} receive a broken config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
				return;
			}
			config.save(name);
			setDimensionConfigJson(name, configJson);
			player.sendSystemMessage(Component.nullToEmpty("[SFCRe] Config was successful upload!"));
			LOGGER.info("{} receive a config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
		}));
	}
}
