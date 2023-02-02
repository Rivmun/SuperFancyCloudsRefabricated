package com.rimo.sfcr;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.client.MinecraftClient;

@Config(name = "sfcr")
public class SFCReConfig implements ConfigData {
	//enable
	private boolean enableMod = true;
	public boolean isEnableMod() {return enableMod;}
	public void setEnableMod(boolean isEnable) {enableMod = isEnable;}
	//weather detect
	private boolean enableWeatherDensity = true;
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
	//cloud height
	private int cloudHeight = 192;
	public int getCloudHeight() {return cloudHeight;}
	public void setCloudHeight(int height) {cloudHeight = height;}
	//cloud thickness
	private int cloudLayerThickness = 32;
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	//cloud render distance
	private int cloudRenderDistance = 96;
	@SuppressWarnings("resource")
	public int getCloudRenderDistance() {
		if (cloudRenderDistanceFitToView && MinecraftClient.getInstance().player != null) {
			return MinecraftClient.getInstance().options.getClampedViewDistance() * 12;
		} else {
			return cloudRenderDistance;
		}
	}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	//cloud render distance fit to view
	private boolean cloudRenderDistanceFitToView = false;
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	//fog
	private boolean enableFog = true;
	public boolean isEnableFog() {return enableFog;}
	public void setEnableFog(boolean isEnable) {enableFog = isEnable;}
	//fog auto distance
	private boolean fogAutoDistance = true;
	public boolean isFogAutoDistance() {return fogAutoDistance;}
	public void setFogAutoDistance(boolean isEnable) {fogAutoDistance = isEnable;}
	//fog distance
	private int fogMinDistance = 2;
	private int fogMaxDistance = 4;
	public int getFogMinDistance() {return fogMinDistance;}
	public int getFogMaxDistance() {return fogMaxDistance;}
	public void setFogDisance(int min, int max) {
		if (min > max) {
			fogMinDistance = max;
			fogMaxDistance = min;
		} else {
			fogMinDistance = min;
			fogMaxDistance = max;
		}
	}
	
	//When nofog, need this to extend frustum.
	public int getMaxFogDistanceWhenNoFog() {
		return cloudRenderDistance / 48;
	}
}
