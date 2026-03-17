package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.SharedConfig;
import com.rimo.sfcr.core.AbstractSeasonCompat;
import com.rimo.sfcr.core.Data;
import com.rimo.sfcr.core.Sampler;
import com.rimo.sfcr.mixin.Plugin;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Common {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = new Config().load();
	public static final Data DATA = new Data(CONFIG);
	public static AbstractSeasonCompat seasonHandler = AbstractSeasonCompat.getInstance(CONFIG);
	/**
	 * Data.Weather - use to pre-detect function, sent when weather will be changed.
	 */
	public static final Identifier PACKET_WEATHER = new Identifier(MOD_ID, "weather_s2c");
	/**
	 * Contains:<br>
	 * 1.String dimensionName - use to load specific config, sent when player join at first time and dimension change. <br>
	 * 2.@Emptyable String dimensionConfigJson - specific configJson which existing on server side when serverConfig is enabled<br>
	 * 3.Long seed - use to init sampler.
	 */
	public static final Identifier PACKET_DIMENSION = new Identifier(MOD_ID, "dimension");
	/**
	 * an empty packet to notice client upload its config
	 */
	public static final Identifier PACKET_UPLOAD_REQUEST = new Identifier(MOD_ID, "upload_request_s2c");

	private record DimensionData(long seed, String configJson, Sampler sampler) {}
	private static final ConcurrentHashMap<String, DimensionData> DIMENSION_CACHE = new ConcurrentHashMap<>();  // cache config to prevent high frequent IO

	private static final Set<Long> apiDebugTime = ConcurrentHashMap.newKeySet();
	public static String debugString;

	public static void init() {
		// manually mixin debug
		LifecycleEvent.SETUP.register(() -> {
			Set<String> list = Plugin.MIXINS;
			if (! list.isEmpty()) {
				StringBuilder str = new StringBuilder();
				for (String s : list)
					str.append("  ").append(s).append("\n");
				LOGGER.error("{} was failed to apply mixin(s):\n{}Some function may no work.", MOD_ID,  str);
			}
		});

		// dimension cache system
		LifecycleEvent.SERVER_LEVEL_LOAD.register(Common::loadDimensionData);
		LifecycleEvent.SERVER_LEVEL_UNLOAD.register(world -> {
			String name = world.getRegistryKey().getValue().toString();
			DIMENSION_CACHE.remove(name);
		});

		// Dimension Sender
		PlayerEvent.PLAYER_JOIN.register(player -> {
			MinecraftServer server = player.getServer();
			// Always send config to host whatever isEnable, to prevent function shutdown when read a config which enabled is not.
			if (! CONFIG.isEnableServer() && server != null && ! server.isHost(player.getGameProfile()))
				return;
			sendDimensionPacket(player, player.getServerWorld().getRegistryKey());
		});
		PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
			MinecraftServer server = player.getServer();
			if (! CONFIG.isEnableServer() && server != null && ! server.isHost(player.getGameProfile()))
				return;
			sendDimensionPacket(player, newLevel);
		});

		TickEvent.SERVER_POST.register(server -> {
			if (server.getTicks() % 20 != 0)
				return;
			// Weather is a common stat between different world, just check once
			ServerWorld world = server.getOverworld();
			// Sender
			if (DATA.updateWeather(world)) {  // always update
				if (! CONFIG.isEnableServer())
					return;
				Data.Weather nextWeather = DATA.getNextWeather();
				NetworkManager.sendToPlayers(server.getPlayerManager().getPlayerList(), PACKET_WEATHER, new PacketByteBuf(Unpooled.buffer())
						.writeEnumConstant(nextWeather)
				);
				if (CONFIG.isEnableDebug())
					LOGGER.info("{} broadcast next weather: {}", MOD_ID, nextWeather);
			}
			// update
			DATA.updateWeatherDensity(world);

			//debug
			if (! apiDebugTime.isEmpty()) {
				double time = apiDebugTime.stream().mapToLong(t -> t).sum() / 1000000F;
				int size = apiDebugTime.size();
				debugString = "[SFCR] Api was " + size + " call/s, avg " + String.format("%.1f", size / 20F) +
						"call/t, cost " + String.format("%.4f", time / 20) + "ms/t, " + String.format("%.4f", time / size) + "ms/call.";
				apiDebugTime.clear();
			}
		});

		if (seasonHandler == null)
			return;
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (world.getTime() % 24000 != 0)
				return;
			// season base on time, but different world may have different time, so we must update it in level tick instead of server.
			DimensionData data = DIMENSION_CACHE.get(world.getRegistryKey().getValue().toString());
			if (data == null)
				return;
			data.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(world));
		});
	}

	// Dimension Packet Sender
	private static void sendDimensionPacket(ServerPlayerEntity player, RegistryKey<World> key) {
		String name = key.getValue().toString();
		DimensionData data = loadDimensionData(player.getServerWorld());
		NetworkManager.sendToPlayer(player, PACKET_DIMENSION, new PacketByteBuf(Unpooled.buffer())
				.writeString(name)
				.writeString(data.configJson)
				.writeVarLong(data.seed)
		);
		if (CONFIG.isEnableDebug())
			LOGGER.info("{} send dimension '{}' packet to {}", MOD_ID, name, player.getName().getString());
	}

	private static long getSeed(ServerWorld world) {
		return world.getSeed() >> 5 & 0x7FFFFFFFFFFFFFFFL;  // don't send actually seed for anti-cheat
	}

	/**
	 * Load a dimension config into cache, or refresh its config and sampler.
	 * @return the newest cache of this world.
	 */
	private static DimensionData loadDimensionData(ServerWorld world) {
		String name = world.getRegistryKey().getValue().toString();
		Config config = new Config();
		String configJson = config.load(name) ? config.toString() : "";
		return DIMENSION_CACHE.compute(name, (key, existing) -> {
			if (existing == null) {
				long seed = getSeed(world);
				Sampler sampler = new Sampler().setSeed(seed).setWorld(world).setConfig(config);
				return new DimensionData(seed, configJson, sampler);
			} else {
				existing.sampler.setConfig(config).setWorld(world);
				return new DimensionData(existing.seed(), configJson, existing.sampler());
			}
		});
	}

	/**
	 * Refresh specific dimension cache config from JSON String.<br>
	 * Will do nothing if {@link #DIMENSION_CACHE} no have such dimension
	 * @param dimensionName Syntax example: 'minecraft:overworld'
	 * @param configJson Generated by {@link SharedConfig#toString()}
	 */
	public static void setDimensionConfigJson(String dimensionName, String configJson) {
		try {
			DIMENSION_CACHE.computeIfPresent(dimensionName, (key, existing) -> {
				existing.sampler.setConfig(new Config().fromString(configJson));
				return new DimensionData(existing.seed, configJson, existing.sampler());
			});
		} catch (JsonSyntaxException ignored) {}
	}

	/**
	 * Get specific config of dimension from cache
	 * @return a String generated by {@link SharedConfig#toString()}, or {@code null} if specific dimension doesn't exist in cache
	 */
	public static @Nullable String getDimensionConfigJson(String dimensionName) {
		try {
			return DIMENSION_CACHE.get(dimensionName).configJson;
		} catch (Exception e) {
			return null;
		}
	}

	static void clearDimensionCache() {
		DIMENSION_CACHE.clear();
	}

	/**
	 * For logical side, use {@link Sampler#isCloudCovered(double, double, double)} from {@link #DIMENSION_CACHE} to calculate where is no cloud above.<br>
	 * If you are client, {@link Client#isNoCloudCovered(double, double, double)} is recommended.
	 * @param world Current world/level
	 * @param x Components of target world pos
	 * @param y Components of target world pos
	 * @param z Components of target world pos
	 * @return Always {@code false} if this point above cloud, or cache of this world not found, or 'NCNR logical' function disabled.
	 */
	public static boolean isNoCloudCovered(World world, double x, double y, double z) {
		long time = System.nanoTime();
		boolean result = _isNoCloudCovered(world, x, y, z);
		apiDebugTime.add(System.nanoTime() - time);
		return result;
	}
	private static boolean _isNoCloudCovered(World world, double x, double y, double z) {
		if (! CONFIG.isCloudRainLogically())
			return false;
		String name = world.getRegistryKey().getValue().toString();
		DimensionData data = DIMENSION_CACHE.get(name);
		if (data == null) {
			if (world instanceof ServerWorld serverWorld) {
				data = loadDimensionData(serverWorld);
			} else {
				return false;
			}
		}
		return ! data.sampler.isCloudCovered(x, y, z);
	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		StringBuilder text = new StringBuilder(MOD_ID + " got an error:\n" + e.toString());
		for (StackTraceElement i : e.getStackTrace()) {
			text.append("\n    at ").append(i.toString());
		}
		LOGGER.error(text.toString());
	}
}
