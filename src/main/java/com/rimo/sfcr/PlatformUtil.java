package com.rimo.sfcr;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlatformUtil {
	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	public static boolean isModLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}

	public static Path getConfigFolder() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static boolean isFabric() {
		return true;
	}
}
