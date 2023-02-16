package com.rimo.sfcr;

import com.rimo.sfcr.register.Command;
import net.fabricmc.api.DedicatedServerModInitializer;

public class SFCReServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		Command.register();
	}

}
