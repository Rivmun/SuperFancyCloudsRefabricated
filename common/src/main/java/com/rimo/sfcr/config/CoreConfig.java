package com.rimo.sfcr.config;

import com.rimo.sfcr.util.CloudRefreshSpeed;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class CoreConfig {

	CoreConfig() {
		biomeFilterList.add("RIVER");
	}

	protected int cloudHeight = -1;
	protected int cloudBlockSize = 8;
	protected int cloudLayerThickness = 32;
	protected int sampleSteps = 2;
	protected int cloudColor = 0xFFFFFF;
	protected float cloudBrightMultiplier = 0.1f;
	protected float densityThreshold = 1.3f;
	protected float thresholdMultiplier = 1.5f;
	protected boolean enableWeatherDensity = true;
	protected int weatherPreDetectTime = 10;
	protected int cloudDensityPercent = 25;
	protected int rainDensityPercent = 60;
	protected int thunderDensityPercent = 90;
	protected CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.SLOW;
	protected int biomeDensityMultiplier = 50;
	protected boolean isBiomeDensityByChunk = false;
	protected boolean isBiomeDensityUseLoadedChunk = false;
	protected List<String> biomeFilterList = new ArrayList<String>();

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
	public int getBiomeDensityMultiplier() {return biomeDensityMultiplier;}
	public boolean isBiomeDensityByChunk() {return isBiomeDensityByChunk;}
	public boolean isBiomeDensityUseLoadedChunk() {return isBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}

	public void setCloudHeight(int height) {cloudHeight = height;}
	public void setCloudBlockSize(int size) {cloudBlockSize = size;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
	public void setCloudColor(int cloudColor) {this.cloudColor = cloudColor;}
	public void setCloudBrightMultiplier(float cloudBrightMultiplier) {this.cloudBrightMultiplier = cloudBrightMultiplier;}
	public void setDensityThreshold(float density) {densityThreshold = density;}
	public void setThresholdMultiplier(float multipler) {thresholdMultiplier = multipler;}
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
	public void setWeatherPreDetectTime(int time) {weatherPreDetectTime = time;}
	public void setCloudDensityPercent(int density) {cloudDensityPercent = density;}
	public void setRainDensityPercent(int density) {rainDensityPercent = density;}
	public void setThunderDensityPercent(int density) {thunderDensityPercent = density;}
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setBiomeDensityMultiplier(int multiplier) {biomeDensityMultiplier = multiplier;}
	public void setBiomeDensityByChunk(boolean isEnable) {isBiomeDensityByChunk = isEnable;}
	public void setBiomeDensityUseLoadedChunk(boolean isEnable) {isBiomeDensityUseLoadedChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}

	public void setCoreConfig(CoreConfig config) {		//TODO: why do I need to write this sht...
		this.cloudHeight					= config.cloudHeight;
		this.cloudBlockSize					= config.cloudBlockSize;
		this.cloudLayerThickness			= config.cloudLayerThickness;
		this.sampleSteps					= config.sampleSteps;
		this.cloudColor						= config.cloudColor;
		this.cloudBrightMultiplier			= config.cloudBrightMultiplier;
		this.densityThreshold				= config.densityThreshold;
		this.thresholdMultiplier			= config.thresholdMultiplier;
		this.enableWeatherDensity			= config.enableWeatherDensity;
		this.weatherPreDetectTime			= config.weatherPreDetectTime;
		this.cloudDensityPercent			= config.cloudDensityPercent;
		this.rainDensityPercent				= config.rainDensityPercent;
		this.thunderDensityPercent			= config.thunderDensityPercent;
		this.densityChangingSpeed			= config.densityChangingSpeed;
		this.biomeDensityMultiplier			= config.biomeDensityMultiplier;
		this.isBiomeDensityByChunk			= config.isBiomeDensityByChunk;
		this.isBiomeDensityUseLoadedChunk	= config.isBiomeDensityUseLoadedChunk;
		this.biomeFilterList				= config.biomeFilterList;
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

	public boolean isFilterListHasBiome(Biome.Category biomeCategory) {
		return this.getBiomeFilterList().contains(biomeCategory.toString());
	}
}
