package com.rimo.sfcr;

import com.rimo.sfcr.Common.WorldInfoPayload;
import com.rimo.sfcr.Common.WeatherPayload;
import com.rimo.sfcr.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Random;
import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class Client implements ClientModInitializer {
	public static final boolean isDistantHorizonsLoaded = FabricLoader.getInstance().isModLoaded("distanthorizons");
	private static boolean hasServer = false;  //if not, calc density and weather on local; if yes, wait message payload from server.
	public static boolean isCustomDimensionConfig = false;  //indicate current config is default or not.
	public static Renderer RENDERER;

	@Override
	public void onInitializeClient() {
		ResourceLoader.registerBuiltinPack(
				Identifier.fromNamespaceAndPath(Common.MOD_ID, "cloud_shader"),
				FabricLoader.getInstance().getModContainer(Common.MOD_ID).get(),
				Component.translatable("text.sfcr.buildInResourcePack"),
				PackActivationType.ALWAYS_ENABLED
		);

		//init mod
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();
		});

		//init renderer
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!hasServer)
				RENDERER.initSampler(new Random().nextLong());
			RENDERER.setRenderer(CONFIG);
		});

		//update
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (! CONFIG.isEnableMod())
				return;
			if (!hasServer && client.player != null)
				DATA.updateWeatherClient(client.player.level());
			DATA.updateDensity(client.player);
			if (RENDERER instanceof RendererDHCompat renderer && client.player != null)
				renderer.updateDHRenderer();  //manually update when inject to DH instead of mixinned vanilla call.
		});

		//switch config
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			boolean oldEnableMod = CONFIG.isEnableMod();
			boolean oldDHCompat = CONFIG.isEnableDHCompat();
			CONFIG = Config.load(world.dimension().identifier().toString());
			applyConfigChange(oldEnableMod, oldDHCompat);
		});

		//reset
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			hasServer = false;
			RENDERER.stop();
			if (isCustomDimensionConfig)
				CONFIG = Config.load();
		});

		//world info receiver
		ClientPlayNetworking.registerGlobalReceiver(WorldInfoPayload.TYPE, (payload, context) -> {
			hasServer = true;
			RENDERER.initSampler(payload.seed());
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver world info: " + payload.seed());
		});

		//weather receiver
		ClientPlayNetworking.registerGlobalReceiver(WeatherPayload.TYPE, (payload, context) -> {
			DATA.nextWeather = payload.weather();
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver weather: " + payload.weather().name());
		});
	}

	@Environment(EnvType.CLIENT)
	public static void applyConfigChange(boolean oldEnableMod, boolean oldDHCompat) {
		DATA.setConfig(CONFIG);
		if (oldDHCompat != CONFIG.isEnableDHCompat()) {  //convert renderer class
			RENDERER = CONFIG.isEnableDHCompat() ?
					new RendererDHCompat(RENDERER) :
					new Renderer(RENDERER);
		} else {
			RENDERER.setRenderer(CONFIG);
		}
	}

}
