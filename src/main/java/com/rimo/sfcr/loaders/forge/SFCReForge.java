//? if forge {
/*package com.rimo.sfcr.loaders.forge;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
//~ if ! 1.16.5 'me.shedaniel.' -> 'dev.'
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//? if = 1.16.5 {
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
//? } else {
/^import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.network.NetworkConstants;
^///? }
//? if = 1.18.2 {
/^import net.minecraftforge.client.ConfigGuiHandler;
^///? } else if > 1.19 {
/^import net.minecraftforge.client.ConfigScreenHandler;
^///? }

@Mod(Common.MOD_ID)
public class SFCReForge {
	public SFCReForge() {
		// Submit our event bus to let architectury register our content on the right time
		EventBuses.registerModEventBus(Common.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

		Common.init();
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Client::init);
		DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> DedicatedServer::init);

	//? if ! 1.16.5 {
		/^if (ModList.get().isLoaded("cloth_config")) {
			ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
			DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> SFCReForge::registerModsPage);
		}
	}

	public static void registerModsPage() {
		//~ if < 1.19 'ConfigScreenHandler.ConfigScreenFactory' -> 'ConfigGuiHandler.ConfigGuiFactory'
		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () -> new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> {
			return new ConfigScreen().build();
		}));
	}
	^///? } else {
		if (ModList.get().isLoaded("cloth-config")) {
			ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
			DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> SFCReForge::registerModsPage);
		}
	}

	public static void registerModsPage() {
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (client, parent) -> {
			return new ConfigScreen().build();
		});
	}
	//? }
}
*///? }
