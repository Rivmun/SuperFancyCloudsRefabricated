//? if fabric {
package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class EntryPoint implements ModInitializer, ClientModInitializer, DedicatedServerModInitializer {
	@Override
	public void onInitialize() {
		ServerLevelEvents.LOAD.register((server, level) -> Common.addDimensionData(level));
		ServerLevelEvents.UNLOAD.register((server, level) -> Common.removeDimensionData(level));
		ServerTickEvents.END_SERVER_TICK.register(Common::onTick);
		ServerTickEvents.END_LEVEL_TICK.register(Common::onLevelTick);
		ServerPlayerEvents.JOIN.register(player -> Common.sendDimensionPacket(player, player.level().dimension()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (oldPlayer.level().dimension() != newPlayer.level().dimension())
				Common.sendDimensionPacket(newPlayer, newPlayer.level().dimension());
		});
	}

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> Client.init());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
			if (! FabricLoader.getInstance().isModLoaded("cloth-config2"))
				return;
			dispatcher.register(ClientCommands.literal(Common.MOD_ID).executes(context1 -> {
				Minecraft client = context1.getSource().getClient();
				client.execute(() -> client.setScreen(new ConfigScreen().build()));
				return 1;
			}));
		});
		PayloadTypeRegistry.clientboundPlay().register(Common.WeatherPayload.TYPE, Common.WeatherPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Common.UploadRequestPayload.TYPE, Common.UploadRequestPayload.CODEC);
		ClientPlayNetworking.registerGlobalReceiver(Common.WeatherPayload.TYPE, (payload, context) ->
				Client.handleWeatherPayload(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(Common.DimensionPayload.TYPE, (payload, context) ->
				Client.handleDimensionPayload(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(Common.UploadRequestPayload.TYPE, (payload, context) ->
				Client.handleUploadRequestPayload()
		);

		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> Client.onLevelLoad(level));
		ClientTickEvents.END_CLIENT_TICK.register(Client::onTick);
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> Client.onQuit());
	}

	@Override
	public void onInitializeServer() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, env) ->
				DedicatedServer.registerCommand(dispatcher)
		);
		PayloadTypeRegistry.serverboundPlay().register(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Common.DimensionPayload.TYPE, (payload, context) ->
				DedicatedServer.handleDimensionPayload(payload, context.player())
		);
	}
}
//? }
