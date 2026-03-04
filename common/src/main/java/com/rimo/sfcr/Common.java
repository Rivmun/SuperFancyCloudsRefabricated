package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.core.Data;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Common {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = new Config().load();
	public static final Data DATA = new Data(CONFIG);
	/**
	 * long seed - part of world seed use to init sampler, sent when player join this world.
	 */
	public record SeedPayload(long seed) implements CustomPacketPayload {
		public static final Type<SeedPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "seed_s2c"));
		public static final StreamCodec<FriendlyByteBuf, SeedPayload> CODEC = StreamCodec.of(
				(buf, value) -> buf.writeLong(value.seed),
				buf -> new SeedPayload(buf.readLong())
		);
		@Override
		public @NotNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
	/**
	 * Data.Weather - use to pre-detect function, sent when weather will be changed.
	 */
	public record WeatherPayload(Data.Weather weather) implements CustomPacketPayload {
		public static final Type<WeatherPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "weather_s2c"));
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
	 * 2.@Emptyable String dimensionConfigJson - specific configJson which existing on server side when serverConfig is enabled
	 */
	public record DimensionPayload(String name, String sharedConfigJson) implements CustomPacketPayload {
		public static final Type<DimensionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "dimension"));
		public static final StreamCodec<FriendlyByteBuf, DimensionPayload> CODEC = StreamCodec.of(
				((buf, value) -> {
					buf.writeUtf(value.name);
					buf.writeUtf(value.sharedConfigJson);
				}),
				buf -> new DimensionPayload(buf.readUtf(), buf.readUtf())
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
		public static final Type<UploadRequestPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "upload_request_s2c"));
		public static final StreamCodec<FriendlyByteBuf, UploadRequestPayload> CODEC = StreamCodec.of(
				((buf, value) -> {}),
				buf -> new UploadRequestPayload()
		);
		@Override
		public @NotNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final Map<String, String> CONFIG_CACHE = new ConcurrentHashMap<>();  // cache config to prevent high frequent IO
	private static final Object CACHE_LOCK = new Object();

	public static void init() {
		// Seed Sender
		PlayerEvent.PLAYER_JOIN.register(player -> {
			MinecraftServer server = player.getServer();
			if (! CONFIG.isEnableServer() && server != null && ! server.isSingleplayerOwner(player.getGameProfile()))
				return;
			long seed = player.serverLevel().getSeed() >> 5 & 0x7FFFFFFFFFFFFFFFL;  // don't send actually seed for anti-cheat
			NetworkManager.sendToPlayer(player, new SeedPayload(seed));
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send seed {} to {}", MOD_ID, seed, player.getName().getString());

			// Dimension Sender also run here because CHANGE_DIMENSION event not be called when join
			sendDimensionPacket(player, player.level().dimension());
		});

		PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
			MinecraftServer server = player.getServer();
			// Always send config to host whatever isEnable, to prevent function shutdown when read a config which enabled is not.
			if (! CONFIG.isEnableServer() && server != null && ! server.isSingleplayerOwner(player.getGameProfile()))
				return;
			sendDimensionPacket(player, newLevel);
		});

		// Weather Sender
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (world.getGameTime() % 20 == 0 && DATA.updateWeather(world)) {  // always update
				if (!CONFIG.isEnableServer())
					return;
				Data.Weather nextWeather = DATA.getNextWeather();
				NetworkManager.sendToPlayers(world.players(), new WeatherPayload(nextWeather));
				if (CONFIG.isEnableDebug())
					LOGGER.info("{} broadcast next weather: {}", MOD_ID, nextWeather);
			}
		});
	}

	// Dimension Packet Sender
	private static void sendDimensionPacket(ServerPlayer player, ResourceKey<Level> newLevel) {
		String name = newLevel.location().toString();
		String configJson = CONFIG.isEnableServer() ? getDimensionConfigJson(name) : "";
		NetworkManager.sendToPlayer(player, new DimensionPayload(
				name,
				configJson
		));
		if (CONFIG.isEnableDebug())
			LOGGER.info("{} send dimension '{}' packet to {}", MOD_ID, name, player.getName().getString());
	}

	static void setDimensionConfigJson(String dimensionName, String configJson) {
		synchronized (CACHE_LOCK) {
			clearConfigCache(dimensionName);
			CONFIG_CACHE.put(dimensionName, configJson);
		}
	}

	// - concurrent check powered by doubao.ai
	static String getDimensionConfigJson(String dimensionName) {
		if (CONFIG_CACHE.containsKey(dimensionName))
			return CONFIG_CACHE.get(dimensionName);
		synchronized (CACHE_LOCK) {
			if (CONFIG_CACHE.containsKey(dimensionName))  //check again
				return CONFIG_CACHE.get(dimensionName);
			Config config = new Config();
			String configJson = config.load(dimensionName) ?
					config.toString() :
					"";
			CONFIG_CACHE.put(dimensionName, configJson);
			return configJson;
		}
	}

	/**
	 * @param name syntax like "minecraft:overworld", input null to clear all
	 */
	public static void clearConfigCache(@Nullable String name) {
		if (name == null)
			CONFIG_CACHE.clear();
		else
			CONFIG_CACHE.remove(name);
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
