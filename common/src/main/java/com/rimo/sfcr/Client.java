package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.core.CloudData;
import com.rimo.sfcr.core.Data;
import com.rimo.sfcr.core.Renderer;
import com.rimo.sfcr.core.RendererDHCompat;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

@Environment(EnvType.CLIENT)
public class Client {
	public static boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	private static boolean hasServer = false;
	public static boolean isConfigHasBeenOverride = false;
	public static boolean isCustomDimensionConfig = false;
	public static Renderer RENDERER;

	public static void init() {
		// Game boot
		ClientLifecycleEvent.CLIENT_STARTED.register(client -> {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();
		});

		// World loaded
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (! hasServer)
				CloudData.initSampler(new Random().nextLong());  //get a random seed before server send
		});
		ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(world -> {
			String dimensionName = world.getRegistryKey().getValue().toString();
			if (! hasServer || ! CONFIG.isEnableServer()) {
				if (CONFIG.load(dimensionName) && ! dimensionName.equals(Config.OVERWORLD))  //if not sfcr server or disabled server config, read config by client itself.
					isCustomDimensionConfig = true;
				isConfigHasBeenOverride = false;
			}
		});

		// Update data
		ClientTickEvent.CLIENT_POST.register(client -> {
			if (!CONFIG.isEnableRender())
				return;
			if (!hasServer && client.world != null)
				DATA.updateWeatherClient(client.world);
			if (client.player != null)
				DATA.updateDensity(client.player);
		});

		// Quit reset
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			hasServer = false;
			isCustomDimensionConfig = false;
			isConfigHasBeenOverride = false;
			RENDERER.stop();
			CONFIG.load();
			Common.clearConfigCache(null);
		});

		//seed receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_SEED, (buf, context) -> {
			hasServer = true;
			long seed = buf.readVarLong();
			CloudData.initSampler(seed);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive seed {}", MOD_ID, seed);
		});

		//dimension packet receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_DIMENSION, (buf, context) -> {
			String name = buf.readString();
			String configJson = buf.readString();
			if (! configJson.isEmpty() && CONFIG.isEnableServer()) {
				try {
					CONFIG.fromString(configJson);
					if (! MinecraftClient.getInstance().isIntegratedServerRunning())  //singleplayer override itself? ur joking...
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
		});

		//weather receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_WEATHER, (buf, context) -> {
			Data.Weather weather = buf.readEnumConstant(Data.Weather.class);
			DATA.setNextWeather(weather);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} receive weather: {}", MOD_ID, weather);
		});

		//upload request receiver & shared config sender
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_UPLOAD_REQUEST, (buf, context) -> {
			String configJson = CONFIG.toString();
			NetworkManager.sendToServer(PACKET_SHARED_CONFIG, new PacketByteBuf(Unpooled.buffer())
					.writeString(configJson)
			);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send current config to server", MOD_ID);
		});
	}

	@Environment(EnvType.CLIENT)
	public static void applyConfigChange(boolean oldEnableMod, boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat())
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat(RENDERER) : new Renderer(RENDERER);
	}
}
