package com.rimo.sfcr.network;

import com.google.gson.Gson;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CoreConfig;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class ConfigSyncMessage {
	private final long seed;
//	private final int cloudHeight;
//	private final int cloudBlockSize;
//	private final int cloudLayerThickness;
//	private final int sampleSteps;
//	private final int cloudColor;
//	private final float cloudBrightMultiplier;
//	private final float densityThreshold;
//	private final float thresholdMultiplier;
//	private final boolean enableWeatherDensity;
//	private final int weatherPreDetectTime;
//	private final int cloudDensityPercent;
//	private final int rainDensityPercent;
//	private final int thunderDensityPercent;
//	private final CloudRefreshSpeed densityChangingSpeed;
//	private final int snowDensity;
//	private final int rainDensity;
//	private final int noneDensity;
//	private final boolean isBiomeDensityByChunk;
//	private final boolean isBiomeDensityUseLoadedChunk;
//	private int listSize;
//	private final List<String> biomeFilterList;
	private final String data;
	private static final Gson gson = new Gson();

	public ConfigSyncMessage(long seed, CoreConfig config) {
		this.seed							= seed;
//		this.cloudHeight					= config.getCloudHeight();
//		this.cloudBlockSize					= config.getCloudBlockSize();
//		this.cloudLayerThickness			= config.getCloudLayerThickness();
//		this.sampleSteps					= config.getSampleSteps();
//		this.cloudColor						= config.getCloudColor();
//		this.cloudBrightMultiplier			= config.getCloudBrightMultiplier();
//		this.densityThreshold				= config.getDensityThreshold();
//		this.thresholdMultiplier			= config.getThresholdMultiplier();
//		this.enableWeatherDensity			= config.isEnableWeatherDensity();
//		this.weatherPreDetectTime			= config.getWeatherPreDetectTime();
//		this.cloudDensityPercent			= config.getCloudDensityPercent();
//		this.rainDensityPercent				= config.getRainDensityPercent();
//		this.thunderDensityPercent			= config.getThunderDensityPercent();
//		this.densityChangingSpeed			= config.getDensityChangingSpeed();
//		this.snowDensity					= config.getSnowDensity();
//		this.rainDensity					= config.getRainDensity();
//		this.noneDensity					= config.getNoneDensity();
//		this.isBiomeDensityByChunk			= config.isBiomeDensityByChunk();
//		this.isBiomeDensityUseLoadedChunk	= config.isBiomeDensityUseLoadedChunk();
//		this.listSize						= config.getBiomeFilterList().size();
//		this.biomeFilterList				= config.getBiomeFilterList();
		this.data = gson.toJson(config);
	}

	public ConfigSyncMessage(PacketByteBuf packet) {
		this.seed							= packet.readLong();
//		this.cloudHeight					= packet.readInt();
//		this.cloudBlockSize					= packet.readInt();
//		this.cloudLayerThickness			= packet.readInt();
//		this.sampleSteps					= packet.readInt();
//		this.cloudColor						= packet.readInt();
//		this.cloudBrightMultiplier			= packet.readFloat();
//		this.densityThreshold				= packet.readFloat();
//		this.thresholdMultiplier			= packet.readFloat();
//		this.enableWeatherDensity			= packet.readBoolean();
//		this.weatherPreDetectTime			= packet.readInt();
//		this.cloudDensityPercent			= packet.readInt();
//		this.rainDensityPercent				= packet.readInt();
//		this.thunderDensityPercent			= packet.readInt();
//		this.densityChangingSpeed			= packet.readEnumConstant(CloudRefreshSpeed.class);
//		this.snowDensity					= packet.readInt();
//		this.rainDensity					= packet.readInt();
//		this.noneDensity					= packet.readInt();
//		this.isBiomeDensityByChunk			= packet.readBoolean();
//		this.isBiomeDensityUseLoadedChunk	= packet.readBoolean();
//		this.listSize						= packet.readInt();
//		this.biomeFilterList				= new ArrayList<>();
//		while (this.listSize-- > 0)
//			this.biomeFilterList.add(packet.readString());
		this.data = packet.readString();
	}

	public static void encode(ConfigSyncMessage message, PacketByteBuf packet) {
		packet.writeLong(					message.seed);
//		packet.writeInt(					message.cloudHeight);
//		packet.writeInt(					message.cloudBlockSize);
//		packet.writeInt(					message.cloudLayerThickness);
//		packet.writeInt(					message.sampleSteps);
//		packet.writeInt(					message.cloudColor);
//		packet.writeFloat(					message.cloudBrightMultiplier);
//		packet.writeFloat(					message.densityThreshold);
//		packet.writeFloat(					message.thresholdMultiplier);
//		packet.writeBoolean(				message.enableWeatherDensity);
//		packet.writeInt(					message.weatherPreDetectTime);
//		packet.writeInt(					message.cloudDensityPercent);
//		packet.writeInt(					message.rainDensityPercent);
//		packet.writeInt(					message.thunderDensityPercent);
//		packet.writeEnumConstant(			message.densityChangingSpeed);
//		packet.writeInt(					message.snowDensity);
//		packet.writeInt(					message.rainDensity);
//		packet.writeInt(					message.noneDensity);
//		packet.writeBoolean(				message.isBiomeDensityByChunk);
//		packet.writeBoolean(				message.isBiomeDensityUseLoadedChunk);
//		packet.writeInt(					message.listSize);
//		message.biomeFilterList.forEach(packet::writeString);
		packet.writeString(message.data);
	}

	public static void receive(ConfigSyncMessage message, Supplier<NetworkManager.PacketContext> contextSupplier) {
		if (!SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;
		if (contextSupplier.get().getEnv() != EnvType.CLIENT)
			return;

		contextSupplier.get().queue(() -> {
			SFCReMod.RUNTIME.seed = message.seed;
//			SFCReMod.COMMON_CONFIG.setCloudHeight(					message.cloudHeight);
//			SFCReMod.COMMON_CONFIG.setCloudBlockSize(				message.cloudBlockSize);
//			SFCReMod.COMMON_CONFIG.setCloudLayerThickness(			message.cloudLayerThickness);
//			SFCReMod.COMMON_CONFIG.setSampleSteps(					message.sampleSteps);
//			SFCReMod.COMMON_CONFIG.setCloudColor(					message.cloudColor);
//			SFCReMod.COMMON_CONFIG.setCloudBrightMultiplier(		message.cloudBrightMultiplier);
//			SFCReMod.COMMON_CONFIG.setDensityThreshold(				message.densityThreshold);
//			SFCReMod.COMMON_CONFIG.setThresholdMultiplier(			message.thresholdMultiplier);
//			SFCReMod.COMMON_CONFIG.setEnableWeatherDensity(			message.enableWeatherDensity);
//			SFCReMod.COMMON_CONFIG.setWeatherPreDetectTime(			message.weatherPreDetectTime);
//			SFCReMod.COMMON_CONFIG.setCloudDensityPercent(			message.cloudDensityPercent);
//			SFCReMod.COMMON_CONFIG.setRainDensityPercent(			message.rainDensityPercent);
//			SFCReMod.COMMON_CONFIG.setThunderDensityPercent(		message.thunderDensityPercent);
//			SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(			message.densityChangingSpeed);
//			SFCReMod.COMMON_CONFIG.setSnowDensity(					message.snowDensity);
//			SFCReMod.COMMON_CONFIG.setRainDensity(					message.rainDensity);
//			SFCReMod.COMMON_CONFIG.setNoneDensity(					message.noneDensity);
//			SFCReMod.COMMON_CONFIG.setBiomeDensityByChunk(			message.isBiomeDensityByChunk);
//			SFCReMod.COMMON_CONFIG.setBiomeDensityUseLoadedChunk(	message.isBiomeDensityUseLoadedChunk);
//			SFCReMod.COMMON_CONFIG.setBiomeFilterList(				message.biomeFilterList);
			SFCReMod.COMMON_CONFIG.setCoreConfig(gson.fromJson(message.data, CoreConfig.class));

			SFCReMod.RENDERER.init();		// Reset renderer.
			SFCReMod.RENDERER.updateConfig(SFCReMod.COMMON_CONFIG);
			if (SFCReMod.COMMON_CONFIG.isEnableDebug())
				contextSupplier.get().getPlayer().sendMessage(Text.translatable("text.sfcr.command.sync_full_succ"), false);
		});
//		, () -> {
//			SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
//			SFCReMod.COMMON_CONFIG_HOLDER.save();
//			contextSupplier.get().getPlayer().sendMessage(Text.translatable("text.sfcr.command.sync_fail"), false);
//		})
	}
}
