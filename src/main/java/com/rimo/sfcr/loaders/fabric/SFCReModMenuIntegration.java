//? if fabric {
package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.config.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class SFCReModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
			return parent -> new ConfigScreen().build();
		} else {
			return parent -> null;
		}
	}
}
//? }
