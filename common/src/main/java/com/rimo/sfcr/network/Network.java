package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.util.WeatherType;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public class Network {
	public static final Identifier CHANNEL_CONFIG = new Identifier(SFCReMod.MOD_ID, "config_s2c");
	public static final Identifier CHANNEL_RUNTIME = new Identifier(SFCReMod.MOD_ID, "runtime_s2c");
	public static final Identifier PACKET_WEATHER = new Identifier(SFCReMod.MOD_ID, "weather_s2c");

	public static final NetworkChannel CONFIG_CHANNEL = NetworkChannel.create(CHANNEL_CONFIG);
	public static final NetworkChannel RUNTIME_CHANNEL = NetworkChannel.create(CHANNEL_RUNTIME);

	public static void init() {
		CONFIG_CHANNEL.register(ConfigSyncMessage.class, ConfigSyncMessage::encode, ConfigSyncMessage::new, ConfigSyncMessage::receive);
		RUNTIME_CHANNEL.register(RuntimeSyncMessage.class, RuntimeSyncMessage::encode, RuntimeSyncMessage::new, RuntimeSyncMessage::receive);
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_SYNC_REQUEST, Network::receiveSyncRequest);
	}

	public static void initClient() {
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_WEATHER, Network::receiveWeather);
	}

	public static void sendWeather(MinecraftServer server) {
		server.getPlayerManager().getPlayerList().forEach(player -> {
			NetworkManager.sendToPlayer(
					player,
					PACKET_WEATHER,
					new PacketByteBuf(Unpooled.buffer()).writeEnumConstant(SFCReMod.RUNTIME.nextWeather)
			);
		});

		if (SFCReMod.COMMON_CONFIG.isEnableDebug())
			SFCReMod.LOGGER.info("Sent weather forecast '" + SFCReMod.RUNTIME.nextWeather.toString() + "' to allPlayers.");
	}

	@Environment(EnvType.CLIENT)
	public static void receiveWeather(PacketByteBuf packet, NetworkManager.PacketContext content) {
		synchronized (SFCReMod.RUNTIME) {
			SFCReMod.RUNTIME.nextWeather = packet.readEnumConstant(WeatherType.class);
		}
	}
}
