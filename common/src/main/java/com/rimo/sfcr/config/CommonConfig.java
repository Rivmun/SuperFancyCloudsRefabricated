package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.util.CloudRefreshSpeed;
import me.shedaniel.architectury.platform.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommonConfig extends CoreConfig {
	public CommonConfig() {super();}
	//----GENERAL----
	protected boolean enableMod = true;
	protected boolean enableDebug = false;
	private boolean enableServerConfig = false;
	protected int secPerSync = 60;
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

	public void setCommonConfig(CommonConfig config) {		//TODO: Why do I need to write this sht...
		this.enableMod						= config.enableMod;
		this.enableDebug					= config.enableDebug;
		this.enableServerConfig				= config.enableServerConfig;
		this.secPerSync						= config.secPerSync;
		this.cloudRenderDistance			= config.cloudRenderDistance;
		this.cloudRenderDistanceFitToView	= config.cloudRenderDistanceFitToView;
		this.normalRefreshSpeed				= config.normalRefreshSpeed;
		this.enableTerrainDodge				= config.enableTerrainDodge;
		this.enableNormalCull				= config.enableNormalCull;
		this.cullRadianMultiplier			= config.cullRadianMultiplier;
		this.rebuildInterval				= config.rebuildInterval;
		this.enableFog						= config.enableFog;
		this.fogAutoDistance				= config.fogAutoDistance;
		this.fogMinDistance					= config.fogMinDistance;
		this.fogMaxDistance					= config.fogMaxDistance;
		this.enableSmoothChange				= config.enableSmoothChange;
		this.weatherRefreshSpeed			= config.weatherRefreshSpeed;
		this.setCoreConfig(config);
	}

	//conversion
	public int getAutoFogMaxDistance() {
		return (int) (cloudRenderDistance / 48f * cloudRenderDistance / 48f * cloudBlockSize / 16f);
	}

	//load
	public void load() {
		Gson gson = new Gson();
		Path path = Platform.getConfigFolder().resolve(SFCReMod.MOD_ID + ".json");
		if (Files.exists(path)) {
			try {
				BufferedReader reader = Files.newBufferedReader(path);
				CommonConfig config = gson.fromJson(reader, CommonConfig.class);
				reader.close();
				this.setCommonConfig(config);
			} catch (IOException | JsonParseException e) {
				SFCReMod.exceptionCatcher(e);
			}
		} else {
			this.save();
		}
	}

	//save
	public void save() {
		Gson gson = new Gson();
		Path path = Platform.getConfigFolder().resolve(SFCReMod.MOD_ID + ".json");
		try {
			Files.createDirectories(path.getParent());
			BufferedWriter writer = Files.newBufferedWriter(path);
			gson.toJson(this, writer);
			writer.close();
		} catch (IOException e) {
			SFCReMod.exceptionCatcher(e);
		}
	}
}
