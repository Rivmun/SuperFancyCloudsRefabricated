package com.rimo.sfcr.config;

import com.google.gson.JsonParseException;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.PlatformUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.rimo.sfcr.Common.MOD_ID;

public class Config extends SharedConfig {
	private boolean enableDebug = false;
	private boolean enableServer = true;
	private boolean enableViewCulling = false;
	private float cullRadianMultiplier = 1.2f;
	private int rebuildInterval = 10;
	private boolean enableSmoothChange = false;
	private boolean isEnableDHCompat = false;
	private float dhRenderRangeMultiplier = 1F;
	private boolean isEnableParticleRainCompat = false;
	private boolean isCloudRainLogically = false;

	/**
	 * Do you want to call .load() to read a local config?
	 */
	public Config() {}
	public void setConfig(Config config) {
		this.enableDebug                = config.enableDebug;
		this.enableServer               = config.enableServer;
		this.enableViewCulling          = config.enableViewCulling;
		this.cullRadianMultiplier       = config.cullRadianMultiplier;
		this.rebuildInterval            = config.rebuildInterval;
		this.enableSmoothChange         = config.enableSmoothChange;
		this.isEnableDHCompat           = config.isEnableDHCompat;
		this.dhRenderRangeMultiplier    = config.dhRenderRangeMultiplier;
		this.isEnableParticleRainCompat = config.isEnableParticleRainCompat;
		this.isCloudRainLogically       = config.isCloudRainLogically;
		setSharedConfig(config);
	}

	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableServer() {return enableServer;}
	public boolean getEnableViewCulling() {return enableViewCulling;}
	public float getCullRadianMultiplier() {return cullRadianMultiplier;}
	public int getRebuildInterval() {return rebuildInterval;}
	public boolean isEnableSmoothChange() {return enableSmoothChange;}
	public boolean isEnableDHCompat() {return isEnableDHCompat && Client.isDistantHorizonsLoaded;}
	public float getDhRenderRangeMultiplier() {return dhRenderRangeMultiplier;}
	public boolean isEnableParticleRainCompat() {return isEnableParticleRainCompat && isEnableRender();}
	public boolean isCloudRainLogically() {return isCloudRainLogically && isEnableCloudRain && enableServer;}

	public void setEnableDebug(boolean isEnable) {enableDebug = isEnable;}
	public void setEnableServer(boolean isEnable) {enableServer = isEnable;}
	public void setEnableViewCulling(boolean enableViewCulling) {this.enableViewCulling = enableViewCulling;}
	public void setCullRadianMultiplier(float value) {cullRadianMultiplier = value;}
	public void setRebuildInterval(int value) {rebuildInterval = value;}
	public void setEnableSmoothChange(boolean isEnable) {enableSmoothChange = isEnable;}
	public void setEnableDHCompat(boolean enableDHCompat) {isEnableDHCompat = enableDHCompat && Client.isDistantHorizonsLoaded;}
	public void setDhRenderRangeMultiplier(float value) {dhRenderRangeMultiplier = value;}
	public void setEnableParticleRainCompat(boolean enable) {isEnableParticleRainCompat = enable && isEnableCloudRain;}
	public void setCloudRainLogically(boolean enable) {this.isCloudRainLogically = enable && isEnableCloudRain && enableServer;}

	/*
	 * -----IO-----
	 */

	private static final Path DEFAULT_PATH = PlatformUtil.getConfigFolder().resolve(MOD_ID).resolve(MOD_ID + ".json");
	public static final String OVERWORLD = "minecraft:overworld";

	private static Path getDimensionConfigPath(String dimensionName) {
		if (dimensionName.equals(OVERWORLD))
			return DEFAULT_PATH;
		dimensionName = "_" + dimensionName.replace(":", "_");
		return DEFAULT_PATH.getParent().resolve(MOD_ID + dimensionName + ".json");
	}

	public Config load() {
		if (Files.exists(DEFAULT_PATH)) {
			load(OVERWORLD);
		} else {
			Path defaultPath_1_9_1 = DEFAULT_PATH.getParent().getParent().resolve(DEFAULT_PATH.getFileName());
			if (Files.exists(defaultPath_1_9_1)) {
				try {
					Files.copy(defaultPath_1_9_1, DEFAULT_PATH);  //copy old config file to new folder
					load(OVERWORLD);
				} catch (IOException ignore) {}
			} else {
				save();  //write default file
			}
		}
		return this;
	}

	/**
	 * Load dimensionName specific config then {@link #setConfig} to this instance.<br>
	 * If specific config not exist, it'll load default config then {@link #setConfig}.<br>
	 * File path like 'sfcr_modName_dimensionName.json'
	 * @param dimensionNamespace syntax like "minecraft:overworld" from RegistryKey.getRegistry().getValue().toString()
	 * @return {@code true} if success to load dimension specific config,<br>{@code false} if not or dimensionName is minecraft:overworld.
	 */
	public boolean load(String dimensionNamespace) {
		Path path = getDimensionConfigPath(dimensionNamespace);
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			setConfig(GSON.fromJson(reader, Config.class));
			if (isEnableDebug())
				Common.LOGGER.info("{} load config file: {}", MOD_ID, path.getFileName());
		} catch (IOException | JsonParseException e) {
			Common.LOGGER.error("{} failed to read config file: {}, is the file written by older version?", MOD_ID, path.getFileName());
			return false;
		}
		return path != DEFAULT_PATH;
	}

	public void save() {
		save(OVERWORLD);
	}

	/**
	 * Save config file to .minecraft/config/sfcr_modName_dimensionName.json
	 * @param dimensionNamespace syntax like "minecraft:overworld" from RegistryKey.getRegistry().getValue().toString()
	 */
	public void save(String dimensionNamespace) {
		Path path = getDimensionConfigPath(dimensionNamespace);
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Common.LOGGER.error("{} failed to write config file: {}", MOD_ID, path.getFileName());
		}
	}

	public static void delete(String dimensionName) {
		try {
			Files.deleteIfExists(getDimensionConfigPath(dimensionName));
		} catch (IOException ignored) {}
	}
}
