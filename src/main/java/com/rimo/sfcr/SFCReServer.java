package com.rimo.sfcr;

import com.rimo.sfcr.register.Command;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class SFCReServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		Command.register();
		ServerPlayNetworking.registerGlobalReceiver(SFCReMain.PACKET_SYNC_REQUEST, SFCReServer::receiveSyncRequest);
	}
	
	@Environment(EnvType.SERVER)
	public static void receiveSyncRequest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		SFCReRuntimeData.sendInitialData(player, server);
		if (packet.readBoolean())
			SFCReMain.sendConfig(player, server);
	}

}
