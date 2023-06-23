package com.rimo.sfcr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;

public class SFCReFabric implements ModInitializer, ClientModInitializer, DedicatedServerModInitializer {
	@Override
	public void onInitialize() {
		SFCReMod.init();
	}

	@Override
	public void onInitializeClient() {
		SFCReMod.initClient();
	}

	@Override
	public void onInitializeServer() {
		SFCReMod.initServer();
	}
}
