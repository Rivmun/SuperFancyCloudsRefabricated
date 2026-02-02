package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
	private boolean enableMod = true;
	private int cloudHeightOffset = 0;
	private int cloudThickness = 34;
	private int renderDistance = 31;
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
	private boolean enableBottomDim = true;
	private boolean enableDynamic = true;
	private boolean enableDebug = false;
	private int cloudColor = 0xFFFFFF;
	private boolean enableDuskBlush = true;
	private boolean enableDHCompat = false;
	private int dhDistanceMultipler = 1;
	private int dhHeightEnhance = 0;

	public boolean isEnableMod() {return enableMod;}
	public void setEnableMod(boolean enableMod) {this.enableMod = enableMod;}
	public int getCloudHeightOffset() {return cloudHeightOffset;}
	public void setCloudHeightOffset(int cloudHeightOffset) {this.cloudHeightOffset = cloudHeightOffset;}
	public int getCloudThickness() {return cloudThickness;}
	public void setCloudThickness(int cloudThickness) {this.cloudThickness = cloudThickness;}
	public int getRenderDistance() {return renderDistance;}
	public void setRenderDistance(int renderDistance) {this.renderDistance = renderDistance;}
	public boolean isEnableRenderDistanceFitToView() {return enableRenderDistanceFitToView;}
	public void setEnableRenderDistanceFitToView(boolean enableRenderDistanceFitToView) {this.enableRenderDistanceFitToView = enableRenderDistanceFitToView;}
	public int getSampleSteps() {return sampleSteps;}
	public void setSampleSteps(int sampleSteps) {this.sampleSteps = sampleSteps;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public void setEnableTerrainDodge(boolean enableTerrainDodge) {this.enableTerrainDodge = enableTerrainDodge;}
	public float getDensityThreshold() {return densityThreshold;}
	public void setDensityThreshold(float densityThreshold) {this.densityThreshold = densityThreshold;}
	public float getThresholdMultiplier() {return thresholdMultiplier;}
	public void setThresholdMultiplier(float thresholdMultiplier) {this.thresholdMultiplier = thresholdMultiplier;}
	public int getDensityPercent() {return densityPercent;}
	public void setDensityPercent(int densityPercent) {this.densityPercent = densityPercent;}
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
	public void setEnableWeatherDensity(boolean enableWeatherDensity) {this.enableWeatherDensity = enableWeatherDensity;}
	public int getRainDensityPercent() {return rainDensityPercent;}
	public void setRainDensityPercent(int rainDensityPercent) {this.rainDensityPercent = rainDensityPercent;}
	public int getThunderDensityPercent() {return thunderDensityPercent;}
	public void setThunderDensityPercent(int thunderDensityPercent) {this.thunderDensityPercent = thunderDensityPercent;}
	public int getWeatherPreDetectTime() {return weatherPreDetectTime;}
	public void setWeatherPreDetectTime(int weatherPreDetectTime) {this.weatherPreDetectTime = weatherPreDetectTime;}
	public RefreshSpeed getRefreshSpeed() {return refreshSpeed;}
	public void setRefreshSpeed(RefreshSpeed refreshSpeed) {this.refreshSpeed = refreshSpeed;}
	public RefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public void setWeatherRefreshSpeed(RefreshSpeed weatherRefreshSpeed) {this.weatherRefreshSpeed = weatherRefreshSpeed;}
	public RefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public void setDensityChangingSpeed(RefreshSpeed densityChangingSpeed) {this.densityChangingSpeed = densityChangingSpeed;}
	public int getBiomeDensityPercent() {return biomeDensityPercent;}
	public void setBiomeDensityPercent(int biomeDensityPercent) {this.biomeDensityPercent = biomeDensityPercent;}
	public boolean isEnableBiomeDensityByChunk() {return enableBiomeDensityByChunk;}
	public void setEnableBiomeDensityByChunk(boolean enableBiomeDensityByChunk) {this.enableBiomeDensityByChunk = enableBiomeDensityByChunk;}
	public boolean isEnableBiomeDensityUseLoadedChunk() {return enableBiomeDensityUseLoadedChunk;}
	public void setEnableBiomeDensityUseLoadedChunk(boolean enableBiomeDensityUseLoadedChunk) {this.enableBiomeDensityUseLoadedChunk = enableBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeBlackList() {return biomeBlackList;}
	public void setBiomeBlackList(List<String> biomeBlackList) {this.biomeBlackList = biomeBlackList;}
	public boolean isEnableDebug() {return enableDebug;}
	public void setEnableDebug(boolean enableDebug) {this.enableDebug = enableDebug;}
	public boolean isEnableBottomDim() {return enableBottomDim;}
	public void setEnableBottomDim(boolean enableBottomDim) {this.enableBottomDim = enableBottomDim;}
	public boolean isEnableDynamic() {return enableDynamic;}
	public void setEnableDynamic(boolean enableDynamic) {this.enableDynamic = enableDynamic;}
	public int getCloudColor() {return cloudColor;}
	public void setCloudColor(int cloudColor) {this.cloudColor = cloudColor;}
	public boolean isEnableDuskBlush() {return enableDuskBlush;}
	public void setEnableDuskBlush(boolean enableDuskBlush) {this.enableDuskBlush = enableDuskBlush;}
	public boolean isEnableDHCompat() {return enableDHCompat && Client.isDistantHorizonsLoaded;}
	public void setEnableDHCompat(boolean enableDHCompat) {this.enableDHCompat = enableDHCompat && Client.isDistantHorizonsLoaded;}
	public int getDhDistanceMultipler() {return dhDistanceMultipler;}
	public void setDhDistanceMultipler(int dhDistanceMultipler) {this.dhDistanceMultipler = dhDistanceMultipler;}
	public int getDhHeightEnhance() {return dhHeightEnhance;}
	public void setDhHeightEnhance(int dhHeightEnhance) {this.dhHeightEnhance = dhHeightEnhance;}

	private static final String OVERWORLD = "minecraft:overworld";
	private static final Path DEFAULT_PATH = FabricLoader.getInstance().getConfigDir().resolve(Common.MOD_ID + ".json");
	public static final List<String> DEF_BIOME_BLACKLIST = new ArrayList<>(List.of("#minecraft:is_river"));

	public static Config load() {
		return load(OVERWORLD);  //load default
	}

	public static Config load(String dimensionNamespace) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();  //gson.setPrettyPrinting() can output highly readable file.
		Config config = new Config();
		Path path = DEFAULT_PATH;
		Client.isCustomDimensionConfig = false;
		if (!Files.exists(path))
			save(config);  //write default file if not exist.
		if (! dimensionNamespace.equals(OVERWORLD)) {
			dimensionNamespace = "_" + dimensionNamespace.replace(":", "_");  //sfcr_modName_dimensionName.json
			Path path2 = FabricLoader.getInstance().getConfigDir().resolve(Common.MOD_ID + dimensionNamespace + ".json");
			if (Files.exists(path2)) {
				path = path2;  //load dimension config if exist, or load default (path unmodified if not exist)
				Client.isCustomDimensionConfig = true;
			}
		}
		try {
			BufferedReader reader = Files.newBufferedReader(path);
			config = gson.fromJson(reader, Config.class);
			reader.close();
		} catch (IOException | JsonParseException e) {
			Common.LOGGER.error("Failed to read config file: {}", path.getFileName());
		}
		Common.LOGGER.info("Load config file: {}", path.getFileName());
		return config;
	}

	public static void save(Config config) {
		save(config, OVERWORLD);  //save default
	}

	public static void save(Config config, String dimensionNamespace) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Path path = FabricLoader.getInstance().getConfigDir();
		if (! dimensionNamespace.equals(OVERWORLD)) {
			dimensionNamespace = "_" + dimensionNamespace.replace(":", "_");
			path = path.resolve(Common.MOD_ID + dimensionNamespace + ".json");
		} else
			path = path.resolve(Common.MOD_ID + ".json");
		try {
			Files.createDirectories(path.getParent());
			BufferedWriter writer = Files.newBufferedWriter(path);
			gson.toJson(config, writer);
			writer.close();
		} catch (IOException e) {
			Common.LOGGER.error("Failed to write config file: {}", path.getFileName());
		}
	}

	public boolean isFilterListHasNoBiome(Holder<Biome> biome) {
		boolean isHas = false;
		if (this.getBiomeBlackList().contains(biome.unwrapKey().orElse(Biomes.THE_VOID).identifier().toString())) {
			isHas = true;
		} else {
			for (TagKey<Biome> tag : biome.tags().toList()) {
				if (this.getBiomeBlackList().contains("#" + tag.location())) {
					isHas = true;
					break;
				}
			}
		}
		return ! isHas;
	}

}
