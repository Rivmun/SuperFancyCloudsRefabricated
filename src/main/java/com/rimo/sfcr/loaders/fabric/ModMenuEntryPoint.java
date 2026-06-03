package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.config.ConfigScreenYACL;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuEntryPoint implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
			return parent -> new ConfigScreenYACL().buildScreen(parent);
		} else {
			return parent -> null;
		}
	}
}
