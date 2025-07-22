package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.ServerWorldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common implements ModInitializer {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = Config.load();
	public static final Identifier PACKET_WORLD_SEED = Identifier.of(MOD_ID, "world_seed_packet");
	public static final Identifier PACKET_WEATHER = Identifier.of(MOD_ID, "weather_packet");

	private static Weather currentWeather;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(WorldInfoPayload.ID, WorldInfoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WeatherPayload.ID, WeatherPayload.CODEC);

		//world info sender
		ServerPlayerEvents.JOIN.register(player -> {
			if (! CONFIG.isEnableMod())
				return;
			ServerWorld world = player.getWorld();
			ServerPlayNetworking.send(player, new WorldInfoPayload(world.getSeed(), world.getTime() / 20f));
			if (CONFIG.isEnableDebug())
				LOGGER.info("Send " + world.getSeed() + ", " + world.getTime() / 20f + " to " + player.getName().getString());
		});

		//weather sender
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (! CONFIG.isEnableMod())
				return;
			// Weather Pre-detect
			Weather nextWeather;
			ServerWorldProperties worldProperties = ((ServerWorldAccessor) world).getWorldProperties();
			if (worldProperties.isRaining()) {
				if (worldProperties.isThundering()) {
					nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.RAIN : Weather.THUNDER;
				} else {
					nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
							? Weather.THUNDER
							: worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.CLEAR : Weather.RAIN;
				}
			} else {
				if (worldProperties.getClearWeatherTime() != 0) {
					nextWeather = worldProperties.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.RAIN : Weather.CLEAR;
				} else {
					nextWeather = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
							? worldProperties.getRainTime() < worldProperties.getThunderTime() ? Weather.RAIN : Weather.THUNDER
							: Weather.CLEAR;
				}
			}
			// Broadcast...
			if (currentWeather != nextWeather) {
				currentWeather = nextWeather;
				world.getPlayers().forEach(player ->
						ServerPlayNetworking.send(player, new WeatherPayload(nextWeather))
				);
				if (CONFIG.isEnableDebug())
					LOGGER.info("Broadcast next weather: " + nextWeather.name());
			}
		});
	}

	public record WorldInfoPayload(long seed, double time) implements CustomPayload {
		public static final Id<WorldInfoPayload> ID = new CustomPayload.Id<>(PACKET_WORLD_SEED);
		public static final PacketCodec<PacketByteBuf, WorldInfoPayload> CODEC = PacketCodec.of(
				(value, buf) -> buf.
						writeLong(value.seed).
						writeDouble(value.time),
				buf -> new WorldInfoPayload(
						buf.readLong(),
						buf.readDouble()
				)
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record WeatherPayload(Weather weather) implements CustomPayload {
		public static final Id<WeatherPayload> ID = new CustomPayload.Id<>(PACKET_WEATHER);
		public static final PacketCodec<PacketByteBuf, WeatherPayload> CODEC = PacketCodec.of(
				((value, buf) -> buf.
						writeEnumConstant(value.weather)
				),
				buf -> new WeatherPayload(
						buf.readEnumConstant(Weather.class)
				)
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}
