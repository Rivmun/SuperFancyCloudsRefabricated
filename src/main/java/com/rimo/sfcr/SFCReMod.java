package com.rimo.sfcr;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rimo.sfcr.config.SFCReConfig;

public class SFCReMod implements ClientModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("sfcr");
	
	public static final ConfigHolder<SFCReConfig> CONFIG = AutoConfig.register(SFCReConfig.class, GsonConfigSerializer::new);
	
	public static final SFCReRenderer RENDERER = new SFCReRenderer();;

	@Override
	public void onInitializeClient() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> RENDERER.init());
		ClientTickEvents.START_CLIENT_TICK.register((client) -> RENDERER.tick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RENDERER.clean());
	}
}
