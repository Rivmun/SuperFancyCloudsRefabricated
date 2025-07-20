package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Client implements ClientModInitializer {
	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = Config.load();
	public static Data DATA;
	public static Renderer RENDERER;

	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			DATA = new Data(CONFIG);
			RENDERER = new Renderer(CONFIG, DATA);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			RENDERER.setSampler(client.player.getWorld() instanceof ServerWorld world ?
					world.getSeed() :
					new Random().nextLong()
			);
			RENDERER.setRenderer(CONFIG);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			DATA.updateWeather(client);
			DATA.updateDensity(client);
		});

		ResourceManagerHelper.registerBuiltinResourcePack(
				Identifier.of(MOD_ID, "cloud_shader"),
				FabricLoader.getInstance().getModContainer(MOD_ID).get(),
				Text.translatable("text.sfcr.buildInResourcePack"),
				ResourcePackActivationType.ALWAYS_ENABLED
		);
	}
}
