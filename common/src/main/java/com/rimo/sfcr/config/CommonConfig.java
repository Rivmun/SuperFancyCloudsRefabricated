package com.rimo.sfcr.config;

import com.rimo.sfcr.util.CloudRefreshSpeed;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "sfcr")
public class CommonConfig extends CoreConfig implements ConfigData {
	//----GENERAL----
	private boolean enableMod = true;
	private boolean enableDebug = false;
	private boolean enableServerConfig = false;
	private int secPerSync = 60;
	private boolean enableFog = true;
	private boolean enableNormalCull = true;
	private float cullRadianMultiplier = 1.0f;
	private int rebuildInterval = 10;
	private boolean enableSmoothChange = false;
	//----CLOUDS----
	private int cloudRenderDistance = 96;
	private boolean cloudRenderDistanceFitToView = false;
	private CloudRefreshSpeed normalRefreshSpeed = CloudRefreshSpeed.SLOW;
	private boolean enableTerrainDodge = true;
	//----FOG----
	private boolean fogAutoDistance = true;
	private int fogMinDistance = 2;
	private int fogMaxDistance = 4;
	//----DYNAMIC----
	private CloudRefreshSpeed weatherRefreshSpeed = CloudRefreshSpeed.FAST;

	//output func.
	public boolean isEnableMod() {return enableMod;}
	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableServerConfig() {return enableServerConfig;}
	public int getSecPerSync() {return secPerSync;}
	public int getCloudRenderDistance() {return cloudRenderDistance;}
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public boolean isEnableNormalCull() {return enableNormalCull;}
	public float getCullRadianMultiplier() {return cullRadianMultiplier;}
	public int getRebuildInterval() {return rebuildInterval;}
	public boolean isEnableFog() {return enableFog;}
	public boolean isFogAutoDistance() {return fogAutoDistance;}
	public int getFogMinDistance() {return fogMinDistance;}
	public int getFogMaxDistance() {return fogMaxDistance;}
	public boolean isEnableSmoothChange() {return enableSmoothChange;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}

	//input func.
	public void setEnableMod(boolean isEnable) {enableMod = isEnable;}
	public void setEnableDebug(boolean isEnable) {enableDebug = isEnable;}
	public void setEnableServerConfig(boolean isEnable) {enableServerConfig = isEnable;}
	public void setSecPerSync(int value) {secPerSync = value; }
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setEnableTerrainDodge(boolean isEnable) {enableTerrainDodge = isEnable;}
	public void setEnableNormalCull(boolean isEnable) {enableNormalCull = isEnable;}
	public void setCullRadianMultiplier(float value) {cullRadianMultiplier = value;}
	public void setRebuildInterval(int value) {rebuildInterval = value;}
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
	public void setEnableSmoothChange(boolean isEnable) {enableSmoothChange = isEnable;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}

	//conversion
	public int getAutoFogMaxDistance() {
		return (int) (cloudRenderDistance / 48f * cloudRenderDistance / 48f * cloudBlockSize / 16f);
	}
}
