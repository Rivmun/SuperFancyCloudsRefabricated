package com.rimo.sfcr.config;

import com.google.gson.JsonParseException;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import dev.architectury.platform.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.rimo.sfcr.Common.MOD_ID;

public class Config extends SharedConfig {
	private boolean enableDebug = false;
	private boolean enableServer = false;
	private CullMode cullMode = CullMode.RECTANGULAR;
	private float cullRadianMultiplier = 1.0f;
	private int rebuildInterval = 10;
	private boolean enableSmoothChange = false;
	private boolean isEnableDHCompat = false;

	/**
	 * Do you want to call .load() to read a local config?
	 */
	public Config() {}
	public void setConfig(Config config) {
		this.enableDebug                  = config.enableDebug;
		this.enableServer                 = config.enableServer;
		this.cullMode                     = config.cullMode;
		this.cullRadianMultiplier         = config.cullRadianMultiplier;
		this.rebuildInterval              = config.rebuildInterval;
		this.enableSmoothChange           = config.enableSmoothChange;
		this.isEnableDHCompat             = config.isEnableDHCompat;
		setSharedConfig(config);
	}

	public boolean isEnableDebug() {return enableDebug;}
	public boolean isEnableServer() {return enableServer;}
	public CullMode getCullMode() {return cullMode;}
	public float getCullRadianMultiplier() {return cullRadianMultiplier;}
	public int getRebuildInterval() {return rebuildInterval;}
	public boolean isEnableSmoothChange() {return enableSmoothChange;}
	public boolean isEnableDHCompat() {return isEnableDHCompat && Client.isDistantHorizonsLoaded;}

	public void setEnableDebug(boolean isEnable) {enableDebug = isEnable;}
	public void setEnableServer(boolean isEnable) {
		enableServer = isEnable;}
	public void setCullMode(CullMode cullMode) {this.cullMode = cullMode;}
	public void setCullRadianMultiplier(float value) {cullRadianMultiplier = value;}
	public void setRebuildInterval(int value) {rebuildInterval = value;}
	public void setEnableSmoothChange(boolean isEnable) {enableSmoothChange = isEnable;}
	public void setEnableDHCompat(boolean enableDHCompat) {isEnableDHCompat = enableDHCompat && Client.isDistantHorizonsLoaded;}

	/*
	 * -----IO-----
	 */

	private static final Path DEFAULT_PATH = Platform.getConfigFolder().resolve(Common.MOD_ID + ".json");
	public static final String OVERWORLD = "minecraft:overworld";

	public Config load() {
		load(OVERWORLD);
		return this;
	}

	/**
	 * Load dimensionName specific config then setConfig to this instance.<br>
	 * If specific config not exist, it'll load default config then setConfig.<br>
	 * File path like sfcr_modName_dimensionName.json
	 * @param dimensionNamespace syntax like "minecraft:overworld" from RegistryKey.getRegistry().getValue().toString()
	 * @return true if success to load dimension specific config, false if not.
	 */
	public boolean load(String dimensionNamespace) {
		Path path = DEFAULT_PATH;
		if (!Files.exists(path))
			save();  //write default file if not exist.
		if (! dimensionNamespace.equals(OVERWORLD)) {
			dimensionNamespace = "_" + dimensionNamespace.replace(":", "_");
			Path path2 = Platform.getConfigFolder().resolve(Common.MOD_ID + dimensionNamespace + ".json");
			if (Files.exists(path2)) {
				path = path2;  //load dimension config if exists, or load default (path unmodified if not exist)
			} else {
				return false;
			}
		}
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			setConfig(GSON.fromJson(reader, Config.class));
		} catch (IOException | JsonParseException e) {
			Common.LOGGER.error("{} failed to read config file: {}", MOD_ID, path.getFileName());
			return false;
		}
		Common.LOGGER.info("{} load config file: {}", MOD_ID, path.getFileName());
		return true;
	}

	public void save() {
		save(OVERWORLD);
	}

	/**
	 * Save config file to .minecraft/config/sfcr_modName_dimensionName.json
	 * @param dimensionNamespace syntax like "minecraft:overworld" from RegistryKey.getRegistry().getValue().toString()
	 */
	public void save(String dimensionNamespace) {
		Path path = DEFAULT_PATH;
		if (! dimensionNamespace.equals(OVERWORLD)) {
			dimensionNamespace = "_" + dimensionNamespace.replace(":", "_");
			path = path.getParent().resolve(Common.MOD_ID + dimensionNamespace + ".json");
		}
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Common.LOGGER.error("{} failed to write config file: {}", MOD_ID, path.getFileName());
		}
	}

}
