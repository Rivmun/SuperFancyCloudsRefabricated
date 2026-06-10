package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.core.*;
//~ if neoforge 'fabric' -> 'neoforge'
import com.rimo.sfcr.loaders.fabric.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

public class Client {
	public static final boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	public static final boolean isParticleRainLoaded = Platform.isModLoaded("particlerain");
	public static boolean isIrisLoadedShader = false;
	private static boolean hasServer = false;
	public static boolean isConfigHasBeenOverride = false;
	public static boolean isCustomDimensionConfig = false;
	public static Renderer RENDERER;

	public static void init() {
		RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();
	}

	public static void onLevelLoad(Level level) {
		Sampler sampler = Renderer.sampler.setLevel(level).setSeed(new Random().nextLong());  //get a random seed before server send
		String dimensionName = level.dimension().identifier().toString();
		if (! hasServer || ! CONFIG.isEnableServer()) {  //if not sfcr server or disabled server config, read config by client itself.
			if (CONFIG.load(dimensionName))
				isCustomDimensionConfig = true;
			isConfigHasBeenOverride = false;
			sampler.setConfig(CONFIG);
		}
		if (seasonHandler != null)
			Renderer.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(level));
	}

	public static void onTick(Minecraft client) {
		ClientLevel level = client.level;
		if (! CONFIG.isEnableRender() || level == null || level.getGameTime() % 20 != 0)
			return;
		if (! client.isLocalServer()) {
			if (! hasServer)
				DATA.updateWeatherClient(level);
			DATA.updateWeatherDensity(level);
		}
		if (client.player != null)
			DATA.updateBiomeDensity(client.player);
		if (seasonHandler != null && level.getGameTime() % 24000 == 0)
			Renderer.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(level));
	}

	public static void onQuit(@Nullable LocalPlayer player) {
		if (player == null)
			return;
		hasServer = false;
		isCustomDimensionConfig = false;
		isConfigHasBeenOverride = false;
		if (RENDERER != null)
			RENDERER.stop();
		CONFIG.load();
		clearDimensionCache();
	}

	public static void handleDimensionPayload(DimensionPayload payload) {
		String name = payload.name();
		String configJson = payload.sharedConfigJson();
		long seed = payload.seed();
		hasServer = true;
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
			if (CONFIG.load(name))  //Client trying to load dimension config if server not send...
				isCustomDimensionConfig = true;
			isConfigHasBeenOverride = false;
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive dimension name '{}'", MOD_ID, name);
		}
		Renderer.sampler.setSeed(seed).setConfig(CONFIG);
	}

	public static void handleWeatherPayload(WeatherPayload payload) {
		Data.Weather weather = payload.weather();
		DATA.setNextWeather(weather);
		if (CONFIG.isEnableDebug())
			LOGGER.info("{} receive weather: {}", MOD_ID, weather);
	}

	//upload request receiver & shared config sender
	public static void handleUploadRequestPayload() {
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;
		String name = level.dimension().identifier().toString();
		String configJson = CONFIG.toString();
		Platform.sendToServer(new DimensionPayload(
				name,
				configJson,
				0L
		));
		if (CONFIG.isEnableDebug())
			LOGGER.info("{} send current config to server", MOD_ID);
	}

	public static void applyConfigChange(boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat()) {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat(RENDERER) : new Renderer(RENDERER);
		} else if (! CONFIG.isEnableRender()) {
			RENDERER.stop();
		}
		Renderer.sampler.setConfig(CONFIG);
	}

	/**
	 * For render (client) side, use {@link Renderer#isCloudCovered(double, double, double)} to calculate where is no cloud above<br>
	 * Call from logical side is useless and may crash, {@link Common#isNoCloudCovered(Level, double, double, double)} is recommended.
	 * @param x Components of target Level pos
	 * @param y Components of target Level pos
	 * @param z Components of target Level pos
	 * @return {@code true} if this point is covered by SFC clouds, {@code false} if not.<br>
	 * Note that if this point is above cloud, or NCNR function is disabled, it always {@code false}.
	 */
	public static boolean isNoCloudCovered(double x, double y, double z) {
		if (! CONFIG.isEnableCloudRain() || RENDERER == null )
			return false;
		return ! RENDERER.isCloudCovered(x, y, z);
	}
}
