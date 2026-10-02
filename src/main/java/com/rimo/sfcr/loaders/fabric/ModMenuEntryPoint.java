//? if fabric {
package com.rimo.sfcr.loaders.fabric;

import com.rimo.sfcr.config.ConfigScreen;
import com.rimo.sfcr.config.MissingConfigLibScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuEntryPoint implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> FabricLoader.getInstance().isModLoaded("cloth-config2") ?
			new ConfigScreen().build(parent) :
			new MissingConfigLibScreen(parent);
	}
}
//? }
