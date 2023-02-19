package com.rimo.sfcr;

import com.rimo.sfcr.core.SFCReRenderer;
import com.rimo.sfcr.core.SFCReRuntimeData;

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

		ClientPlayNetworking.registerGlobalReceiver(SFCReMain.PACKET_CONFIG, SFCReMain::receiveConfig);
		ClientPlayNetworking.registerGlobalReceiver(SFCReRuntimeData.PACKET_RUNTIME, SFCReRuntimeData::receiveRuntimeData);
		ClientPlayNetworking.registerGlobalReceiver(SFCReRuntimeData.PACKET_WEATHER, SFCReRuntimeData::receiveWeather);
		
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			RENDERER.init();
			sendSyncRequest(true);
		});
		ClientTickEvents.START_CLIENT_TICK.register((client) -> RENDERER.tick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			RENDERER.clean();
			SFCReMain.CONFIGHOLDER.load();
			SFCReMain.config = SFCReMain.CONFIGHOLDER.getConfig();
		});
	}
	
	@Environment(EnvType.CLIENT)
	public static void sendSyncRequest(boolean isFull) {
		if (!SFCReMain.config.isEnableServerConfig())
			return;
		PacketByteBuf packet = PacketByteBufs.create();
		packet.writeBoolean(isFull);
		ClientPlayNetworking.send(SFCReMain.PACKET_SYNC_REQUEST, packet);
	}
}
