package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.ArrayList;
import java.util.List;

public class SharedConfig {
	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final List<String> DEF_BIOME_FILTER_LIST = new ArrayList<>(List.of(
			"#minecraft:is_river"
	));

	private boolean isEnableRender = true;
	private boolean enableFog = true;
	private boolean fogAutoDistance = true;
	private int fogMinDistance = 2;
	private int fogMaxDistance = 4;
	private boolean isEnableCloudRain = false;
	private int cloudRenderDistance = 64;
	private boolean cloudRenderDistanceFitToView = false;
	private int cloudHeight = 192;
	private int cloudBlockSize = 12;
	private int cloudLayerThickness = 10;
	private boolean enableTerrainDodge = true;
	private int sampleSteps = 2;
	private int cloudColor = 0xFFFFFFFF;
	private boolean enableBottomDim = true;
	private boolean enableDuskBlush = true;
	private float cloudBrightMultiplier = 0.1f;
	private float densityThreshold = 1.3f;
	private float thresholdMultiplier = 1.5f;
	private boolean enableWeatherDensity = true;
	private int weatherPreDetectTime = 5;
	private int cloudDensityPercent = 25;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private float densityAtNight = 0.7F;
	private CloudRefreshSpeed normalRefreshSpeed = CloudRefreshSpeed.SLOW;
	private CloudRefreshSpeed weatherRefreshSpeed = CloudRefreshSpeed.FAST;
	private CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.SLOW;
	private int snowDensity = 60;
	private int rainDensity = 90;
	private int noneDensity = 0;
	private boolean isBiomeDensityByChunk = false;
	private boolean isBiomeDensityUseLoadedChunk = false;
	private List<String> biomeFilterList = DEF_BIOME_FILTER_LIST;

	public SharedConfig() {}
	public void setSharedConfig(SharedConfig config) {
		this.isEnableRender               = config.isEnableRender;
		this.enableFog                    = config.enableFog;
		this.fogAutoDistance              = config.fogAutoDistance;
		this.fogMinDistance               = config.fogMinDistance;
		this.fogMaxDistance               = config.fogMaxDistance;
		this.isEnableCloudRain            = config.isEnableCloudRain;
		this.cloudRenderDistance          = config.cloudRenderDistance;
		this.cloudRenderDistanceFitToView = config.cloudRenderDistanceFitToView;
		this.cloudHeight                  = config.cloudHeight;
		this.cloudBlockSize               = config.cloudBlockSize;
		this.cloudLayerThickness          = config.cloudLayerThickness;
		this.enableTerrainDodge           = config.enableTerrainDodge;
		this.sampleSteps                  = config.sampleSteps;
		this.cloudColor                   = config.cloudColor;
		this.enableBottomDim              = config.enableBottomDim;
		this.enableDuskBlush              = config.enableDuskBlush;
		this.cloudBrightMultiplier        = config.cloudBrightMultiplier;
		this.densityThreshold             = config.densityThreshold;
		this.thresholdMultiplier          = config.thresholdMultiplier;
		this.enableWeatherDensity         = config.enableWeatherDensity;
		this.weatherPreDetectTime         = config.weatherPreDetectTime;
		this.cloudDensityPercent          = config.cloudDensityPercent;
		this.rainDensityPercent           = config.rainDensityPercent;
		this.thunderDensityPercent        = config.thunderDensityPercent;
		this.densityAtNight               = config.densityAtNight;
		this.normalRefreshSpeed           = config.normalRefreshSpeed;
		this.weatherRefreshSpeed          = config.weatherRefreshSpeed;
		this.densityChangingSpeed         = config.densityChangingSpeed;
		this.snowDensity                  = config.snowDensity;
		this.rainDensity                  = config.rainDensity;
		this.noneDensity                  = config.noneDensity;
		this.isBiomeDensityByChunk        = config.isBiomeDensityByChunk;
		this.isBiomeDensityUseLoadedChunk = config.isBiomeDensityUseLoadedChunk;
		this.biomeFilterList              = config.biomeFilterList;
	}

