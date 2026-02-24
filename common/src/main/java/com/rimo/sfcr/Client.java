package com.rimo.sfcr;

import com.rimo.sfcr.core.CloudData;
import com.rimo.sfcr.core.Data;
import com.rimo.sfcr.core.Renderer;
import com.rimo.sfcr.core.RendererDHCompat;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

@Environment(EnvType.CLIENT)
public class Client {
	public static final boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	private static boolean hasServer = false;
	public static boolean isCustomDimensionConfig = false;
	public static Renderer RENDERER;

	public static void init() {
		//init mod
		ClientLifecycleEvent.CLIENT_STARTED.register(client -> {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();
		});

		//init renderer
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (!hasServer)
				CloudData.initSampler(new Random().nextLong());
		});

		//update
		ClientTickEvent.CLIENT_POST.register(client -> {
			if (!CONFIG.isEnableMod())
				return;
			if (!hasServer && client.world != null)
				DATA.updateWeatherClient(client.world);
			DATA.updateDensity(client.player);
			//TODO: DHCompat...
		});

		//reset
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			hasServer = false;
			RENDERER.stop();
			CONFIG.load();
		});

		//world info receiver
		NetworkManager.registerReceiver(
				NetworkManager.Side.S2C,
				PACKET_WORLD_INFO,
				(buf, context) -> {
					hasServer = true;
					long seed = buf.readVarLong();
					CloudData.initSampler(seed);
					if (CONFIG.isEnableDebug())
						LOGGER.info("Receive world info: {}", seed);
				}
		);

		//weather receiver
		NetworkManager.registerReceiver(
				NetworkManager.Side.S2C,
				PACKET_WEATHER,
				(buf, context) -> {
					Data.Weather weather = buf.readEnumConstant(Data.Weather.class);
					DATA.setNextWeather(weather);
					if (CONFIG.isEnableDebug())
						LOGGER.info("Receive weather: {}", weather);
				}
		);
	}

	public static void applyConfigChange(boolean oldEnableMod, boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat()) {
			RENDERER = CONFIG.isEnableDHCompat() ?
					new RendererDHCompat(RENDERER) :
					new Renderer(RENDERER);
		}
	}
}
