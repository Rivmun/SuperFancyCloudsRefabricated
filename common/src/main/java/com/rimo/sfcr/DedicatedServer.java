package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rimo.sfcr.config.Config;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static com.rimo.sfcr.Common.*;

@Environment(EnvType.SERVER)
public class DedicatedServer {
	public static void init() {
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> dispatcher
				.register(literal(MOD_ID)
						.then(literal("help")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(context -> {
								context.getSource().sendMessage(Text.of("- - - - - SFCR Help Page - - - - -"));
								context.getSource().sendMessage(Text.of("/sfcr help - Show this page."));
								context.getSource().sendMessage(Text.of("/sfcr status - Show current dimension's config."));
								context.getSource().sendMessage(Text.of("/sfcr service [true|false] - Set SFCR server activity."));
								context.getSource().sendMessage(Text.of("/sfcr logical [true|false] - Set whether NCNR function affect to logical behavior."));
								context.getSource().sendMessage(Text.of("/sfcr debug [true|false] - Set SFCR should output more log or not."));
								context.getSource().sendMessage(Text.of("/sfcr upload - upload your current config to server as current dimension specific config."));
								return 1;
							})
						)
						.then(literal("status")
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> {
									String dimensionName = context.getSource().getWorld().getRegistryKey().getValue().toString();
									String configJson = Common.getDimensionConfigJson(dimensionName);
									if (configJson == null) {
										context.getSource().sendMessage(Text.of("§4[SFCRe] Got an error that config cache of " + dimensionName +
												" is not found, please re-enter dimension or reload server."));
										LOGGER.error("{} unable to print config for {} at {}. It shouldn't be happened...", MOD_ID, context.getSource().getName(), dimensionName);
										return 1;
									}
									if (configJson.isEmpty()) {
										context.getSource().sendMessage(Text.of("[SFCRe] This dimension '" + dimensionName + "' has no config."));
										context.getSource().sendMessage(Text.of("[SFCRe] Use '/sfcr upload' to upload your current config to server."));
										return 1;
									}
									context.getSource().sendMessage(Text.of("[SFCRe] Dimension config of '" + dimensionName + "' are:"));
									context.getSource().sendMessage(Text.of(configJson));
									return 1;
								})
						)
						.then(literal("service")
								.requires(source -> source.hasPermissionLevel(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											Common.CONFIG.setEnableServer(context.getArgument("e", Boolean.class));
											context.getSource().sendMessage(Text.of("[SFCRe] service status changed!"));
											return 1;
										})
								)
						)
						.then(literal("logical")
								.requires(source -> source.hasPermissionLevel(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											Common.CONFIG.setCloudRainLogically(context.getArgument("e", Boolean.class));
											context.getSource().sendMessage(Text.of("[SFCRe] NoCloudNoRain for logical side status changed!"));
											return 1;
										})
								)
						)
						.then(literal("debug")
								.requires(source -> source.hasPermissionLevel(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											Common.CONFIG.setEnableDebug(context.getArgument("e", Boolean.class));
											context.getSource().sendMessage(Text.of("[SFCRe] Debug status changed!"));
											return 1;
										})
								)
						)
						.then(literal("upload")
								.requires(source -> source.hasPermissionLevel(4))
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									if (player == null) {
										context.getSource().sendMessage(Text.of("§4[SFCRe] Please cast it from client!"));
										return 1;
									}
									NetworkManager.sendToPlayer(player, PACKET_UPLOAD_REQUEST, new PacketByteBuf(Unpooled.buffer()));
									return 1;
								})
						)
				)
		);

		// Shared Config Receiver
		// allows server can get a new dimension config uploaded by player
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_DIMENSION, ((buf, context) -> {
			String name = buf.readString();
			String configJson = buf.readString();
			long l = buf.readVarLong();
			PlayerEntity player = context.getPlayer();
			if (! player.getCommandSource().hasPermissionLevel(4)) {  //check permission again
				player.sendMessage(Text.of("§4[SFCRe] Your permission is not enough to upload config!"));
				LOGGER.warn("{} was refuse a configJson uploaded by {} because his/her permission check was fail. But why he/she can use 'upload' command?",
						MOD_ID, player.getName().getString());
				return;
			}
			Config config = new Config();
			try {
				config.fromString(configJson);
			} catch (JsonSyntaxException e) {
				player.sendMessage(Text.of("§4[SFCRe] You upload a config that server cannot read, please check your mod version!"));
				LOGGER.error("{} receive a broken config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
				return;
			}
			config.save(name);
			setDimensionConfigJson(name, configJson);
			player.sendMessage(Text.of("[SFCRe] Config was successful upload!"));
			LOGGER.info("{} receive a config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
		}));
	}
}
