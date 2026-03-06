package com.rimo.sfcr;

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

import static com.rimo.sfcr.Common.*;

@Mod(MOD_ID)
public class SFCReNeoforge {
	public SFCReNeoforge(IEventBus bus) {
		Common.init();
	}

	@OnlyIn(Dist.CLIENT)
	@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
	public static class ClientInit {
		@SubscribeEvent
		public static void clientInit(FMLClientSetupEvent event) {
			Client.init();
			// TODO: Issue that ArchAPI's LifeCircleEvent.CLIENT_STARTED cannot invoke correctly, so we manually init Renderer in here temporally.
			Client.RENDERER = CONFIG.isEnableDHCompat() ? new RendererDHCompat() : new Renderer();

			ModList modList = ModList.get();
			if (modList.isLoaded("cloth_config")) {
				modList.getModContainerById(MOD_ID).ifPresent(container ->
						container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parentScreen) ->
								new ConfigScreen().build()
						)
				);
			}
		}
	}

	@OnlyIn(Dist.DEDICATED_SERVER)
	@EventBusSubscriber(modid = MOD_ID, value = Dist.DEDICATED_SERVER)
	public static class ServerInit {
		@SubscribeEvent
		public static void serverInit(FMLDedicatedServerSetupEvent event) {
			Server.init();
		}
	}
}
