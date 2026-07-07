package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.SharedConfig;
import com.rimo.sfcr.core.AbstractSeasonCompat;
import com.rimo.sfcr.core.Data;
import com.rimo.sfcr.core.Sampler;
//~ if neoforge 'fabric' -> 'neoforge'
import com.rimo.sfcr.loaders.fabric.Platform;
import com.rimo.sfcr.mixin.Plugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
	public record WeatherPayload(Data.Weather weather) implements CustomPacketPayload {
		public static final Type<WeatherPayload> TYPE = new CustomPacketPayload.Type<>(VersionUtil.getId("weather_s2c"));
		public static final StreamCodec<FriendlyByteBuf, WeatherPayload> CODEC = StreamCodec.of(
				(buf, value) -> buf.writeEnum(value.weather),
				buf -> new WeatherPayload(buf.readEnum(Data.Weather.class))
		);
		@Override
		public @NotNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
	/**
	 * Contains:<br>
	 * 1.String dimensionName - use to load specific config, sent when player join at first time and dimension change. <br>
	 * 2.@Emptyable String dimensionConfigJson - specific configJson which existing on server side when serverConfig is enabled<br>
	 * 3.Long seed - use to init sampler.
	 */
	public record DimensionPayload(String name, String sharedConfigJson, long seed) implements CustomPacketPayload {
		public static final Type<DimensionPayload> TYPE = new CustomPacketPayload.Type<>(VersionUtil.getId("dimension"));
		public static final StreamCodec<FriendlyByteBuf, DimensionPayload> CODEC = StreamCodec.of(
				((buf, value) -> {
					buf.writeUtf(value.name);
					buf.writeUtf(value.sharedConfigJson);
					buf.writeLong(value.seed);
				}),
				buf -> new DimensionPayload(buf.readUtf(), buf.readUtf(), buf.readLong())
		);
		@Override
		public @NotNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
	/**
	 * an empty packet to notice client upload its config
	 */
	public record UploadRequestPayload() implements CustomPacketPayload {
		public static final Type<UploadRequestPayload> TYPE = new CustomPacketPayload.Type<>(VersionUtil.getId("upload_request_s2c"));
		public static final StreamCodec<FriendlyByteBuf, UploadRequestPayload> CODEC = StreamCodec.of(
				((buf, value) -> {}),
				buf -> new UploadRequestPayload()
		);
		@Override
		public @NotNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private record DimensionData(long seed, String configJson, Sampler sampler) {}
	private static final ConcurrentHashMap<String, DimensionData> DIMENSION_CACHE = new ConcurrentHashMap<>();  // cache config to prevent high frequent IO. key is dimensionName.
	static final Set<ServerPlayer> playerWithSfcr = ConcurrentHashMap.newKeySet();

	private static final Object debugLock = new Object();
	private static long apiDebugTime = 0L;
	private static int apiCallCounter = 0;
	private static String debugString = "";

	public static void onPlayerJoin(ServerPlayer player) {
		if (Platform.canReceive(player, WeatherPayload.TYPE)) {
			playerWithSfcr.add(player);
		} else {
			return;
		}
		sendDimensionPacket(player, player.level().dimension());
	}

	public static void onPlayerChangedDimension(ServerPlayer player, ResourceKey<Level> destination) {
		sendDimensionPacket(player, destination);
	}

	public static void onPlayerQuit(ServerPlayer player) {
		playerWithSfcr.remove(player);
	}

	public static void onTick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0)
			return;
		// Weather is a common stat between different level, just check once
		ServerLevel level = server.overworld();
		// Sender
		//~ if = 1.21.11 'server' -> 'level'
		if (DATA.updateWeather(server) && CONFIG.isEnableServer()) {  // always update
			Data.Weather nextWeather = DATA.getNextWeather();
			playerWithSfcr.forEach(player ->
					Platform.sendToPlayer(player, new WeatherPayload(nextWeather))
			);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} broadcast next weather: {}", MOD_ID, nextWeather);
		}
		// update
		DATA.updateWeatherDensity(level);

		//debug
		if (CONFIG.isEnableDebug()) {
			updateDebugString();
			Plugin.checkMixinApplied();
		}
	}

	public static void onLevelTick(ServerLevel level) {
		if (seasonHandler == null)
			return;
		if (level.getGameTime() % 24000 != 0)
			return;
		// season base on time, but different level may have different time, so we must update it in level tick instead of server.
		DimensionData data = DIMENSION_CACHE.get(level.dimension().identifier().toString());
		if (data == null)
			return;
		data.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(level));
	}

	// Dimension Packet Sender
	private static void sendDimensionPacket(ServerPlayer player, ResourceKey<Level> key) {
		MinecraftServer server = player.level().getServer();
		boolean isHost = ! server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
		if (! isHost && (! CONFIG.isEnableServer() || ! playerWithSfcr.contains(player)))
			return;
		String name = key.identifier().toString();
		DimensionData data = loadDimensionData(player.level());
		Platform.sendToPlayer(player, new DimensionPayload(
				name,
				data.configJson,
				data.seed
		));
		if (CONFIG.isEnableDebug())
			LOGGER.info("{} send dimension '{}' packet to {}", MOD_ID, name, player.getName().getString());
	}

	private static long getSeed(ServerLevel level) {
		return level.getSeed() >> 5 & 0x7FFFFFFFFFFFFFFFL;  // don't send actually seed for anti-cheat
	}

	/**
	 * Load a dimension config into cache, or refresh its config and sampler.
	 * @return the newest cache of this Level.
	 */
	private static DimensionData loadDimensionData(ServerLevel level) {
		String name = level.dimension().identifier().toString();
		SharedConfig config = new SharedConfig();
		String configJson = config.load(name) || name.equals(Config.OVERWORLD) ? config.toString() : "";
		if (CONFIG.isEnableDebug())
			LOGGER.info("load dimensionData {} into cache...", name);
		return DIMENSION_CACHE.compute(name, (key, existing) -> {
			if (existing == null) {
				long seed = getSeed(level);
				Sampler sampler = new Sampler().setSeed(seed).setLevel(level).setConfig(config);
				return new DimensionData(seed, configJson, sampler);
			} else {
				existing.sampler.setConfig(config).setLevel(level);
				return new DimensionData(existing.seed(), configJson, existing.sampler());
			}
		});
	}

	private static @Nullable DimensionData getDimensionData(Level level, String name) {
		DimensionData data = DIMENSION_CACHE.get(name);
		if (data == null) {
			if (level instanceof ServerLevel) {
				data = loadDimensionData((ServerLevel) level);
			}
		}
		return data;
	}

	/**
	 * @see #loadDimensionData(ServerLevel)
	 */
	public static void addDimensionData(ServerLevel level) {
		loadDimensionData(level);
	}

	public static void removeDimensionData(ServerLevel level) {
		String name = level.dimension().identifier().toString();
		DIMENSION_CACHE.remove(name);
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
				existing.sampler.setConfig(new SharedConfig().fromString(configJson));
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
	 * @param level Current level/level
	 * @param x Components of target level pos
	 * @param y Components of target level pos
	 * @param z Components of target level pos
	 * @return Always {@code false} if this point above cloud, or cache of this level not found, or 'NCNR logical' function disabled.
	 */
	public static boolean isNoCloudCovered(Level level, double x, double y, double z) {
		if (! CONFIG.isCloudRainLogically())
			return false;
		long time = System.nanoTime();
		String name = level.dimension().identifier().toString();
		DimensionData data = getDimensionData(level, name);
		boolean result = data != null && ! data.sampler.isCloudCovered(x, y, z);
		if (CONFIG.isEnableDebug())
			recordApiTime(System.nanoTime() - time);
		return result;
	}

	/**
	 * @see #isNoCloudCovered(Level, double, double, double)
	 * @return {@code true} if this point has cloudBlock.
	 */
	public static boolean isCloud(Level level, double x, double y, double z) {
		long time = System.nanoTime();
		String name = level.dimension().identifier().toString();
		DimensionData data = getDimensionData(level, name);
		boolean result = data != null && data.sampler.isCloud(x, y, z);
		if (CONFIG.isEnableDebug())
			recordApiTime(System.nanoTime() - time);
		return result;
	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		StringBuilder text = new StringBuilder(MOD_ID + " got an error:\n" + e.toString());
		for (StackTraceElement i : e.getStackTrace()) {
			text.append("\n    at ").append(i.toString());
		}
		LOGGER.error(text.toString());
	}

	private static void recordApiTime(long duration) {
		synchronized (debugLock) {
			apiDebugTime += duration;
			apiCallCounter ++;
		}
	}

	private static void updateDebugString() {
		float time;
		int counter;
		synchronized (debugLock) {
			time = (float) (apiDebugTime / 1e6);
			counter = apiCallCounter;
			apiDebugTime = 0L;
			apiCallCounter = 0;
		}
		if (counter == 0) {
			debugString = "[SFCR] Api hasn't called yet in last 20 ticks.";
			return;
		}
		debugString = String.format("[SFCR] Api was %scall/s, avg %.1fcall/t, cost %.4fms/t, %.4fms/call",
				counter, counter / 20F, time / 20F, time / counter);
	}

	public static String getDebugString() {
		return debugString;
	}
}
