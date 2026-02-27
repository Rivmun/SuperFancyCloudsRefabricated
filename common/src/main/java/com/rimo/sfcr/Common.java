package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.core.Data;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
	 * long seed - part of world seed use to init sampler, sent when player join this world.
	 */
	public static final Identifier PACKET_SEED = new Identifier(MOD_ID, "seed_s2c");
	/**
	 * Data.Weather - use to pre-detect function, sent when weather will be changed.
	 */
	public static final Identifier PACKET_WEATHER = new Identifier(MOD_ID, "weather_s2c");
	/**
	 * Contains:<br>
	 * 1.String dimensionName - use to load specific config, sent when player join at first time and dimension change. <br>
	 * 2.@Emptyable String dimensionConfigJson - specific configJson which existing on server side when serverConfig is enabled
	 */
	public static final Identifier PACKET_DIMENSION = new Identifier(MOD_ID, "dimension_s2c");
	/**
	 * an empty packet to notice client upload its config
	 */
	public static final Identifier PACKET_UPLOAD_REQUEST = new Identifier(MOD_ID, "upload_request_s2c");
	/**
	 * String SharedConfigJson
	 */
	public static final Identifier PACKET_SHARED_CONFIG = new Identifier(MOD_ID, "shared_config_c2s");

	private static final Map<String, String> CONFIG_CACHE = new ConcurrentHashMap<>();  // cache config to prevent high frequent IO
	private static final Object CACHE_LOCK = new Object();

	public static void init() {
		// Seed Sender
		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (! CONFIG.isEnableServer())
				return;
			long seed = player.getServerWorld().getSeed() >> 5 & 0x7FFFFFFFFFFFFFFFL;  // don't send actually seed for anti-cheat
			NetworkManager.sendToPlayer(player, PACKET_SEED, new PacketByteBuf(Unpooled.buffer())
					.writeVarLong(seed)
			);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send seed {} to {}", MOD_ID, seed, player.getName().getString());

			// Dimension Sender also run here because CHANGE_DIMENSION event not be called when join
			sendDimensionPacket(player, player.getServerWorld().getRegistryKey());
		});

		PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
			MinecraftServer server = player.getServer();
			// Always send config to host whatever isEnable, to prevent function shutdown when read a config which enabled is not.
			if (! CONFIG.isEnableServer() && server != null && ! server.isHost(player.getGameProfile()))
				return;
			sendDimensionPacket(player, newLevel);
		});

		// Weather Sender
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (!CONFIG.isEnableServer())
				return;
			if (world.getTime() % 20 == 0 && DATA.updateWeather(world)) {
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
	private static void sendDimensionPacket(ServerPlayerEntity player, RegistryKey<World> newLevel) {
		String name = newLevel.getValue().toString();
		String configJson = CONFIG.isEnableServer() ? getDimensionConfigJson(name) : "";
		NetworkManager.sendToPlayer(player, PACKET_DIMENSION, new PacketByteBuf(Unpooled.buffer())
				.writeString(name)
				.writeString(configJson)
		);
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
