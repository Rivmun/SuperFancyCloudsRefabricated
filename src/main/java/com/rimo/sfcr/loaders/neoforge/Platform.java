//? if neoforge {
/*package com.rimo.sfcr.loaders.neoforge;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.DedicatedServer;
import com.rimo.sfcr.config.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.file.Path;

@Mod(Common.MOD_ID)
@EventBusSubscriber(modid = Common.MOD_ID)
public class Platform {
	@SubscribeEvent
	public static void registerPayloadType(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.commonToClient(Common.WeatherPayload.TYPE, Common.WeatherPayload.CODEC);
		registrar.commonToClient(Common.UploadRequestPayload.TYPE, Common.UploadRequestPayload.CODEC);
	}
	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel)  //this event invoked both on client/server, we must ensure it only runs on server
			Common.addDimensionData(serverLevel);
	}
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel serverLevel)
			Common.removeDimensionData(serverLevel);
	}
	@SubscribeEvent
	public static void onTick(ServerTickEvent.Post event) {
		Common.onTick(event.getServer());
	}
	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel serverLevel)
			Common.onLevelTick(serverLevel);
	}
	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Common.onPlayerJoin((ServerPlayer) event.getEntity());
	}
	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			Common.onPlayerChangedDimension(player, event.getTo());
	}
	@SubscribeEvent
	public static void onQuit(PlayerEvent.PlayerLoggedOutEvent event) {
		Common.onPlayerQuit((ServerPlayer) event.getEntity());
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
		public static void registerCommand(RegisterClientCommandsEvent event) {
			if (! ModList.get().isLoaded("cloth_config"))
				return;
			event.getDispatcher().register(Commands.literal(Common.MOD_ID + "config").executes(context -> {
				Minecraft client = Minecraft.getInstance();
				client.execute(() -> client.setScreen(new ConfigScreen().build()));
				return 1;
			}));
		}
		@SubscribeEvent
		public static void registerPayload(RegisterPayloadHandlersEvent event) {
			event.registrar("1").commonBidirectional(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC,
					(payload, context) -> {},  // Do nothing on toServer packet.
					(payload, context) -> Client.handleDimensionPayload(payload)
			);
		}
		@SubscribeEvent
		public static void registerPayloadHandler(RegisterClientPayloadHandlersEvent event) {
			event.register(Common.WeatherPayload.TYPE, (payload, context) -> Client.handleWeatherPayload(payload));
			event.register(Common.UploadRequestPayload.TYPE, (payload, context) -> Client.handleUploadRequestPayload());
		}
		@SubscribeEvent
		public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
			Client.onLevelLoad(event.getPlayer().level());
		}
		@SubscribeEvent
		public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			Client.onLevelLoad(event.getEntity().level());
		}
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Client.onTick(Minecraft.getInstance());
		}
		@SubscribeEvent
		public static void onQuit(ClientPlayerNetworkEvent.LoggingOut event) {
			Client.onQuit(event.getPlayer());
		}
	}

	@OnlyIn(Dist.DEDICATED_SERVER)
	@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.DEDICATED_SERVER)
	public static class ServerInit {
		@SubscribeEvent
		public static void registerNetwork(RegisterPayloadHandlersEvent event) {
			event.registrar("1").commonBidirectional(Common.DimensionPayload.TYPE, Common.DimensionPayload.CODEC,
					(payload, context) -> DedicatedServer.handleDimensionPayload(payload, context.player()),
					(payload, context) -> {}  // Wants to handler a toClient packet on DedicatedServer? ur joking...
			);
		}
		@SubscribeEvent
		public static void registerCommand(RegisterCommandsEvent event) {
			DedicatedServer.registerCommand(event.getDispatcher());
		}
	}

	// - - - - - Platform specific function - - - - -
	public static boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return player.connection.hasChannel(type);
	}
	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}
	@OnlyIn(Dist.CLIENT)
	public static void sendToServer(CustomPacketPayload payload) {
		ClientPacketDistributor.sendToServer(payload);
	}
	public static boolean isModLoaded(String id) {
		return ModList.get().isLoaded(id);
	}
	public static Path getConfigFolder() {
		return FMLPaths.CONFIGDIR.get();
	}
	public static boolean isFabric() {
		return false;
	}
}
*///? }
