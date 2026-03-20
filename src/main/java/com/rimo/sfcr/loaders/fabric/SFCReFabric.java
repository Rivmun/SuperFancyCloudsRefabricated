//? if fabric {
package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

public class SFCReFabric implements ModInitializer, ClientModInitializer, DedicatedServerModInitializer {
	@Override
	public void onInitialize() {
		Common.init();
	}

	@Override
	public void onInitializeClient() {
		Client.init();
	}

	@Override
	public void onInitializeServer() {
		DedicatedServer.init();
	}
}
//? }
