package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rimo.sfcr.Client;
import me.shedaniel.autoconfig.ConfigData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config implements ConfigData {
	public static final List<String> DEF_BIOME_BLACKLIST = new ArrayList<>(List.of(
			"#minecraft:is_river"
	));

	public int getCloudHeight() {
		return cloudHeight;
	}

	public void setCloudHeight(int cloudHeight) {
		this.cloudHeight = cloudHeight;
	}

	public int getCloudLayerHeight() {
		return cloudLayerHeight;
	}

	public void setCloudLayerHeight(int cloudLayerHeight) {
		this.cloudLayerHeight = cloudLayerHeight;
	}

	public int getRenderDistance() {
		return renderDistance;
	}

	public void setRenderDistance(int renderDistance) {
		this.renderDistance = renderDistance;
	}

	public boolean isEnableRenderDistanceFitToView() {
		return enableRenderDistanceFitToView;
	}

	public void setEnableRenderDistanceFitToView(boolean enableRenderDistanceFitToView) {
		this.enableRenderDistanceFitToView = enableRenderDistanceFitToView;
	}


	public int getSampleSteps() {
		return sampleSteps;
	}

	public void setSampleSteps(int sampleSteps) {
		this.sampleSteps = sampleSteps;
	}

	public boolean isEnableTerrainDodge() {
		return enableTerrainDodge;
	}

	public void setEnableTerrainDodge(boolean enableTerrainDodge) {
		this.enableTerrainDodge = enableTerrainDodge;
	}

	public float getDensityThreshold() {
		return densityThreshold;
	}

	public void setDensityThreshold(float densityThreshold) {
		this.densityThreshold = densityThreshold;
	}

	public float getThresholdMultiplier() {
		return thresholdMultiplier;
	}

	public void setThresholdMultiplier(float thresholdMultiplier) {
		this.thresholdMultiplier = thresholdMultiplier;
	}

	public int getDensityPercent() {
		return densityPercent;
	}

	public void setDensityPercent(int densityPercent) {
		this.densityPercent = densityPercent;
	}

	public boolean isEnableWeatherDensity() {
		return enableWeatherDensity;
	}

	public void setEnableWeatherDensity(boolean enableWeatherDensity) {
		this.enableWeatherDensity = enableWeatherDensity;
	}

	public int getRainDensityPercent() {
		return rainDensityPercent;
	}

	public void setRainDensityPercent(int rainDensityPercent) {
		this.rainDensityPercent = rainDensityPercent;
	}

	public int getThunderDensityPercent() {
		return thunderDensityPercent;
	}

	public void setThunderDensityPercent(int thunderDensityPercent) {
		this.thunderDensityPercent = thunderDensityPercent;
	}

	public int getWeatherPreDetectTime() {
		return weatherPreDetectTime;
	}

	public void setWeatherPreDetectTime(int weatherPreDetectTime) {
		this.weatherPreDetectTime = weatherPreDetectTime;
	}

	public RefreshSpeed getRefreshSpeed() {
		return refreshSpeed;
	}

	public void setRefreshSpeed(RefreshSpeed refreshSpeed) {
		this.refreshSpeed = refreshSpeed;
	}

	public RefreshSpeed getWeatherRefreshSpeed() {
		return weatherRefreshSpeed;
	}

	public void setWeatherRefreshSpeed(RefreshSpeed weatherRefreshSpeed) {
		this.weatherRefreshSpeed = weatherRefreshSpeed;
	}

	public RefreshSpeed getDensityChangingSpeed() {
		return densityChangingSpeed;
	}

	public void setDensityChangingSpeed(RefreshSpeed densityChangingSpeed) {
		this.densityChangingSpeed = densityChangingSpeed;
	}

	public int getBiomeDensityPercent() {
		return biomeDensityPercent;
	}

	public void setBiomeDensityPercent(int biomeDensityPercent) {
		this.biomeDensityPercent = biomeDensityPercent;
	}

	public boolean isEnableBiomeDensityByChunk() {
		return enableBiomeDensityByChunk;
	}

	public void setEnableBiomeDensityByChunk(boolean enableBiomeDensityByChunk) {
		this.enableBiomeDensityByChunk = enableBiomeDensityByChunk;
	}

	public boolean isEnableBiomeDensityUseLoadedChunk() {
		return enableBiomeDensityUseLoadedChunk;
	}

	public void setEnableBiomeDensityUseLoadedChunk(boolean enableBiomeDensityUseLoadedChunk) {
		this.enableBiomeDensityUseLoadedChunk = enableBiomeDensityUseLoadedChunk;
	}

	public List<String> getBiomeBlackList() {
		return biomeBlackList;
	}

	public void setBiomeBlackList(List<String> biomeBlackList) {
		this.biomeBlackList = biomeBlackList;
	}

	private int cloudHeight = -1;
	private int cloudLayerHeight = 8;
	private int renderDistance = 48;
	private boolean enableRenderDistanceFitToView = false;
	private int sampleSteps = 2;
	private boolean enableTerrainDodge = true;
	private float densityThreshold = 1.3f;
	private float thresholdMultiplier = 1.5f;
	private int densityPercent = 25;
	private boolean enableWeatherDensity = true;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private int weatherPreDetectTime = 10;
	private RefreshSpeed refreshSpeed = RefreshSpeed.SLOW;
	private RefreshSpeed weatherRefreshSpeed = RefreshSpeed.FAST;
	private RefreshSpeed densityChangingSpeed = RefreshSpeed.SLOW;
	private int biomeDensityPercent = 50;
	private boolean enableBiomeDensityByChunk = false;
	private boolean enableBiomeDensityUseLoadedChunk = false;
	private List<String> biomeBlackList = DEF_BIOME_BLACKLIST;

	public static Config load() {
		Gson gson = new Gson();
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Client.MOD_ID + ".json");
		Config config = new Config();
		if (Files.exists(path)) {
			try {
				BufferedReader reader = Files.newBufferedReader(path);
				config = gson.fromJson(reader, Config.class);
				reader.close();
			} catch (IOException | JsonParseException e) {
				Client.LOGGER.error("config file read failure!");
			}
		} else {
			save(config);
		}
		return config;
	}

	public static void save(Config config) {
		Gson gson = new Gson();
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Client.MOD_ID + ".json");
		try {
			Files.createDirectories(path.getParent());
			BufferedWriter writer = Files.newBufferedWriter(path);
			gson.toJson(config, writer);
			writer.close();
		} catch (IOException e) {
			Client.LOGGER.error("config file write failure!");
		}
	}

	public boolean isFilterListHasBiome(RegistryEntry<Biome> biome) {
		var isHas = false;
		if (this.getBiomeBlackList().contains(biome.getKey().get().getValue().toString())) {
			isHas = true;
		} else {
			for (TagKey<Biome> tag : biome.streamTags().toList()) {
				if (this.getBiomeBlackList().contains("#" + tag.id().toString())) {
					isHas = true;
					break;
				}
			}
		}
		return isHas;
	}

}
