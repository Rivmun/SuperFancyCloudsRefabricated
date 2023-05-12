package com.rimo.sfcr.config;

import java.util.ArrayList;
import java.util.List;
import com.rimo.sfcr.util.CloudRefreshSpeed;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;

@Config(name = "sfcr")
public class SFCReConfig implements ConfigData {

	public static List<String> DEF_BIOME_FILTER_LIST = new ArrayList<String>(List.of(
			"#minecraft:is_river"
	));

	//----GENERAL----
	private boolean enableMod = true;
	private boolean enableServerConfig = false;
	private boolean enableFog = true;
	private boolean enableWeatherDensity = true;
	private int secPerSync = 30;		// Banned in config screen on client.
	private boolean enableNormalCull = true;
	private float cullRadianMultiplier = 1.0f;
	private boolean enableSmoothChange = false;
	private boolean enableDebug = false;
	//----CLOUDS----
	private int cloudHeight = 192;
	private int cloudBlockSize = 16;
	private int cloudLayerThickness = 32;
	private int cloudRenderDistance = 96;
	private boolean cloudRenderDistanceFitToView = false;
	private CloudRefreshSpeed normalRefreshSpeed = CloudRefreshSpeed.SLOW;
	private int sampleSteps = 3;
	private boolean enableTerrainDodge = true;
	//----FOG----
	private boolean fogAutoDistance = true;
	private int fogMinDistance = 2;
	private int fogMaxDistance = 4;
	//----DYNAMIC----
	private float densityThreshold = 1.3f;
	private float thresholdMultiplier = 1.5f;
	private int weatherPreDetectTime = 10;
	private int cloudDensityPercent = 25;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private CloudRefreshSpeed weatherRefreshSpeed = CloudRefreshSpeed.FAST;
	private CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.SLOW;
	private int snowDensity = 60;
	private int rainDensity = 90;
	private int noneDensity = 0;
	private boolean isBiomeDensityByChunk = false;
	private boolean isBiomeDensityUseLoadedChunk = false;
	private List<String> biomeFilterList = DEF_BIOME_FILTER_LIST;

	//output func.
	public boolean isEnableMod() {return enableMod;}
	public boolean isEnableServerConfig() {return enableServerConfig;}
	public int getSecPerSync() {return secPerSync;}
	public int getCloudHeight() {return cloudHeight;}
	public int getCloudBlockSize() {return cloudBlockSize;}
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	public int getCloudRenderDistance() {return cloudRenderDistance;}
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public int getSampleSteps() {return sampleSteps;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public boolean isEnableNormalCull() {return enableNormalCull;}
	public float getCullRadianMultiplier() {return cullRadianMultiplier;}
	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableFog() {return enableFog;}
	public boolean isFogAutoDistance() {return fogAutoDistance;}
	public int getFogMinDistance() {return fogMinDistance;}
	public int getFogMaxDistance() {return fogMaxDistance;}
	public float getDensityThreshold() {return densityThreshold;}
	public float getThresholdMultiplier() {return thresholdMultiplier;}
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
	public boolean isEnableSmoothChange() {return enableSmoothChange;}
	public int getWeatherPreDetectTime() {return weatherPreDetectTime;}
	public int getCloudDensityPercent() {return cloudDensityPercent;}
	public int getRainDensityPercent() {return rainDensityPercent;}
	public int getThunderDensityPercent() {return thunderDensityPercent;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public CloudRefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public int getSnowDensity() {return snowDensity;}
	public int getRainDensity() {return rainDensity;}
	public int getNoneDensity() {return noneDensity;}
	public boolean isBiomeDensityByChunk() {return isBiomeDensityByChunk;}
	public boolean isBiomeDensityUseLoadedChunk() {return isBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}

	//input func.
	public void setEnableMod(boolean isEnable) {enableMod = isEnable;}
	public void setEnableServerConfig(boolean isEnable) {enableServerConfig = isEnable;}
	public void setSecPerSync(int value) {secPerSync = value; }
	public void setCloudHeight(int height) {cloudHeight = height;}
	public void setCloudBlockSize(int size) {cloudBlockSize = size;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
	public void setEnableTerrainDodge(boolean isEnable) {enableTerrainDodge = isEnable;}
	public void setEnableNormalCull(boolean isEnable) {enableNormalCull = isEnable;}
	public void setCullRadianMultiplier(float value) {cullRadianMultiplier = value;}
	public void setEnableDebug(boolean isEnable) {enableDebug = isEnable;}
	public void setEnableFog(boolean isEnable) {enableFog = isEnable;}
	public void setFogAutoDistance(boolean isEnable) {fogAutoDistance = isEnable;}
	public void setFogDisance(int min, int max) {
		if (min > max) {
			fogMinDistance = max;
			fogMaxDistance = min;
		} else {
			fogMinDistance = min;
			fogMaxDistance = max;
		}
	}
	public void setDensityThreshold(float density) {densityThreshold = density;}
	public void setThresholdMultiplier(float multipler) {thresholdMultiplier = multipler;}
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
	public void setEnableSmoothChange(boolean isEnable) {enableSmoothChange = isEnable;}
	public void setWeatherPreDetectTime(int time) {weatherPreDetectTime = time;}
	public void setCloudDensityPercent(int density) {cloudDensityPercent = density;}
	public void setRainDensityPercent(int density) {rainDensityPercent = density;}
	public void setThunderDensityPercent(int density) {thunderDensityPercent = density;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setSnowDensity(int density) {snowDensity = density;}
	public void setRainDensity(int density) {rainDensity = density;}
	public void setNoneDensity(int density) {noneDensity = density;}
	public void setBiomeDensityByChunk(boolean isEnable) {isBiomeDensityByChunk = isEnable;}
	public void setBiomeDensityUseLoadedChunk(boolean isEnable) {isBiomeDensityUseLoadedChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}

	//conversion
	public int getAutoFogMaxDistance() {
		return (int) (cloudRenderDistance / 48f * cloudRenderDistance / 48f * cloudBlockSize / 16f);
	}

	public int getNumFromSpeedEnum(CloudRefreshSpeed value) {
		if (value.equals(CloudRefreshSpeed.VERY_FAST)) {
			return 5;
		} else if (value.equals(CloudRefreshSpeed.FAST)){
			return 10;
		} else if (value.equals(CloudRefreshSpeed.NORMAL)) {
			return 20;
		} else if (value.equals(CloudRefreshSpeed.SLOW)) {
			return 30;
		} else if (value.equals(CloudRefreshSpeed.VERY_SLOW)) {
			return 40;
		} else {
			return 20;
		}
	}

	public boolean isFilterListHasBiome(RegistryEntry<Biome> biome) {
		var isHas = false;
		if (this.getBiomeFilterList().contains(biome.getKey().get().getValue().toString())) {
			isHas = true;
		} else {
			for (TagKey<Biome> tag : biome.streamTags().toList()) {
				if (this.getBiomeFilterList().contains("#" + tag.id().toString())) {
					isHas = true;
					break;
				}
			}
		}
		return isHas;
	}

	public float getDownfall(Precipitation i) {
		if (i.equals(Precipitation.SNOW)) {
			return this.getSnowDensity() / 100f;
		} else if (i.equals(Precipitation.RAIN)) {
			return this.getRainDensity() / 100f;
		} else {
			return this.getNoneDensity() / 100f;
		}
	}
}
