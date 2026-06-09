//? if neoforge {
/*package com.rimo.sfcr.loaders.neoforge;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;

import static com.rimo.sfcr.Common.LOGGER;
import static com.rimo.sfcr.Common.MOD_ID;

@Mod(MOD_ID)
public class SFCReNeoForge {
	public SFCReNeoForge(IEventBus bus) {
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
		@SubscribeEvent
		// arch-api cannot register same payload on both side in neoforge-1.21.1, so we can only register in here.
		public static void registerPayload(RegisterPayloadHandlersEvent event) {
			event.registrar("1").commonBidirectional(
					Common.DimensionPayload.TYPE,
					Common.DimensionPayload.CODEC,
					new DirectionalPayloadHandler<>(
							(payload, context) ->
									Client.handleDimensionPayload(payload.name(), payload.sharedConfigJson(), payload.seed()),
							(payload, context) -> {}
					)
			);
			LOGGER.info("succ reg dimension payload on client side.");
		}
	}

	@OnlyIn(Dist.DEDICATED_SERVER)
	@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.DEDICATED_SERVER)
	public static class ServerInit {
		@SubscribeEvent
		public static void serverInit(FMLDedicatedServerSetupEvent event) {
			DedicatedServer.init();
		}
		@SubscribeEvent
		public static void registerPayload(RegisterPayloadHandlersEvent event) {
			event.registrar("1").commonBidirectional(
					Common.DimensionPayload.TYPE,
					Common.DimensionPayload.CODEC,
					new DirectionalPayloadHandler<>(
							(payload, context) -> {},
							(payload, context) ->
									DedicatedServer.handleDimensionPayload(payload.name(), payload.sharedConfigJson(), context.player())
					)
			);
			LOGGER.info("succ reg dimension payload on server side.");
		}
	}
}
*///? }
