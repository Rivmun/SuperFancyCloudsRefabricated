//? if fabric {
package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//~ if = 1.21.11 'ClientLevelEvents' -> 'ClientWorldEvents'
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//~ if = 1.21.11 'ServerEntityLevelChangeEvents' -> 'ServerEntityWorldChangeEvents'
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
//~ if = 1.21.11 'ServerLevelEvents' -> 'ServerWorldEvents'
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
//~ if = 1.21.11 'ClientCommands' -> 'ClientCommandManager'
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class Platform implements ModInitializer {
	@Override
	public void onInitialize() {
		//~ if = 1.21.11 'clientboundPlay()' -> 'playS2C()' {
		PayloadTypeRegistry.clientboundPlay().register(Common.WeatherPayload.TYPE, Common.WeatherPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Common.UploadRequestPayload.TYPE, Common.UploadRequestPayload.CODEC);
		//~ }
		//~ if = 1.21.11 'serverboundPlay()' -> 'playC2S()'
		PayloadTypeRegistry.serverboundPlay().register(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC);

		//~ if = 1.21.11 'ServerLevelEvents' -> 'ServerWorldEvents' {
		ServerLevelEvents.LOAD.register((server, level) -> Common.addDimensionData(level));
		ServerLevelEvents.UNLOAD.register((server, level) -> Common.removeDimensionData(level));
		//~ }
		ServerTickEvents.END_SERVER_TICK.register(Common::onTick);
		//~ if = 1.21.11 'END_LEVEL_TICK' -> 'END_WORLD_TICK'
		ServerTickEvents.END_LEVEL_TICK.register(Common::onLevelTick);
		ServerPlayerEvents.JOIN.register(Common::onPlayerJoin);
		//~ if = 1.21.11 'ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL' -> 'ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD'
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, oldLevel, newLevel) -> {
			Common.onPlayerChangedDimension(player, newLevel.dimension());
		});
		ServerPlayerEvents.LEAVE.register(Common::onPlayerQuit);
	}

	public static class ClientInit implements ClientModInitializer {
		@Override
		@Environment(EnvType.CLIENT)
		public void onInitializeClient() {
			ClientLifecycleEvents.CLIENT_STARTED.register(client -> Client.init());
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
				if (! FabricLoader.getInstance().isModLoaded("cloth-config2"))
					return;
				//~ if = 1.21.11 'ClientCommands' -> 'ClientCommandManager'
				dispatcher.register(ClientCommands.literal(Common.MOD_ID + "config").executes(context1 -> {
					Minecraft client = Minecraft.getInstance();
					//~ if < 26.2 '.gui.setScreen' -> '.setScreen'
					client.execute(() -> client.gui.setScreen(new ConfigScreen().build()));
					return 1;
				}));
			});
			ClientPlayNetworking.registerGlobalReceiver(Common.WeatherPayload.TYPE, (payload, context) ->
					Client.handleWeatherPayload(payload)
			);
			ClientPlayNetworking.registerGlobalReceiver(Common.DimensionPayload.TYPE, (payload, context) ->
					Client.handleDimensionPayload(payload)
			);
			ClientPlayNetworking.registerGlobalReceiver(Common.UploadRequestPayload.TYPE, (payload, context) ->
					Client.handleUploadRequestPayload()
			);

			//~ if = 1.21.11 'ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE' -> 'ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE'
			ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> Client.onLevelLoad(level));  //this event can be invoked both on player join & changed dimension.
			ClientTickEvents.END_CLIENT_TICK.register(Client::onTick);
			ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> Client.onQuit(client.player));
		}
	}

	public static class ServerInit implements DedicatedServerModInitializer {
		@Override
		@Environment(EnvType.SERVER)
		public void onInitializeServer() {
			CommandRegistrationCallback.EVENT.register((dispatcher, context, env) ->
					DedicatedServer.registerCommand(dispatcher)
			);
			ServerPlayNetworking.registerGlobalReceiver(Common.DimensionPayload.TYPE, (payload, context) ->
					DedicatedServer.handleDimensionPayload(payload, context.player())
			);
		}
	}

	// - - - - - Platform specific function - - - - -
	public static boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return ServerPlayNetworking.canSend(player, type);
	}
	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
	@Environment(EnvType.CLIENT)
	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
	public static boolean isModLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}
	public static Path getConfigFolder() {
		return FabricLoader.getInstance().getConfigDir();
	}
	public static boolean isFabric() {
		return true;
	}
}
//? }
