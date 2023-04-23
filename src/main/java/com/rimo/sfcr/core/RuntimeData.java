package com.rimo.sfcr.core;

import com.rimo.sfcr.SFCReClient;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import com.rimo.sfcr.util.WeatherType;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class RuntimeData {

	public static Identifier PACKET_RUNTIME = new Identifier("sfcr", "runtime_s2c");
	public static Identifier PACKET_WEATHER = new Identifier("sfcr", "weather_s2c");

	public static SFCReConfig CONFIG = SFCReMain.CONFIGHOLDER.getConfig();

	public long seed = Random.create().nextLong();
	public double time = 0;
	public int fullOffset = 0;
	public double partialOffset = 0;

	private RegistryKey<World> worldKey;
	public WeatherType nextWeather = WeatherType.CLEAR;

	private double lastSyncTime = 0;

	public void init(MinecraftServer server, ServerWorld world) {
		seed = Random.create().nextLong();
		worldKey = world.getRegistryKey();
	}

	public void tick(MinecraftServer server) {

		if (server.isDedicated()) {
			// 20 tick per second.
			partialOffset += 1 / 20f;
			time += 1 / 20f;
		}

		// Weather Pre-detect
		var worldProperties = ((ServerWorldAccessor) server.getWorld(worldKey)).getWorldProperties();
		var currentWeather = nextWeather;
		if (worldProperties.isThundering()) {
			nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.CLEAR : WeatherType.THUNDER;
		} else if (worldProperties.isRaining()) {
			nextWeather = worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.CLEAR : WeatherType.RAIN;
		} else {
			if (worldProperties.getClearWeatherTime() != 0) {
				nextWeather = worldProperties.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.CLEAR;
			} else {
				nextWeather = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
						? worldProperties.getRainTime() < worldProperties.getThunderTime() ? WeatherType.RAIN : WeatherType.THUNDER
						: WeatherType.CLEAR;
			}
		}
		if (nextWeather != currentWeather)
			sendWeather(server);

		if (CONFIG.isEnableDebug() && server.getTicks() % (CONFIG.getWeatherPreDetectTime() * 20) == 0) {
			SFCReMain.LOGGER.info("isThnd: " + worldProperties.isThundering() + ", isRain: " + worldProperties.isRaining());
			SFCReMain.LOGGER.info("thndTime: " + worldProperties.getThunderTime() + ", rainTime: " + worldProperties.getRainTime() + ", clearTime: " + worldProperties.getClearWeatherTime());
			SFCReMain.LOGGER.info("currentWeather: " + currentWeather.toString() + ", nextWeather: " + nextWeather.toString());
		}
	}

	public void clientTick(World world) {

		// Fix up partial offset...
		partialOffset += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
		time += MinecraftClient.getInstance().getLastFrameDuration() / 20f;

		if (!MinecraftClient.getInstance().isIntegratedServerRunning() && lastSyncTime != 0)		// Only runs when connected to a server without SFCR
			nextWeather = world.isThundering() ? WeatherType.THUNDER : world.isRaining() ? WeatherType.RAIN : WeatherType.CLEAR;

		// Auto Sync
		if (lastSyncTime < time - CONFIG.getSecPerSync())
			SFCReClient.sendSyncRequest(false);
	}

	public void end() {
		// Do nothing here...
	}

	public void checkFullOffset() {
		fullOffset += (int) partialOffset / CONFIG.getCloudBlockSize();
	}

	public void checkPartialOffset() {
		partialOffset = partialOffset % CONFIG.getCloudBlockSize();
	}

	public RuntimeData getInstance() {
		return this;
	}

	public static void sendRuntimeData(ServerPlayerEntity player, MinecraftServer server) {
		if (!SFCReMain.config.isEnableMod())
			return;

		PacketByteBuf packet = PacketByteBufs.create();
		packet.writeDouble(SFCReMain.RUNTIME.time);
		packet.writeInt(SFCReMain.RUNTIME.fullOffset);
		packet.writeDouble(SFCReMain.RUNTIME.partialOffset);

		ServerPlayNetworking.send(player, PACKET_RUNTIME, packet);
	}

	public static void receiveRuntimeData(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		if (!SFCReMain.config.isEnableServerConfig())
			return;

		synchronized (SFCReMain.RUNTIME) {
			SFCReMain.RUNTIME.time = packet.readDouble();
			SFCReMain.RUNTIME.fullOffset = packet.readInt();
			SFCReMain.RUNTIME.partialOffset = packet.readDouble();
	
			SFCReMain.RUNTIME.lastSyncTime = SFCReMain.RUNTIME.time;
		}

		if (CONFIG.isEnableDebug())
			client.getMessageHandler().onGameMessage(Text.translatable("text.sfcr.command.sync_succ"), false);
	}

	public static void sendWeather(MinecraftServer server) {
		PacketByteBuf packet = PacketByteBufs.create();
		packet.writeEnumConstant(SFCReMain.RUNTIME.nextWeather);
		for (ServerPlayerEntity player : PlayerLookup.all(server)) {
			ServerPlayNetworking.send(player, PACKET_WEATHER, packet);
		}
	}

	public static void receiveWeather(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		synchronized (SFCReMain.RUNTIME) {
			SFCReMain.RUNTIME.nextWeather = packet.readEnumConstant(WeatherType.class);
		}
	}
}
