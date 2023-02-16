package com.rimo.sfcr.config;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.client.MinecraftClient;

@Config(name = "sfcr")
public class SFCReConfig implements ConfigData {
	//----CLOUDS, GENERAL----
	private boolean enableMod = true;
	private boolean enableServerConfig = false;
	private int secPerSync = 30;		// Banned in config screen on client.
	private int cloudHeight = 192;
	private int cloudLayerThickness = 32;
	private int cloudRenderDistance = 96;
	private boolean cloudRenderDistanceFitToView = false;
	private CloudRefreshSpeed normalRefreshSpeed = CloudRefreshSpeed.SLOW;
	private int sampleSteps = 3;
	private boolean enableDebug = false;
	//----FOG----
	private boolean enableFog = true;
	private boolean fogAutoDistance = true;
	private int fogMinDistance = 2;
	private int fogMaxDistance = 4;
	//----DENSITY CHANGE----
	private boolean enableWeatherDensity = true;
	private int weatherPreDetectTime = 10;
	private int cloudDensityPercent = 25;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private CloudRefreshSpeed weatherRefreshSpeed = CloudRefreshSpeed.FAST;
	private CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.NORMAL;
	private int biomeDensityMultipler = 50;
	private List<String> biomeFilterList = new ArrayList<>();
	
	//output func.
	public boolean isEnableMod() {return enableMod;}
	public boolean isEnableServerConfig() {return enableServerConfig;}
	public int getSecPerSync() {return secPerSync;}
	public int getCloudHeight() {return cloudHeight;}
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	@SuppressWarnings("resource")
	public int getCloudRenderDistance() {
		if (cloudRenderDistanceFitToView && MinecraftClient.getInstance().player != null) {
			return MinecraftClient.getInstance().options.getClampedViewDistance() * 12;
		} else {
			return cloudRenderDistance;
		}
	}
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public int getSampleSteps() {return sampleSteps;}
	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableFog() {return enableFog;}
	public boolean isFogAutoDistance() {return fogAutoDistance;}
	public int getFogMinDistance() {return fogMinDistance;}
	public int getFogMaxDistance() {return fogMaxDistance;}
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
	public int getWeatherPreDetectTime() {return weatherPreDetectTime;}
	public int getCloudDensityPercent() {return cloudDensityPercent;}
	public int getRainDensityPercent() {return rainDensityPercent;}
	public int getThunderDensityPercent() {return thunderDensityPercent;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public CloudRefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public int getBiomeDensityMultipler() {return biomeDensityMultipler;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}
	
	//input func.
	public void setEnableMod(boolean isEnable) {enableMod = isEnable;}
	public void setEnableServerConfig(boolean isEnable) {enableServerConfig = isEnable;}
	public void setSecPerSync(int value) {secPerSync = value; }
	public void setCloudHeight(int height) {cloudHeight = height;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
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
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
	public void setWeatherPreDetectTime(int time) {weatherPreDetectTime = time;}
	public void setCloudDensityPercent(int density) {cloudDensityPercent = density;}
	public void setRainDensityPercent(int density) {rainDensityPercent = density;}
	public void setThunderDensityPercent(int density) {thunderDensityPercent = density;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setBiomeDensityMultipler(int multipler) {biomeDensityMultipler = multipler;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}
	
	
	SFCReConfig(){
		biomeFilterList.add("minecraft:river");
		biomeFilterList.add("minecraft:frozen_river");
	}
	
	//When nofog, need this to extend frustum.
	public int getMaxFogDistanceWhenNoFog() {
		return (int)Math.pow(cloudRenderDistance / 48f, 2);
	}
	
	//exchanged speed enum.
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
}
