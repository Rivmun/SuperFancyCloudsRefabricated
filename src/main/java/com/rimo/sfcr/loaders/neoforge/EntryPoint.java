//? if neoforge {
/*package com.rimo.sfcr.loaders.neoforge;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
import com.rimo.sfcr.core.Renderer;
import com.rimo.sfcr.core.RendererDHCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import static com.rimo.sfcr.Common.MOD_ID;

@Mod(MOD_ID)
public class EntryPoint {
	public EntryPoint(IEventBus bus) {
		Common.init();
	}

	@OnlyIn(Dist.CLIENT)
	@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
	public static class ClientInit {
		@SubscribeEvent
		public static void clientInit(FMLClientSetupEvent event) {
			Client.init();

			ModList modList = ModList.get();
			if (modList.isLoaded("cloth_config")) {
				modList.getModContainerById(Common.MOD_ID).ifPresent(container ->
						container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parentScreen) ->
								new ConfigScreen().build()
						)
				);
			}
		}
	}

	@OnlyIn(Dist.DEDICATED_SERVER)
	@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.DEDICATED_SERVER)
	public static class ServerInit {
		@SubscribeEvent
		public static void serverInit(FMLDedicatedServerSetupEvent event) {
			DedicatedServer.init();
		}
	}
}
*///? }
