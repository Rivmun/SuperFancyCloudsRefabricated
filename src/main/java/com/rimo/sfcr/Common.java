package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common implements ModInitializer {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier PACKET_WORLD_SEED = Identifier.of(MOD_ID, "world_seed_packet");
	public static final Identifier PACKET_WEATHER = Identifier.of(MOD_ID, "weather_packet");
	public static Config CONFIG = Config.load();
	public static final Data DATA = new Data(CONFIG);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(WorldInfoPayload.ID, WorldInfoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WeatherPayload.ID, WeatherPayload.CODEC);

		//world info sender
		ServerPlayerEvents.JOIN.register(player -> {
			if (! CONFIG.isEnableMod())
				return;
			ServerWorld world = player.getEntityWorld();
			ServerPlayNetworking.send(player, new WorldInfoPayload(world.getSeed()));
			if (CONFIG.isEnableDebug())
				LOGGER.info("Send info " + world.getSeed() + " to " + player.getName().getString());
		});

		//weather sender
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (! CONFIG.isEnableMod())
				return;
			if (DATA.updateWeather(world)) {
				world.getPlayers().forEach(player ->
						ServerPlayNetworking.send(player, new WeatherPayload(DATA.nextWeather))
				);
				if (CONFIG.isEnableDebug())
					LOGGER.info("Broadcast next weather: " + DATA.nextWeather.name());
			}
		});
	}

	public record WorldInfoPayload(long seed) implements CustomPayload {
		public static final Id<WorldInfoPayload> ID = new CustomPayload.Id<>(PACKET_WORLD_SEED);
		public static final PacketCodec<PacketByteBuf, WorldInfoPayload> CODEC = PacketCodec.of(
				(value, buf) -> buf.writeLong(value.seed),
				buf -> new WorldInfoPayload(buf.readLong())
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record WeatherPayload(Data.Weather weather) implements CustomPayload {
		public static final Id<WeatherPayload> ID = new CustomPayload.Id<>(PACKET_WEATHER);
		public static final PacketCodec<PacketByteBuf, WeatherPayload> CODEC = PacketCodec.of(
				((value, buf) -> buf.writeEnumConstant(value.weather)),
				buf -> new WeatherPayload(buf.readEnumConstant(Data.Weather.class))
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}
