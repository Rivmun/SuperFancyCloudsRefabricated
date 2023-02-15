package com.rimo.sfcr;

import com.rimo.sfcr.register.Command;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public class SFCReClient implements ClientModInitializer {

	@Environment(EnvType.CLIENT)
	public static final SFCReRenderer RENDERER = new SFCReRenderer();

	@Override
	public void onInitializeClient() {
		Command.registerClient();

		ClientPlayNetworking.registerGlobalReceiver(SFCReMain.PACKET_CONFIG, SFCReMain::receiveConfig);
		ClientPlayNetworking.registerGlobalReceiver(SFCReRuntimeData.PACKET_RUNTIME, SFCReRuntimeData::receiveInitialData);
		ClientPlayNetworking.registerGlobalReceiver(SFCReRuntimeData.PACKET_WEATHER, SFCReRuntimeData::receiveWeather);
		
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> RENDERER.init());
		ClientTickEvents.START_CLIENT_TICK.register((client) -> RENDERER.tick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RENDERER.clean());
	}
	
	@Environment(EnvType.CLIENT)
	public static void sendSyncRequest(boolean isFull) {
		PacketByteBuf packet = PacketByteBufs.create();
		packet.writeBoolean(isFull);
		ClientPlayNetworking.send(SFCReMain.PACKET_SYNC_REQUEST, packet);
	}
}
