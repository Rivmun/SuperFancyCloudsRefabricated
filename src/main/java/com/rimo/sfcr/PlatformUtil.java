package com.rimo.sfcr;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
*///? } else {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//? }

import java.nio.file.Path;

public class PlatformUtil {
	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		//? if neoforge {
		/*PacketDistributor.sendToPlayer(player, payload);
		*///? } else {
		ServerPlayNetworking.send(player, payload);
		//? }
	}

	public static boolean isModLoaded(String id) {
		//? if neoforge {
		/*return ModList.get().isLoaded(id);
		*///? } else {
		return FabricLoader.getInstance().isModLoaded(id);
		//? }
	}

	public static void sendToServer(CustomPacketPayload payload) {
		//? if neoforge {
		/*ClientPacketDistributor.sendToServer(payload);
		*///? } else {
		ClientPlayNetworking.send(payload);
		//? }
	}

	public static void sendToAllPlayers(MinecraftServer server, CustomPacketPayload payload) {
		//? if neoforge {
		/*PacketDistributor.sendToAllPlayers(payload);
		*///? } else {
		server.getPlayerList().getPlayers().forEach(player ->
				ServerPlayNetworking.send(player, payload)
		);
		//? }
	}

	public static Path getConfigFolder() {
		//? if neoforge {
		/*return FMLPaths.CONFIGDIR.get();
		*///? } else {
		return FabricLoader.getInstance().getConfigDir();
		//? }
	}

	public static boolean isFabric() {
		//~ if fabric 'false' -> 'true'
		return true;
	}

//	public static boolean isNeoForge() {
//		//~ if neoforge 'false' -> 'true'
//		return true;
//	}

}
