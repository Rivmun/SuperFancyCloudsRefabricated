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
public class Server {
	public static void init() {
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> dispatcher
				.register(literal("sfcr")
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> {
							context.getSource().sendMessage(Text.of("- - - - - SFCR Help Page - - - - -"));
							context.getSource().sendMessage(Text.of("/sfcr - Show this page"));
							context.getSource().sendMessage(Text.of("/sfcr status - Show current dimension's config"));
							context.getSource().sendMessage(Text.of("/sfcr [enable|disable] - Toggle SFCR server activity"));
							context.getSource().sendMessage(Text.of("/sfcr upload - upload your current config to server as current dimension specific config"));
							return 1;
						})
						.then(literal("status")
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> {
									String dimensionName = context.getSource().getWorld().getRegistryKey().getValue().toString();
									String configJson = Common.getDimensionConfigJson(dimensionName);
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
						.then(literal("enable")
								.requires(source -> source.hasPermissionLevel(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											Common.CONFIG.setEnableRender(context.getArgument("e", Boolean.class));
											context.getSource().sendMessage(Text.of("[SFCRe] server status changed!"));
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
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_SHARED_CONFIG, ((buf, context) -> {
			String configJson = buf.readString();
			PlayerEntity player = context.getPlayer();
			if (! player.getCommandSource().hasPermissionLevel(4)) {  //check permission again
				player.sendMessage(Text.of("§4[SFCRe] Your permission is not enough to upload config!"));
				LOGGER.warn("{} was refuse a configJson uploaded by {} because his/her permission check was fail. But why he/she can use 'upload' command?", MOD_ID, player.getName().getString());
				return;
			}
			String dimensionName = player.getWorld().getRegistryKey().getValue().toString();
			Config config = new Config();
			try {
				config.fromString(configJson);
			} catch (JsonSyntaxException e) {
				player.sendMessage(Text.of("§4[SFCRe] You upload a config that server cannot read, please check your mod version!"));
				LOGGER.error("{} receive a broken config of {}, uploaded by {}", MOD_ID, dimensionName, player.getName().getString());
				return;
			}
			config.save(dimensionName);
			setDimensionConfigJson(dimensionName, configJson);
			player.sendMessage(Text.of("[SFCRe] Config was success to upload!"));
			LOGGER.info("{} receive a config of {}, uploaded by {}", MOD_ID, dimensionName, player.getName().getString());
		}));
	}
}
