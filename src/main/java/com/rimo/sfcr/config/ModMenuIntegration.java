package com.rimo.sfcr.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("yet_another_config_lib_v3"))
			return parent -> new ConfigScreenYACL().buildScreen(parent);
		return null;
	}
}
