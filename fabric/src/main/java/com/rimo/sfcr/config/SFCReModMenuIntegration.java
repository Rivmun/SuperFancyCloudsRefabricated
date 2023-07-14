package com.rimo.sfcr.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class SFCReModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
			return parent -> new ConfigScreen().buildScreen();
		} else {
			return parent -> null;
		}
	}
}
