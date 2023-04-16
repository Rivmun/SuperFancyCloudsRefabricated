package com.rimo.sfcr;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.core.RuntimeData;
import com.rimo.sfcr.util.CloudRefreshSpeed;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
//import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SFCReMain implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("sfcr");
	public static final ConfigHolder<SFCReConfig> CONFIGHOLDER = AutoConfig.register(SFCReConfig.class, GsonConfigSerializer::new);
	public static final RuntimeData RUNTIME = new RuntimeData();
	public static SFCReConfig config = CONFIGHOLDER.getConfig();

	public static Identifier PACKET_CONFIG = new Identifier("sfcr", "config_s2c");
	public static Identifier PACKET_SYNC_REQUEST = new Identifier("sfcr", "sync_request_c2s");

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(PACKET_SYNC_REQUEST, SFCReMain::receiveSyncRequest);

		ServerWorldEvents.LOAD.register((server, world) -> RUNTIME.init(server, world));		
		ServerTickEvents.START_SERVER_TICK.register(server -> RUNTIME.tick(server));
		/* - - - Listen the request from client instead...
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SFCReMain.sendConfig(handler.getPlayer(), server);
			SFCReRuntimeData.sendRuntimeData(handler.getPlayer(), server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> RUNTIME.end());
		 */
	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		if (config.isEnableDebug()) {
			SFCReMain.LOGGER.error(e.toString());
			for (StackTraceElement i : e.getStackTrace()) {
				SFCReMain.LOGGER.error(i.getClassName() + ":" + i.getLineNumber());
			}
		}
	}

	//Push to Mixin.
	public static int getFogDistance() {
		if (config.isEnableFog()) {
			return config.getFogMaxDistance();
		}
		return config.getMaxFogDistanceWhenNoFog();
	}
	//Push to Mixin.
	public static boolean getModEnabled() {
		return config.isEnableMod();
	}

	public static void sendConfig(ServerPlayerEntity player, MinecraftServer server) {
		if (!config.isEnableMod())
			return;
		PacketByteBuf packet = PacketByteBufs.create();
		packet.writeLong(SFCReMain.RUNTIME.seed);
		packet.writeInt(config.getSecPerSync());
		packet.writeInt(config.getCloudHeight());
		packet.writeInt(config.getCloudLayerThickness());
		packet.writeInt(config.getSampleSteps());
		packet.writeEnumConstant(config.getDensityChangingSpeed());
		packet.writeInt(config.getCloudDensityPercent());
		packet.writeInt(config.getRainDensityPercent());
		packet.writeInt(config.getThunderDensityPercent());
		packet.writeInt(config.getCloudBlockSize());
		packet.writeInt(config.getSnowDensity());
		packet.writeInt(config.getRainDensity());
		packet.writeInt(config.getNoneDensity());
		packet.writeBoolean(config.isBiomeDensityByChunk());
		packet.writeBoolean(config.isBiomeDensityUseLoadedChunk());
		packet.writeBoolean(config.isEnableTerrainDodge());
		packet.writeInt(config.getBiomeFilterList().size());
		for (String id : config.getBiomeFilterList()) {
			packet.writeString(id);
		}
		ServerPlayNetworking.send(player, PACKET_CONFIG, packet);
	}

	public static void receiveConfig(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		if (!config.isEnableServerConfig())
			return;
		SFCReMain.RUNTIME.seed = packet.readLong();
		config.setSecPerSync(packet.readInt());
		config.setCloudHeight(packet.readInt());
		config.setCloudLayerThickness(packet.readInt());
		config.setSampleSteps(packet.readInt());
		config.setDensityChangingSpeed(packet.readEnumConstant(CloudRefreshSpeed.class));
		config.setCloudDensityPercent(packet.readInt());
		config.setRainDensityPercent(packet.readInt());
		config.setThunderDensityPercent(packet.readInt());
		config.setCloudBlockSize(packet.readInt());
		config.setSnowDensity(packet.readInt());
		config.setRainDensity(packet.readInt());
		config.setNoneDensity(packet.readInt());
		config.setBiomeDensityByChunk(packet.readBoolean());
		config.setBiomeDensityUseLoadedChunk(packet.readBoolean());
		config.setEnableTerrainDodge(packet.readBoolean());
		var size = packet.readInt();
		List<String> list = new ArrayList<>();
		while (size > 0) {
			list.add(packet.readString());
			size--;
		}
		config.setBiomeFilterList(list);
		SFCReClient.RENDERER.updateRenderData(config);
		SFCReClient.RENDERER.init();		// Reset renderer.

		if (SFCReMain.config.isEnableDebug())
			client.getMessageHandler().onGameMessage(Text.translatable("text.sfcr.command.sync_full_succ"), false);
	}

	public static void receiveSyncRequest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		var isFull = packet.readBoolean();
		if (isFull)
			SFCReMain.sendConfig(player, server);
		RuntimeData.sendRuntimeData(player, server);
		if (config.isEnableDebug())
			SFCReMain.LOGGER.info("[SFCRe] Auto send " + (isFull ? "full" : "") + "sync data to " + player.getDisplayName().getString());
	}
}
