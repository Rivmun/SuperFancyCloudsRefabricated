package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
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
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

@Environment(EnvType.CLIENT)
public class Client {
	public static final boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	public static final boolean isParticleRainLoaded = Platform.isModLoaded("particlerain");
	public static boolean isIrisLoadedShader = false;
	private static boolean hasServer = false;
	public static boolean isConfigHasBeenOverride = false;
	public static boolean isCustomDimensionConfig = false;
	public static Renderer RENDERER;

	public static void init() {
		// Game boot
		// TODO: Issue that this event (include CLIENT_SETUP) cannot invoke correctly on neoforge, it's causing NPE when quit...
		ClientLifecycleEvent.CLIENT_STARTED.register(client -> {
			if (Platform.isModLoaded("iris"))
				isIrisLoadedShader = IrisApi.getInstance().getConfig().areShadersEnabled();
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();
		});

		// World loaded
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (! hasServer)
				RENDERER.initSampler(new Random().nextLong());  //get a random seed before server send
			RENDERER.setRenderer(CONFIG);
		});
		ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(world -> {
			String dimensionName = world.dimension().identifier().toString();
			if (! hasServer || ! CONFIG.isEnableServer()) {  //if not sfcr server or disabled server config, read config by client itself.
				boolean oldEnableDHCompat = CONFIG.isEnableDHCompat();
				if (CONFIG.load(dimensionName) && ! dimensionName.equals(Config.OVERWORLD))
					isCustomDimensionConfig = true;
				isConfigHasBeenOverride = false;
				applyConfigChange(oldEnableDHCompat);
			}
		});

		// Update data
		ClientTickEvent.CLIENT_POST.register(client -> {
			if (! CONFIG.isEnableRender())
				return;
			if (client.level != null && client.level.getGameTime() % 20 == 0) {
				if (! hasServer && ! client.isLocalServer())
					DATA.updateWeatherClient(client.level);
				if (client.player != null)
					DATA.updateDensity(client.player);
			}
			if (RENDERER instanceof RendererDHCompat renderer) {
				renderer.updateDHRenderer();  //manually update when inject to DH instead of mixinned vanilla call.
			}
		});

		// Quit reset
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			hasServer = false;
			isCustomDimensionConfig = false;
			isConfigHasBeenOverride = false;
			if (RENDERER != null)
				RENDERER.stop();
			CONFIG.load();
			Common.clearConfigCache(null);
		});

		//seed receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, SeedPayload.TYPE, SeedPayload.CODEC, (payload, context) -> {
			hasServer = true;
			long seed = payload.seed();
			RENDERER.initSampler(seed);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive seed {}", MOD_ID, seed);
		});

		//dimension packet receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, DimensionPayload.TYPE, DimensionPayload.CODEC, (payload, context) -> {
			String name = payload.name();
			String configJson = payload.sharedConfigJson();
			if (! configJson.isEmpty() && CONFIG.isEnableServer()) {
				try {
					CONFIG.fromString(configJson);
					if (! Minecraft.getInstance().isLocalServer())  //singleplayer override itself? ur joking...
						isConfigHasBeenOverride = true;
					if (! name.equals(Config.OVERWORLD))
						isCustomDimensionConfig = true;
					if (CONFIG.isEnableDebug())
						LOGGER.info("{} receive sharedConfig of '{}'", MOD_ID, name);
				} catch (JsonSyntaxException e) {
					LOGGER.error("{} cannot read config for {} which is received from server, please check your mod version!", MOD_ID, name);
				}
			} else {
				if (CONFIG.load(name) && ! name.equals(Config.OVERWORLD))  //Client trying to load dimension config if server not send...
					isCustomDimensionConfig = true;
				isConfigHasBeenOverride = false;
				if (CONFIG.isEnableDebug())
					LOGGER.info("{} receive dimension name '{}'", MOD_ID, name);
			}
			RENDERER.setRenderer(CONFIG);
		});

		//weather receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, WeatherPayload.TYPE, WeatherPayload.CODEC, (payload, context) -> {
			Data.Weather weather = payload.weather();
			DATA.setNextWeather(weather);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive weather: {}", MOD_ID, weather);
		});

		//upload request receiver & shared config sender
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, UploadRequestPayload.TYPE, UploadRequestPayload.CODEC, (buf, context) -> {
			String name = context.getPlayer().level().dimension().identifier().toString();
			String configJson = CONFIG.toString();
			NetworkManager.sendToServer(new DimensionPayload(name, configJson));
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send current config to server", MOD_ID);
		});
	}

	public static void applyConfigChange(boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat()) {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat(RENDERER) : new Renderer(RENDERER);
		} else {
			RENDERER.setRenderer(CONFIG);
		}
	}

	/**
	 * @return true if this point is covered by SFC clouds, false if not.<br>
	 * Note that if this point is above cloud, it always returns false.
	 */
	public static boolean isNoCloudCovered(double x, double y, double z) {
		return RENDERER == null || ! RENDERER.isCloudCovered(x, y, z);
	}
}
