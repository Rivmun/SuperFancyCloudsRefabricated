package com.rimo.sfcr;

import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.core.Data;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier PACKET_WORLD_INFO = new Identifier(Common.MOD_ID, "world_info_s2c");
	public static final Identifier PACKET_WEATHER = new Identifier(Common.MOD_ID, "weather_s2c");
	public static final CommonConfig CONFIG = new CommonConfig().load();
	public static final Data DATA = new Data(CONFIG);

	public static void init() {
		// World Info Sender
		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (!CONFIG.isEnableMod())
				return;
			ServerWorld world = player.getServerWorld();
			NetworkManager.sendToPlayer(
					player,
					PACKET_WORLD_INFO,
					new PacketByteBuf(Unpooled.buffer())
							.writeVarLong(world.getSeed())
			);
			if (CONFIG.isEnableDebug())
				LOGGER.info("Send world info {} to {}", world.getSeed(), player.getName().getString());
		});

		// Weather Sender
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (!CONFIG.isEnableMod())
				return;
			if (DATA.updateWeather(world)) {
				world.getPlayers().forEach(player -> {
					NetworkManager.sendToPlayer(
							player,
							PACKET_WEATHER,
							new PacketByteBuf(Unpooled.buffer())
									.writeEnumConstant(DATA.getNextWeather())
					);
				});
				if (CONFIG.isEnableDebug())
					LOGGER.info("Broadcast next weather: {}", DATA.getNextWeather());
			}
		});

	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		LOGGER.error(e.toString());
		for (StackTraceElement i : e.getStackTrace()) {
			LOGGER.error("{}:{}", i.getClassName(), i.getLineNumber());
		}
	}
}
