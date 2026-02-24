package com.rimo.sfcr;

import com.rimo.sfcr.config.ConfigScreen;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

@Mod(Common.MOD_ID)
public class SFCReForge {
	public SFCReForge() {
		// Submit our event bus to let architectury register our content on the right time
		EventBuses.registerModEventBus(Common.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

		Common.init();
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Client::init);
		DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> Server::init);

		if (ModList.get().isLoaded("cloth_config")) {
			ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
			DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> SFCReForge::registerModsPage);
		}
	}

	public static void registerModsPage() {
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
			return new ConfigScreen().buildScreen();
		}));
	}
}
