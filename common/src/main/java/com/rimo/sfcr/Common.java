package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.SharedConfig;
import com.rimo.sfcr.core.Data;
import com.rimo.sfcr.core.Sampler;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Common {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = new Config().load();
	public static final Data DATA = new Data(CONFIG);
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

	private static final Map<String, DimensionData> DIMENSION_CACHE = new ConcurrentHashMap<>();  // cache config to prevent high frequent IO

	public static void init() {
		//create new dimension cache
		LifecycleEvent.SERVER_LEVEL_LOAD.register(world -> {
			String name = world.getRegistryKey().getValue().toString();
			Config config = new Config();
			String configJson = config.load(name) ? config.toString() : "";
			DIMENSION_CACHE.compute(name, (key, existing) -> {
				if (existing == null) {
					long seed = getSeed(world);
					Sampler sampler = new Sampler(seed);
					return new DimensionData(seed, configJson, sampler);
				} else {
					existing.sampler.setSamplerData(config);
					return new DimensionData(existing.seed(), configJson, existing.sampler());
				}
			});
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

		// Weather Sender
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (world.getTime() % 20 == 0 && DATA.updateWeather(world)) {  // always update
				if (!CONFIG.isEnableServer())
					return;
				Data.Weather nextWeather = DATA.getNextWeather();
				NetworkManager.sendToPlayers(world.getPlayers(), PACKET_WEATHER, new PacketByteBuf(Unpooled.buffer())
						.writeEnumConstant(nextWeather)
				);
				if (CONFIG.isEnableDebug())
					LOGGER.info("{} broadcast next weather: {}", MOD_ID, nextWeather);
			}
		});
	}

	// Dimension Packet Sender
	private static void sendDimensionPacket(ServerPlayerEntity player, RegistryKey<World> key) {
		String name = key.getValue().toString();
		DimensionData data = DIMENSION_CACHE.computeIfAbsent(name, name1 ->
			getDimensionData(player.getServerWorld())
		);
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
	 * Will do nothing if {@link #DIMENSION_CACHE} no have such dimension
	 */
	static void setDimensionConfigJson(String dimensionName, String configJson) {
		try {
			DIMENSION_CACHE.computeIfPresent(dimensionName, (key, existing) -> {
				existing.sampler.setSamplerData(new Config().fromString(configJson));
				return new DimensionData(existing.seed, configJson, existing.sampler());
			});
		} catch (JsonSyntaxException ignored) {}
	}

	static String getDimensionConfigJson(ServerWorld world) {
		String name = world.getRegistryKey().getValue().toString();
		return DIMENSION_CACHE.computeIfAbsent(name, key ->
			getDimensionData(world)
		).configJson;
	}

	/**
	 * @param name syntax like "minecraft:overworld", input null to clear all
	 */
	public static void clearConfigCache(@Nullable String name) {
		if (name == null)
			DIMENSION_CACHE.clear();
		else
			DIMENSION_CACHE.remove(name);
	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		StringBuilder text = new StringBuilder(MOD_ID + " got an error:\n" + e.toString());
		for (StackTraceElement i : e.getStackTrace()) {
			text.append("\n    at ").append(i.toString());
		}
		LOGGER.error(text.toString());
	}

	private static DimensionData getDimensionData(ServerWorld world) {
		String name = world.getRegistryKey().getValue().toString();
		long seed = getSeed(world);
		Config config = new Config();
		String configJson = config.load(name) ? config.toString() : "";
		Sampler sampler = new Sampler(seed);
		return new DimensionData(seed, configJson, sampler);
	}

	private record DimensionData(long seed, String configJson, Sampler sampler) {}
}
