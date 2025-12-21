package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common implements ModInitializer {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier PACKET_WORLD_SEED = Identifier.fromNamespaceAndPath(MOD_ID, "world_seed_packet");
	public static final Identifier PACKET_WEATHER = Identifier.fromNamespaceAndPath(MOD_ID, "weather_packet");
	public static Config CONFIG = Config.load();
	public static final Data DATA = new Data(CONFIG);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(WorldInfoPayload.TYPE, WorldInfoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WeatherPayload.TYPE, WeatherPayload.CODEC);

		//world info sender
		ServerPlayerEvents.JOIN.register(player -> {
			if (! CONFIG.isEnableMod())
				return;
			ServerLevel world = player.level();
			ServerPlayNetworking.send(player, new WorldInfoPayload(world.getSeed()));
			if (CONFIG.isEnableDebug())
				LOGGER.info("Send info " + world.getSeed() + " to " + player.getName().getString());
		});

		//weather sender
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (! CONFIG.isEnableMod())
				return;
			if (DATA.updateWeather(world)) {
				world.players().forEach(player ->
						ServerPlayNetworking.send(player, new WeatherPayload(DATA.nextWeather))
				);
				if (CONFIG.isEnableDebug())
					LOGGER.info("Broadcast next weather: " + DATA.nextWeather.name());
			}
		});
	}

	public record WorldInfoPayload(long seed) implements CustomPacketPayload {
		public static final Type<WorldInfoPayload> TYPE = new CustomPacketPayload.Type<>(PACKET_WORLD_SEED);
		public static final StreamCodec<FriendlyByteBuf, WorldInfoPayload> CODEC = StreamCodec.of(
				(buf, value) -> buf.writeLong(value.seed),
				buf -> new WorldInfoPayload(buf.readLong())
		);

		@Override
		public @NonNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record WeatherPayload(Data.Weather weather) implements CustomPacketPayload {
		public static final Type<WeatherPayload> TYPE = new CustomPacketPayload.Type<>(PACKET_WEATHER);
		public static final StreamCodec<FriendlyByteBuf, WeatherPayload> CODEC = StreamCodec.of(
				((buf, value) -> buf.writeEnum(value.weather)),
				buf -> new WeatherPayload(buf.readEnum(Data.Weather.class))
		);

		@Override
		public @NonNull Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
