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

	private boolean enableMod = true;
	private int cloudHeight = 192;
	private int cloudBlockSize = 12;
	private int cloudLayerThickness = 10;
	private int sampleSteps = 2;
	private int cloudColor = 0xFFFFFF;
	private float cloudBrightMultiplier = 0.1f;
	private float densityThreshold = 1.3f;
	private float thresholdMultiplier = 1.5f;
	private boolean enableWeatherDensity = true;
	private int weatherPreDetectTime = 10;
	private int cloudDensityPercent = 25;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.SLOW;
	private int snowDensity = 60;
	private int rainDensity = 90;
	private int noneDensity = 0;
	private boolean isBiomeDensityByChunk = false;
	private boolean isBiomeDensityUseLoadedChunk = false;
	private List<String> biomeFilterList = DEF_BIOME_FILTER_LIST;

	public SharedConfig() {}
	public void setSharedConfig(SharedConfig config) {
		this.enableMod                    = config.enableMod;
		this.cloudHeight                  = config.cloudHeight;
		this.cloudBlockSize               = config.cloudBlockSize;
		this.cloudLayerThickness          = config.cloudLayerThickness;
		this.sampleSteps                  = config.sampleSteps;
		this.cloudColor                   = config.cloudColor;
		this.cloudBrightMultiplier        = config.cloudBrightMultiplier;
		this.densityThreshold             = config.densityThreshold;
		this.thresholdMultiplier          = config.thresholdMultiplier;
		this.enableWeatherDensity         = config.enableWeatherDensity;
		this.weatherPreDetectTime         = config.weatherPreDetectTime;
		this.cloudDensityPercent          = config.cloudDensityPercent;
		this.rainDensityPercent           = config.rainDensityPercent;
		this.thunderDensityPercent        = config.thunderDensityPercent;
		this.densityChangingSpeed         = config.densityChangingSpeed;
		this.snowDensity                  = config.snowDensity;
		this.rainDensity                  = config.rainDensity;
		this.noneDensity                  = config.noneDensity;
		this.isBiomeDensityByChunk        = config.isBiomeDensityByChunk;
		this.isBiomeDensityUseLoadedChunk = config.isBiomeDensityUseLoadedChunk;
		this.biomeFilterList              = config.biomeFilterList;
	}

	public boolean isEnableMod() {return enableMod;}
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
	public CloudRefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public int getSnowDensity() {return snowDensity;}
	public int getRainDensity() {return rainDensity;}
	public int getNoneDensity() {return noneDensity;}
	public boolean isBiomeDensityByChunk() {return isBiomeDensityByChunk;}
	public boolean isBiomeDensityUseLoadedChunk() {return isBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}

	public void setEnableMod(boolean isEnable) {enableMod = isEnable;}
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
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setSnowDensity(int density) {snowDensity = density;}
	public void setRainDensity(int density) {rainDensity = density;}
	public void setNoneDensity(int density) {noneDensity = density;}
	public void setBiomeDensityByChunk(boolean isEnable) {isBiomeDensityByChunk = isEnable;}
	public void setBiomeDensityUseLoadedChunk(boolean isEnable) {isBiomeDensityUseLoadedChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}

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
