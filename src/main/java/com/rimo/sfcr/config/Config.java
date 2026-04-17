package com.rimo.sfcr.config;

import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;

import java.io.BufferedReader;
import java.nio.file.Path;

import static com.rimo.sfcr.Common.MOD_ID;

public class Config extends SharedConfig {
	private boolean enableDebug = false;
	private boolean enableServer = true;
	private boolean enableViewCulling = true;
	private float cullRadianMultiplier = 1.1f;
	private int rebuildInterval = 10;
	private boolean enableSmoothChange = false;
	private boolean isEnableDHCompat = false;
	private boolean isEnableParticleRainCompat = false;
	private boolean isCloudRainLogically = false;

	/**
	 * Do you want to call {@link #load()} to read a local config?
	 */
	public Config() {}

	public void set(Config config) {
		this.enableDebug                = config.enableDebug;
		this.enableServer               = config.enableServer;
		this.enableViewCulling          = config.enableViewCulling;
		this.cullRadianMultiplier       = config.cullRadianMultiplier;
		this.rebuildInterval            = config.rebuildInterval;
		this.enableSmoothChange         = config.enableSmoothChange;
		this.isEnableDHCompat           = config.isEnableDHCompat;
		this.isEnableParticleRainCompat = config.isEnableParticleRainCompat;
		this.isCloudRainLogically       = config.isCloudRainLogically;
		super.set(config);
	}

	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableServer() {return enableServer;}
	public boolean getEnableViewCulling() {return enableViewCulling;}
	public float getCullRadianMultiplier() {return cullRadianMultiplier;}
	public int getRebuildInterval() {return rebuildInterval;}
	public boolean isEnableSmoothChange() {return enableSmoothChange;}
	public boolean isEnableDHCompat() {return isEnableDHCompat && Client.isDistantHorizonsLoaded;}
	public boolean isEnableParticleRainCompat() {return isEnableParticleRainCompat && isEnableRender();}
	public boolean isCloudRainLogically() {return isCloudRainLogically && isEnableCloudRain && enableServer;}

	public void setEnableDebug(boolean isEnable) {enableDebug = isEnable;}
	public void setEnableServer(boolean isEnable) {enableServer = isEnable;}
	public void setEnableViewCulling(boolean enableViewCulling) {this.enableViewCulling = enableViewCulling;}
	public void setCullRadianMultiplier(float value) {cullRadianMultiplier = value;}
	public void setRebuildInterval(int value) {rebuildInterval = value;}
	public void setEnableSmoothChange(boolean isEnable) {enableSmoothChange = isEnable;}
	public void setEnableDHCompat(boolean enableDHCompat) {isEnableDHCompat = enableDHCompat && Client.isDistantHorizonsLoaded;}
	public void setEnableParticleRainCompat(boolean enable) {isEnableParticleRainCompat = enable && isEnableCloudRain;}
	public void setCloudRainLogically(boolean enable) {this.isCloudRainLogically = enable && isEnableCloudRain && enableServer;}

	/**
	 * Load {@link #DEFAULT_PATH} file into this instance, or write this instance into {@link #DEFAULT_PATH} file if the file isn't exist.
	 * @return {@code this}
	 */
	public Config load() {
		load(OVERWORLD);
		return this;
	}

	@Override
	protected void _load(BufferedReader reader, Path path) throws JsonSyntaxException {
		set(GSON.fromJson(reader, Config.class));
		if (isEnableDebug())
			Common.LOGGER.info("{} load config file: {}", MOD_ID, path.getFileName());
	}

	/**
	 * Write this config instance as a file into {@link #DEFAULT_PATH}
	 */
	public void save() {
		save(OVERWORLD);
	}
}
