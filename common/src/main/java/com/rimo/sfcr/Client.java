package com.rimo.sfcr;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.ConfigScreen;
import com.rimo.sfcr.core.*;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
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
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

@Environment(EnvType.CLIENT)
public class Client {
	public static final boolean isDistantHorizonsLoaded = Platform.isModLoaded("distanthorizons");
	public static final boolean isParticleRainLoaded = Platform.isModLoaded("particlerain");
	public static AbstractSeasonCompat seasonHandler = AbstractSeasonCompat.getInstance(CONFIG);
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
			if (! hasServer || ! CONFIG.isEnableServer()) {  //if not sfcr server or disabled server config, read config by client itself.
				if (CONFIG.load(dimensionName) && ! dimensionName.equals(Config.OVERWORLD))
					isCustomDimensionConfig = true;
				isConfigHasBeenOverride = false;
			}
			if (seasonHandler != null)
				DATA.setDensityBySeason(seasonHandler.getSeasonDensityPercent(world));
		});

		// Update data
		ClientTickEvent.CLIENT_POST.register(client -> {
			if (! CONFIG.isEnableRender() || client.world == null || client.world.getTime() % 20 != 0)
				return;
			if (! hasServer && ! client.isIntegratedServerRunning())
				DATA.updateWeatherClient(client.world);
			if (client.player != null)
				DATA.updateDensity(client.player);
		});
		if (seasonHandler != null)
			seasonHandler.registerListener(DATA::setDensityBySeason);

		// Quit reset
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			hasServer = false;
			isCustomDimensionConfig = false;
			isConfigHasBeenOverride = false;
			RENDERER.stop();
			CONFIG.load();
			Common.clearConfigCache(null);
		});

		ClientCommandRegistrationEvent.EVENT.register((dispatcher, dedicated) -> dispatcher
				.register(ClientCommandRegistrationEvent.literal(MOD_ID).executes(context -> {
					if (Platform.isFabric() && Platform.isModLoaded("cloth-config2") || Platform.isForge() && Platform.isModLoaded("cloth_config")) {
						MinecraftClient client = MinecraftClient.getInstance();
						client.execute(() -> client.setScreen(new ConfigScreen().build()));
					} else {
						context.getSource().arch$sendFailure(Text.translatable("text.sfcr.requiredCloth"));
					}
					return 1;
				}))
		);

		//dimension packet receiver
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_DIMENSION, (buf, context) -> {
			hasServer = true;
			String name = buf.readString();
			String configJson = buf.readString();
			long seed = buf.readVarLong();
			CloudData.initSampler(seed);
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
			World world = MinecraftClient.getInstance().world;
			if (world == null)
				return;
			String name = world.getRegistryKey().getValue().toString();
			String configJson = CONFIG.toString();
			NetworkManager.sendToServer(PACKET_DIMENSION, new PacketByteBuf(Unpooled.buffer())
					.writeString(name)
					.writeString(configJson)
					.writeVarLong(0L)
			);
			if (CONFIG.isEnableDebug())
				LOGGER.info("{} send current config to server", MOD_ID);
		});
	}

	public static void applyConfigChange(boolean oldEnableDHCompat) {
		if (oldEnableDHCompat != CONFIG.isEnableDHCompat())
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat(RENDERER) : new Renderer(RENDERER);
	}

	/**
	 * @return true if this point is covered by SFC clouds, false if not.<br>
	 * Note that if this point is above cloud, it always returns false.
	 */
	public static boolean isNoCloudCovered(double x, double y, double z) {
		return RENDERER == null || ! RENDERER.isCloudCovered(x, y, z);
	}
}
