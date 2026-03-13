package com.rimo.sfcr;

import com.rimo.sfcr.Common.SeedPayload;
import com.rimo.sfcr.Common.WeatherPayload;
import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.ConfigScreenYACL;
import com.rimo.sfcr.core.Renderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Random;

import static com.rimo.sfcr.Common.*;

public class Client implements ClientModInitializer {
	private static boolean hasServer = false;  //if not, calc density and weather on local; if yes, wait message payload from server.
	public static boolean isCustomDimensionConfig = false;  //indicate current config is default or not.
	public static Renderer RENDERER;

	@Override
	public void onInitializeClient() {
		//init mod
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			RENDERER = new Renderer();
		});

		//init renderer
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!hasServer)
				RENDERER.initSampler(new Random().nextLong());
			RENDERER.setRenderer(CONFIG);
		});

		//update
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (! CONFIG.isEnableRender())
				return;
			if (!hasServer && client.player != null)
				DATA.updateWeatherClient(client.player.level());
			DATA.updateDensity(client.player);
		});

		//switch config
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
			CONFIG = Config.load(world.dimension().identifier().toString());
			applyConfigChange();
		});

		//reset
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			hasServer = false;
			RENDERER.stop();
			if (isCustomDimensionConfig)
				CONFIG = Config.load();
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher
				.register(ClientCommands.literal(MOD_ID).executes(context -> {
					if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
						Minecraft.getInstance().schedule(() ->
								Minecraft.getInstance().setScreen(new ConfigScreenYACL().buildScreen(null))
						);
						return 1;
					}
					context.getSource().sendFeedback(Component.literal("§4You need to install YACL first."));
					return 1;
				}))
		);

		//world info receiver
		ClientPlayNetworking.registerGlobalReceiver(SeedPayload.TYPE, (payload, context) -> {
			hasServer = true;
			RENDERER.initSampler(payload.seed());
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver world info: {}", payload.seed());
		});

		//weather receiver
		ClientPlayNetworking.registerGlobalReceiver(WeatherPayload.TYPE, (payload, context) -> {
			DATA.nextWeather = payload.weather();
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver weather: {}", payload.weather().name());
		});
	}

	@Environment(EnvType.CLIENT)
	public static void applyConfigChange() {
		DATA.setConfig(CONFIG);
		RENDERER.setRenderer(CONFIG);
	}

	/**
	 * @return true if this point is covered by SFC clouds, false if not.<br>
	 * Note that if this point is above cloud, it always returns false.
	 */
	public static boolean isNoCloudCovered(double x, double y, double z) {
		return RENDERER == null || ! RENDERER.isCloudCovered(x, y, z);
	}

}
