package com.rimo.sfcr;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rimo.sfcr.config.CloudRefreshSpeed;
import com.rimo.sfcr.config.SFCReConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SFCReMain implements ModInitializer {
	
	public static final Logger LOGGER = LoggerFactory.getLogger("sfcr");
	public static final ConfigHolder<SFCReConfig> CONFIGHOLDER = AutoConfig.register(SFCReConfig.class, GsonConfigSerializer::new);
	public static final SFCReRuntimeData RUNTIME = new SFCReRuntimeData();
	public static SFCReConfig config = CONFIGHOLDER.getConfig();
	
	public static Identifier PACKET_CONFIG = new Identifier("sfcr", "config_s2c");
	public static Identifier PACKET_SYNC_REQUEST = new Identifier("sfcr", "sync_request_c2s");
	
	@Override
	public void onInitialize() {
		ServerWorldEvents.LOAD.register((server, world) -> RUNTIME.init(server, world));		
		ServerTickEvents.START_SERVER_TICK.register(server -> RUNTIME.tick(server));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SFCReMain.sendConfig(handler.getPlayer(), server);
			SFCReRuntimeData.sendInitialData(handler.getPlayer(), server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> RUNTIME.end());
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
		packet.writeInt(config.getCloudHeight());
		packet.writeInt(config.getSampleSteps());
		packet.writeEnumConstant(config.getDensityChangingSpeed());
		packet.writeInt(config.getCloudDensityPercent());
		packet.writeInt(config.getRainDensityPercent());
		packet.writeInt(config.getThunderDensityPercent());
		packet.writeInt(config.getBiomeDensityMultipler());
		packet.writeInt(config.getBiomeFilterList().size());
		for (String id : config.getBiomeFilterList()) {
			packet.writeString(id);
		}
		ServerPlayNetworking.send(player, PACKET_CONFIG, packet);
	}
	
	public static void receiveConfig(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
		if (!config.isEnableServerConfig())
			return;
		config.setCloudHeight(packet.readInt());
		config.setSampleSteps(packet.readInt());
		config.setDensityChangingSpeed(packet.readEnumConstant(CloudRefreshSpeed.class));
		config.setCloudDensityPercent(packet.readInt());
		config.setRainDensityPercent(packet.readInt());
		config.setThunderDensityPercent(packet.readInt());
		config.setBiomeDensityMultipler(packet.readInt());
		var size = packet.readInt();
		List<String> list = new ArrayList<>();
		while (size > 0) {
			list.add(packet.readString());
			size--;
		}
		config.setBiomeFilterList(list);
		SFCReClient.RENDERER.updateConfigFromServer(config);
	}
}
