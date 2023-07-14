package com.rimo.sfcr.core;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import com.rimo.sfcr.network.ConfigSyncMessage;
import com.rimo.sfcr.network.Network;
import com.rimo.sfcr.network.RuntimeSyncMessage;
import com.rimo.sfcr.util.WeatherType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Runtime {

	private final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

	public long seed = new Random().nextLong();
	public double time = 0;
	public int fullOffset = 0;
	public double partialOffset = 0;

	private RegistryKey<World> worldKey;
	public WeatherType nextWeather = WeatherType.CLEAR;
	private List<ServerPlayerManager> playerList;

	public void init(ServerWorld world) {
		seed = new Random().nextLong();
		worldKey = world.getRegistryKey();
		playerList = new ArrayList<>();
	}

	public void tick(MinecraftServer server) {

		if (!CONFIG.isEnableMod())
			return;

		if (server.isDedicated()) {		// These data is updated by RENDERER, but dedicated server is not have, so we update here.
			partialOffset += 1 / 20f * CONFIG.getCloudBlockSize() / 16f;
			checkFullOffset();
			checkPartialOffset();
		}
		time += 1 / 20f;		// 20 tick per second.

		// Weather Pre-detect
		var worldProperties = ((ServerWorldAccessor) server.getWorld(worldKey)).getWorldProperties();
		var currentWeather = nextWeather;
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.THUNDER;
			} else {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
						? WeatherType.THUNDER
						: worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.CLEAR : WeatherType.RAIN;
			}
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
			Network.sendWeather(server);

		// Data Sync
		if (server.getTicks() % 20 == 0 && (server.isDedicated() || CONFIG.isEnableServerConfig())) {
			playerList.forEach(manager -> {
				if (manager.lastSyncTime < time - CONFIG.getSecPerSync()) {
					Network.RUNTIME_CHANNEL.sendToPlayer(manager.player, new RuntimeSyncMessage(SFCReMod.RUNTIME));
					manager.lastSyncTime = time;
				}
			});
		}

		if (CONFIG.isEnableDebug() && !server.isDedicated() && server.getTicks() % (CONFIG.getWeatherPreDetectTime() * 20) == 0) {
			SFCReMod.LOGGER.info("isThnd: " + worldProperties.isThundering() + ", isRain: " + worldProperties.isRaining());
			SFCReMod.LOGGER.info("thndTime: " + worldProperties.getThunderTime() + ", rainTime: " + worldProperties.getRainTime() + ", clearTime: " + worldProperties.getClearWeatherTime());
			SFCReMod.LOGGER.info("nextWeather: " + nextWeather.toString());
		}
	}

	public void clientTick(World world) {
		// Weather Detect (Client) - Only runs when connected to a server without sync
		if (!MinecraftClient.getInstance().isIntegratedServerRunning() && CONFIG.isEnableServerConfig())
			nextWeather = world.isThundering() ? WeatherType.THUNDER : world.isRaining() ? WeatherType.RAIN : WeatherType.CLEAR;
	}

	public void checkFullOffset() {
		fullOffset += (int) partialOffset / CONFIG.getCloudBlockSize();
	}

	public void checkPartialOffset() {
		partialOffset = partialOffset % CONFIG.getCloudBlockSize();
	}

	public List<ServerPlayerManager> getPlayerList() {
		return playerList;
	}

	public void addPlayer(ServerPlayerEntity player) {
		if (playerList.stream().noneMatch(manager -> manager.getPlayer() == player))
			return;
		playerList.add(new ServerPlayerManager(player));
	}

	public void removePlayer(ServerPlayerEntity player) {
		playerList.removeIf(manager -> manager.getPlayer() == player);
	}

	public void end(MinecraftServer server) {
		playerList.clear();
	}

	public void clientEnd() {
		//
	}

	public class ServerPlayerManager {
		private final ServerPlayerEntity player;
		private double lastSyncTime;

		ServerPlayerManager(ServerPlayerEntity player) {
			this.player = player;
			this.lastSyncTime = time;
			if (player.getServer().isDedicated() || CONFIG.isEnableServerConfig()) {
				Network.CONFIG_CHANNEL.sendToPlayer(player, new ConfigSyncMessage(seed, SFCReMod.COMMON_CONFIG));
				Network.RUNTIME_CHANNEL.sendToPlayer(player, new RuntimeSyncMessage(SFCReMod.RUNTIME));
			}
		}

		public ServerPlayerEntity getPlayer() {
			return player;
		}

		public void setLastSyncTime(double time) {
			lastSyncTime = time;
		}

		public double getLastSyncTime() {
			return lastSyncTime;
		}
	}
}