	public boolean isEnableRender() {return isEnableRender;}
	public int getCloudHeight() {return cloudHeight;}
	public int getCloudBlockSize() {return cloudBlockSize;}
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	public int getSampleSteps() {return sampleSteps;}
	public int getCloudColor() {return cloudColor;}
	public float getCloudBrightMultiplier() {return cloudBrightMultiplier;}
	public float getDensityThreshold() {return densityThreshold;}
	public float getThresholdMultiplier() {return thresholdMultiplier;}
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
	public int getWeatherPreDetectTime() {return weatherPreDetectTime;}
	public int getCloudDensityPercent() {return cloudDensityPercent;}
	public int getRainDensityPercent() {return rainDensityPercent;}
	public int getThunderDensityPercent() {return thunderDensityPercent;}
	public float getDensityAtNight() {return densityAtNight;}
	public CloudRefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public int getSnowDensity() {return snowDensity;}
	public int getRainDensity() {return rainDensity;}
	public int getNoneDensity() {return noneDensity;}
	public boolean isBiomeDensityByChunk() {return isBiomeDensityByChunk;}
	public boolean isBiomeDensityUseLoadedChunk() {return isBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}
	public int getCloudRenderDistance() {return cloudRenderDistance;}
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public boolean isEnableFog() {return enableFog;}
	public boolean isFogAutoDistance() {return fogAutoDistance;}
	public int getFogMinDistance() {return fogMinDistance;}
	public int getFogMaxDistance() {return fogMaxDistance;}
	public boolean isEnableBottomDim() {return this.enableBottomDim;}
	public boolean isEnableDuskBlush() {return this.enableDuskBlush;}
	public boolean isEnableCloudRain() {return isEnableCloudRain;}

	public void setEnableRender(boolean isEnable) {
		isEnableRender = isEnable;}
	public void setCloudHeight(int height) {cloudHeight = height;}
	public void setCloudBlockSize(int size) {cloudBlockSize = size;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
	public void setCloudColor(int cloudColor) {this.cloudColor = cloudColor;}
	public void setCloudBrightMultiplier(float cloudBrightMultiplier) {this.cloudBrightMultiplier = cloudBrightMultiplier;}
	public void setDensityThreshold(float density) {densityThreshold = density;}
	public void setThresholdMultiplier(float multiplier) {thresholdMultiplier = multiplier;}
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
	public void setWeatherPreDetectTime(int time) {weatherPreDetectTime = time;}
	public void setCloudDensityPercent(int density) {cloudDensityPercent = density;}
	public void setRainDensityPercent(int density) {rainDensityPercent = density;}
	public void setThunderDensityPercent(int density) {thunderDensityPercent = density;}
	public void setDensityAtNight(float density) {this.densityAtNight = density;}
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setSnowDensity(int density) {snowDensity = density;}
	public void setRainDensity(int density) {rainDensity = density;}
	public void setNoneDensity(int density) {noneDensity = density;}
	public void setBiomeDensityByChunk(boolean isEnable) {isBiomeDensityByChunk = isEnable;}
	public void setBiomeDensityUseLoadedChunk(boolean isEnable) {isBiomeDensityUseLoadedChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}
	public void setEnableTerrainDodge(boolean isEnable) {enableTerrainDodge = isEnable;}
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
	public void setEnableBottomDim(boolean enableBottomDim) {this.enableBottomDim = enableBottomDim;}
	public void setEnableDuskBlush(boolean enableDuskBlush) {this.enableDuskBlush = enableDuskBlush;}
	public void setEnableCloudRain(boolean enableCloudRain) {this.isEnableCloudRain = enableCloudRain;}

	//conversion
	public int getAutoFogMaxDistance() {
		return (int) (cloudRenderDistance / 24f * cloudRenderDistance / 24f * getCloudBlockSize() / 16f);
	}

	public boolean isFilterListHasBiome(RegistryEntry<Biome> biome) {
		var isHas = false;
		if (this.getBiomeFilterList().contains(biome.getKey().orElse(BiomeKeys.THE_VOID).getValue().toString())) {
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

	public float getDownfall(Biome.Precipitation i) {
		if (i.equals(Biome.Precipitation.SNOW)) {
			return this.getSnowDensity() / 100f;
		} else if (i.equals(Biome.Precipitation.RAIN)) {
			return this.getRainDensity() / 100f;
		} else {
			return this.getNoneDensity() / 100f;
		}
	}

	/**
	 * @return a SharedConfig JSON string
	 */
	@Override
	public String toString() {
		return GSON.toJson(this, SharedConfig.class);
	}

	/**
	 * @throws JsonSyntaxException if input string cannot convert to SharedConfig
	 */
	public void fromString(String s) throws JsonSyntaxException {
		setSharedConfig(GSON.fromJson(s, SharedConfig.class));
	}
}
