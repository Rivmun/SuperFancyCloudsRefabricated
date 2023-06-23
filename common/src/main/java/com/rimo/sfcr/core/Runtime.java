package com.rimo.sfcr.core;

import java.util.Random;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import com.rimo.sfcr.network.Network;
import com.rimo.sfcr.util.WeatherType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class Runtime {

	private CommonConfig config = SFCReMod.COMMON_CONFIG_HOLDER.getConfig();

	public long seed = new Random().nextLong();
	public double time = 0;
	public int fullOffset = 0;
	public double partialOffset = 0;

	private RegistryKey<World> worldKey;
	public WeatherType nextWeather = WeatherType.CLEAR;

	private double lastSyncTime = 0;

	public void init(ServerWorld world) {
		seed = new Random().nextLong();
		worldKey = world.getRegistryKey();
	}

	public void tick(MinecraftServer server) {

		if (server.isDedicated())
			partialOffset += 1 / 20f * 0.25f * 0.25f * config.getCloudBlockSize() / 16f;		// 20 tick per second.
		time += 1 / 20f;

		// Weather Pre-detect
		var worldProperties = ((ServerWorldAccessor) server.getWorld(worldKey)).getWorldProperties();
		var currentWeather = nextWeather;
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				nextWeather = worldProperties.getThunderTime() / 20 < config.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.THUNDER;
			} else {
				nextWeather = worldProperties.getThunderTime() / 20 < config.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
						? WeatherType.THUNDER
						: worldProperties.getRainTime() / 20 < config.getWeatherPreDetectTime() ? WeatherType.CLEAR : WeatherType.RAIN;
			}
		} else {
			if (worldProperties.getClearWeatherTime() != 0) {
				nextWeather = worldProperties.getClearWeatherTime() / 20 < config.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.CLEAR;
			} else {
				nextWeather = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < config.getWeatherPreDetectTime()
						? worldProperties.getRainTime() < worldProperties.getThunderTime() ? WeatherType.RAIN : WeatherType.THUNDER
						: WeatherType.CLEAR;
			}
		}
		if (nextWeather != currentWeather)
			Network.sendWeather(server);

		if (config.isEnableDebug() && server.getTicks() % (config.getWeatherPreDetectTime() * 20) == 0) {
			SFCReMod.LOGGER.info("isThnd: " + worldProperties.isThundering() + ", isRain: " + worldProperties.isRaining());
			SFCReMod.LOGGER.info("thndTime: " + worldProperties.getThunderTime() + ", rainTime: " + worldProperties.getRainTime() + ", clearTime: " + worldProperties.getClearWeatherTime());
			SFCReMod.LOGGER.info("nextWeather: " + nextWeather.toString());
		}
	}

	public void clientTick(World world) {

		// Weather Detect (Client) - Only runs when connected to a server without sync
		if (!MinecraftClient.getInstance().isIntegratedServerRunning() && lastSyncTime == 0)
			nextWeather = world.isThundering() ? WeatherType.THUNDER : world.isRaining() ? WeatherType.RAIN : WeatherType.CLEAR;

		// Auto Sync
		if (lastSyncTime < time - config.getSecPerSync()) {
			Network.sendSyncRequest(false);
			lastSyncTime = time;
		}
	}

	public void clientEnd() {
		lastSyncTime = 0;
	}

	public void checkFullOffset() {
		fullOffset += (int) partialOffset / config.getCloudBlockSize();
	}

	public void checkPartialOffset() {
		partialOffset = partialOffset % config.getCloudBlockSize();
	}

	public void updateConfig(CommonConfig config) {
		this.config = config;
	}
}
