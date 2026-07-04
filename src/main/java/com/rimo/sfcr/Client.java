package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.ConfigScreen;
import com.rimo.sfcr.core.*;
//~ if < 1.18 'dev.architectury' -> 'me.shedaniel.architectury' {
//? if ! 1.16.5
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
//~ }
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//~ if < 1.19 'Component' -> 'TranslatableComponent'
import net.minecraft.network.chat.Component;
//? if neoforge
//import net.neoforged.neoforge.network.PacketDistributor;
//? if < 1.21 {
/*import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///? }

import java.util.Random;

import static com.rimo.sfcr.Common.*;

public class Client {
	public static final boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	public static final boolean isParticleRainLoaded = Platform.isModLoaded("particlerain");
	private static boolean hasServer = false;
	public static boolean isConfigHasBeenOverride = false;
	public static boolean isCustomDimensionConfig = false;
	public static Renderer RENDERER;
	private static String debugString = "";

	public static void init() {
		// Game boot
		// arch api 's SETUP event is not reliable, RENDERER init we directly placed here
		RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();

		// World loaded
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (! hasServer)
				CloudData.sampler.setSeed(new Random().nextLong());  //get a random seed before server send
		});
		//~ if = 1.16.5 'CLIENT_LEVEL_LOAD' -> 'CLIENT_WORLD_LOAD'
		ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> {
			Sampler sampler = CloudData.sampler.setLevel(level);
			String dimensionName = level.dimension().location().toString();
			if (! hasServer || ! CONFIG.isEnableServer()) {  //if not sfcr server or disabled server config, read config by client itself.
				if (CONFIG.load(dimensionName))
					isCustomDimensionConfig = true;
				isConfigHasBeenOverride = false;
				sampler.setConfig(CONFIG);
			}
			if (seasonHandler != null)
				CloudData.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(level));
		});

		// Update data
		ClientTickEvent.CLIENT_POST.register(client -> {
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
				CloudData.sampler.setDensityBySeason(seasonHandler.getSeasonDensityPercent(level));
			updateDebugString();
		});

		// Quit reset
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			if (player == null)
				return;  // prevent those code runs when join a world?
			hasServer = false;
			isCustomDimensionConfig = false;
			isConfigHasBeenOverride = false;
			if (RENDERER != null)
				RENDERER.stop();
			CONFIG.load();
			clearDimensionCache();
		});

		//? if ! 1.16.5 {
		// client command for configScreen
		//~ if > 1.19 '(dispatcher)' -> '(dispatcher, dedicated)'
		ClientCommandRegistrationEvent.EVENT.register((dispatcher, dedicated) -> dispatcher
				// Don't use only 'sfcr' that will override server command when connect to a server on fabric-1.21.1
				.register(ClientCommandRegistrationEvent.literal(MOD_ID + "config").executes(context -> {
					//~ if > 1.21 '.isForge()' -> '.isNeoForge()'
					if (Platform.isFabric() && Platform.isModLoaded("cloth-config2") || Platform.isNeoForge() && Platform.isModLoaded("cloth_config")) {
						Minecraft client = Minecraft.getInstance();
						//~ if > 1.18 && < 1.20 || > 1.20 && fabric 'client.execute' -> 'client.tell'
						client.tell(() -> client.setScreen(new ConfigScreen().build()));
					} else {
						//~ if < 1.19 'Component.translatable' -> 'new TranslatableComponent'
						context.getSource().arch$sendFailure(Component.translatable("text.sfcr.requiredCloth"));
					}
					return 1;
				}))
		);
		//? }

		//dimension packet receiver
		/*
		 * arch-api cannot register dimension payload on both side in neoforge-1.21.1, we're register it through neo original event hook.
		 * in that way the arch-system doesn't have dimension payload identifier that we cannot receive this payload through arch, so those code is useless.
		 */
		//? if !neoforge {
		//? if < 1.21 {
		/*NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_DIMENSION, (buf, context) -> {
			String name = buf.readUtf();
			String configJson = buf.readUtf();
			long seed = buf.readVarLong();
		*///? } else {
		// register c2s codec into arch-api...
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, DimensionPayload.TYPE, DimensionPayload.CODEC, (payload, context) -> {});
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, DimensionPayload.TYPE, DimensionPayload.CODEC, (payload, context) -> {
			String name = payload.name();
			String configJson = payload.sharedConfigJson();
			long seed = payload.seed();
		//? }
			handleDimensionPayload(name, configJson, seed);
		});
		//? }

		//weather receiver
		//? if < 1.21 {
		/*NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_WEATHER, (buf, context) -> {
			Data.Weather weather = buf.readEnum(Data.Weather.class);
		*///? } else {
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, WeatherPayload.TYPE, WeatherPayload.CODEC, (payload, context) -> {
			Data.Weather weather = payload.weather();
		//? }
			DATA.setNextWeather(weather);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive weather: {}", MOD_ID, weather);
		});

		//upload request receiver & shared config sender
		//? if < 1.21 {
		/*NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_UPLOAD_REQUEST, (buf, context) -> {
		*///? } else
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, UploadRequestPayload.TYPE, UploadRequestPayload.CODEC, (payload, context) -> {
			Level level = Minecraft.getInstance().level;
			if (level == null)
				return;
			String name = level.dimension().location().toString();
			String configJson = CONFIG.toString();
			//? if < 1.21 {
			/*NetworkManager.sendToServer(PACKET_DIMENSION, new FriendlyByteBuf(Unpooled.buffer())
					.writeUtf(name)
					.writeUtf(configJson)
					.writeVarLong(0L)
			);
			*///? } else {
			//~ if neoforge 'NetworkManager.' -> 'PacketDistributor.'
			NetworkManager.sendToServer(new DimensionPayload(
					name,
					configJson,
					0L
			));
			//? }
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send current config to server", MOD_ID);
		});
	}

	public static void handleDimensionPayload(String name, String configJson, long seed) {
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
		CloudData.sampler.setSeed(seed).setConfig(CONFIG);
	}

	public static void applyConfigChange(boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat()) {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat(RENDERER) : new Renderer(RENDERER);
		} else if (! CONFIG.isEnableRender()) {
			RENDERER.stop();
		}
		CloudData.sampler.setConfig(CONFIG);
	}

	/**
	 * For render (client) side, use {@link CloudData#isCloudCovered(double, double, double)} to calculate where is no cloud above<br>
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

	/**
	 * @see #isNoCloudCovered(double, double, double)
	 * @return {@code true} if this point has cloudBlock.
	 */
	public static boolean isCloud(double x, double y, double z) {
		if (! CONFIG.isEnableRender() || RENDERER == null)
			return false;
		return RENDERER.isCloud(x, y, z);
	}

	private static void updateDebugString() {
		if (! Minecraft.getInstance().isLocalServer()) {
			debugString = "[SFCR] pos debug not available in remote game.";
			return;
		}
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;
		Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		boolean debugIsCloud = Common.isCloud(level, pos.x, pos.y, pos.z);
		boolean debugIsCloudClient = Client.isCloud(pos.x, pos.y, pos.z);
		debugString = "[SFCR] isCloud:" + debugIsCloud + ", isCloudClient:" + debugIsCloudClient;
	}

	public static String getDebugString() {
		return debugString;
	}
}
