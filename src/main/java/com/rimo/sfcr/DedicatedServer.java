package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.arguments.BoolArgumentType;
//? if < 1.19
//import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rimo.sfcr.config.Config;
//~ if = 1.16.5 'dev.architectury' -> 'me.shedaniel.architectury' {
//~ if = 1.16.5 'events.common' -> 'events' {
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
//~ }
import dev.architectury.networking.NetworkManager;
//~ }
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
//? if < 1.21 {
/*import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///? }

import static com.rimo.sfcr.Common.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DedicatedServer {
	public static void init() {
		//? if >= 1.21 {
		NetworkManager.registerS2CPayloadType(WeatherPayload.TYPE, WeatherPayload.CODEC);
		NetworkManager.registerS2CPayloadType(UploadRequestPayload.TYPE, UploadRequestPayload.CODEC);
		//? }
		LifecycleEvent.SETUP.register(Common::checkMixinApplied);

		//~ if > 1.19 'access' -> 'access, env'
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> dispatcher
				.register(literal(MOD_ID)
						.then(literal("help")
							.requires(source -> source.hasPermission(2))
							.executes(context -> {
								VersionUtil.sendSystemMessage(context, "- - - - - SFCR Help Page - - - - -");
								VersionUtil.sendSystemMessage(context, "/sfcr help - Show this page.");
								VersionUtil.sendSystemMessage(context, "/sfcr status - Show current dimension's config.");
								VersionUtil.sendSystemMessage(context, "/sfcr service [true|false] - Set SFCR server activity.");
								VersionUtil.sendSystemMessage(context, "/sfcr logical [true|false] - Set whether NCNR function affect to logical behavior.");
								VersionUtil.sendSystemMessage(context, "/sfcr debug [true|false] - Set SFCR should output more log or not.");
								VersionUtil.sendSystemMessage(context, "/sfcr upload - upload your current config to server as current dimension specific config.");
								return 1;
							})
						)
						.then(literal("status")
								.requires(source -> source.hasPermission(2))
								.executes(context -> {
									String dimensionName = context.getSource().getLevel().dimension().location().toString();
									String configJson = getDimensionConfigJson(dimensionName);
									if (configJson == null) {
										VersionUtil.sendSystemMessage(context, "§4[SFCRe] Got an error that config cache of " + dimensionName +
												" is not found, please re-enter dimension or reload server.");
										LOGGER.error("{} unable to print config for {} at {}. It shouldn't be happened...", MOD_ID, context.getSource().getTextName(), dimensionName);
										return 1;
									}
									if (configJson.isEmpty()) {
										VersionUtil.sendSystemMessage(context, "[SFCRe] This dimension '" + dimensionName + "' has no config.");
										VersionUtil.sendSystemMessage(context, "[SFCRe] Use '/sfcr upload' to upload your current config to server.");
										return 1;
									}
									VersionUtil.sendSystemMessage(context, "[SFCRe] Dimension config of '" + dimensionName + "' are:");
									VersionUtil.sendSystemMessage(context, configJson);
									return 1;
								})
						)
						.then(literal("service")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setEnableServer(context.getArgument("e", Boolean.class));
											VersionUtil.sendSystemMessage(context, "[SFCRe] service status changed!");
											return 1;
										})
								)
						)
						.then(literal("logical")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setCloudRainLogically(context.getArgument("e", Boolean.class));
											VersionUtil.sendSystemMessage(context, "[SFCRe] NoCloudNoRain for logical side status changed!");
											return 1;
										})
								)
						)
						.then(literal("debug")
								.requires(source -> source.hasPermission(2))
								.then(argument("e", BoolArgumentType.bool())
										.executes(context -> {
											CONFIG.setEnableDebug(context.getArgument("e", Boolean.class));
											VersionUtil.sendSystemMessage(context, "[SFCRe] Debug status changed!");
											return 1;
										})
								)
						)
						.then(literal("upload")
								.requires(source -> source.hasPermission(4))
								.executes(context -> {
									//? if > 1.19 {
									ServerPlayer player = context.getSource().getPlayer();
									if (player == null) {
									//? } else {
									/*ServerPlayer player;
									try {
										player = context.getSource().getPlayerOrException();
									} catch (CommandSyntaxException e) {
									*///? }
										VersionUtil.sendSystemMessage(context, "§4[SFCRe] Please cast it from client!");
										return 1;
									}
									//? if < 1.21 {
									/*NetworkManager.sendToPlayer(player, PACKET_UPLOAD_REQUEST, new FriendlyByteBuf(Unpooled.buffer()));
									*///? } else
									NetworkManager.sendToPlayer(player, new UploadRequestPayload());
									return 1;
								})
						)
				)
		);

		// Shared Config Receiver
		// allows server can get a new dimension config uploaded by player
		//? if < 1.21 {
		/*NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_DIMENSION, (buf, context) -> {
			String name = buf.readUtf();
			String configJson = buf.readUtf();
			long l = buf.readVarLong();
		*///? } else {
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, DimensionPayload.TYPE, DimensionPayload.CODEC, (payload, context) -> {
			String name = payload.name();
			String configJson = payload.sharedConfigJson();
			long l = payload.seed();
		//? }
			Player player = context.getPlayer();
			if (! player.createCommandSourceStack().hasPermission(4)) {  //check permission again
				VersionUtil.sendMessage(player, "§4[SFCRe] Your permission is not enough to upload config!");
				LOGGER.warn("{} was refuse a configJson uploaded by {} because his/her permission check was fail. But why he/she can use 'upload' command?",
						MOD_ID, player.getName().getString());
				return;
			}
			Config config = new Config();
			try {
				config.fromString(configJson);
			} catch (JsonSyntaxException e) {
				VersionUtil.sendMessage(player, "§4[SFCRe] You upload a config that server cannot read, please check your mod version!");
				LOGGER.error("{} receive a broken config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
				return;
			}
			config.save(name);
			setDimensionConfigJson(name, configJson);
			VersionUtil.sendMessage(player, "[SFCRe] Config was successful upload!");
			LOGGER.info("{} receive a config of {}, uploaded by {}", MOD_ID, name, player.getName().getString());
		});
	}
}
