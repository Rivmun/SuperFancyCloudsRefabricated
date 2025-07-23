package com.rimo.sfcr;

import com.rimo.sfcr.Common.WorldInfoPayload;
import com.rimo.sfcr.Common.WeatherPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Random;
import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class Client implements ClientModInitializer {
	public static Renderer RENDERER;
	public static final Identifier buildInPackId = Identifier.of(Common.MOD_ID, "cloud_shader");

	private static boolean hasServer = false;

	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.registerBuiltinResourcePack(
				buildInPackId,
				FabricLoader.getInstance().getModContainer(Common.MOD_ID).get(),
				Text.translatable("text.sfcr.buildInResourcePack"),
				CONFIG.isEnableMod() ?
						ResourcePackActivationType.DEFAULT_ENABLED :
						ResourcePackActivationType.NORMAL
		);

		//init mod
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			RENDERER = new Renderer();
			if (! CONFIG.isEnableMod())
				/*
					we already set activationType to 'normal' when mod is disabled, to disable build-in resource pack when client start up, it does.
					but this pack still on right in resource manager screen, so here we disable it again to put it to left.
				 */
				client.getResourcePackManager().disable(buildInPackId.toString());
		});

		//init renderer
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!hasServer)
				RENDERER.setSampler(new Random().nextLong());
			RENDERER.setRenderer(CONFIG);
		});

		//update
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (! CONFIG.isEnableMod())
				return;
			if (!hasServer && client.player != null)
				DATA.updateWeatherClient(client.player.getWorld());
			DATA.updateDensity(client.player);
		});

		//reset
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			hasServer = false;
			RENDERER.stop();
		});

		//world info receiver
		ClientPlayNetworking.registerGlobalReceiver(WorldInfoPayload.ID, (payload, context) -> {
			hasServer = true;
			RENDERER.setSampler(payload.seed());
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver world info: " + payload.seed());
		});

		//weather receiver
		ClientPlayNetworking.registerGlobalReceiver(WeatherPayload.ID, (payload, context) -> {
			DATA.nextWeather = payload.weather();
			if (CONFIG.isEnableDebug())
				Common.LOGGER.info("Receiver weather: " + payload.weather().name());
		});
	}
}
