package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.Common;
//~ if neoforge 'fabric' -> 'neoforge'
import com.rimo.sfcr.loaders.fabric.Platform;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.rimo.sfcr.Common.MOD_ID;

public class SharedConfig {
	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final List<String> DEF_BIOME_FILTER_LIST = Arrays.asList(
			"#minecraft:is_river"
	);
	public static final List<String> DEF_SEASON_DENSITY_MAP = Arrays.asList(
			"MID_SUMMER=130,LATE_WINTER=70"
	);

	private boolean isEnableRender = true;
	protected boolean isEnableCloudRain = false;
	private int cloudRenderDistance = 31;
	private int cloudHeight = 0;
	private int cloudBlockSize = 12;
	private int cloudLayerThickness = 9;
	private boolean enableTerrainDodge = true;
	private int sampleSteps = 2;
	private int cloudColor = 0xB2FFFFFF;
	private boolean enableBottomDim = true;
	private boolean enableDuskBlush = true;
	private float densityThreshold = 1.3f;
	private float thresholdMaxReduction = 1.5f;
	private boolean enableDynamic = true;
	private int weatherPreDetectTime = 5;
	private int cloudDensityPercent = 25;
	private int rainDensityPercent = 60;
	private int thunderDensityPercent = 90;
	private float densityAtNight = 0.7F;
	private CloudRefreshSpeed normalRefreshSpeed = CloudRefreshSpeed.SLOW;
	private CloudRefreshSpeed weatherRefreshSpeed = CloudRefreshSpeed.FAST;
	private CloudRefreshSpeed densityChangingSpeed = CloudRefreshSpeed.SLOW;
	private int snowDensity = 60;
	private int rainDensity = 90;
	private int noneDensity = 0;
	private boolean isBiomeDensityByChunk = false;
	private List<String> biomeFilterList = DEF_BIOME_FILTER_LIST;
	private List<String> seasonDensityPercentMap = DEF_SEASON_DENSITY_MAP;

	public SharedConfig() {}

	public void set(SharedConfig config) {
		this.isEnableRender               = config.isEnableRender;
		this.isEnableCloudRain            = config.isEnableCloudRain;
		this.cloudRenderDistance          = config.cloudRenderDistance;
		this.cloudHeight                  = config.cloudHeight;
		this.cloudBlockSize               = config.cloudBlockSize;
		this.cloudLayerThickness          = config.cloudLayerThickness;
		this.enableTerrainDodge           = config.enableTerrainDodge;
		this.sampleSteps                  = config.sampleSteps;
		this.cloudColor                   = config.cloudColor;
		this.enableBottomDim              = config.enableBottomDim;
		this.enableDuskBlush              = config.enableDuskBlush;
		this.densityThreshold             = config.densityThreshold;
		this.thresholdMaxReduction        = config.thresholdMaxReduction;
		this.enableDynamic                = config.enableDynamic;
		this.weatherPreDetectTime         = config.weatherPreDetectTime;
		this.cloudDensityPercent          = config.cloudDensityPercent;
		this.rainDensityPercent           = config.rainDensityPercent;
		this.thunderDensityPercent        = config.thunderDensityPercent;
		this.densityAtNight               = config.densityAtNight;
		this.normalRefreshSpeed           = config.normalRefreshSpeed;
		this.weatherRefreshSpeed          = config.weatherRefreshSpeed;
		this.densityChangingSpeed         = config.densityChangingSpeed;
		this.snowDensity                  = config.snowDensity;
		this.rainDensity                  = config.rainDensity;
		this.noneDensity                  = config.noneDensity;
		this.isBiomeDensityByChunk        = config.isBiomeDensityByChunk;
		this.biomeFilterList              = config.biomeFilterList;
		this.seasonDensityPercentMap      = config.seasonDensityPercentMap;
	}

	public boolean isEnableRender() {return isEnableRender;}
	public int getCloudHeight() {return cloudHeight;}
	public int getCloudBlockSize() {return cloudBlockSize;}
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	public int getSampleSteps() {return sampleSteps;}
	public int getCloudColor() {return cloudColor;}
	public float getDensityThreshold() {return densityThreshold;}
	public float getThresholdMaxReduction() {return thresholdMaxReduction;}
	public boolean isEnableDynamic() {return enableDynamic;}
	public int getWeatherPreDetectTime() {return weatherPreDetectTime;}
	public int getCloudDensityPercent() {return cloudDensityPercent;}
	public int getRainDensityPercent() {return rainDensityPercent;}
	public int getThunderDensityPercent() {return thunderDensityPercent;}
	public float getDensityAtNight() {return densityAtNight;}
	public CloudRefreshSpeed getDensityChangingSpeed() {return densityChangingSpeed;}
	public int getSnowDensity() {return snowDensity;}
	public int getRainDensity() {return rainDensity;}
	public int getNoneDensity() {return noneDensity;}
	public boolean isBiomeDensityByChunk() {return isBiomeDensityByChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}
	public int getCloudRenderDistance() {return cloudRenderDistance;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public boolean isEnableBottomDim() {return this.enableBottomDim;}
	public boolean isEnableDuskBlush() {return this.enableDuskBlush;}
	public boolean isEnableCloudRain() {return isEnableCloudRain && isEnableRender;}
	public List<String> getSeasonDensityPercentMap() {return seasonDensityPercentMap;}

	public void setEnableRender(boolean isEnable) {
		isEnableRender = isEnable;}
	public void setCloudHeight(int height) {cloudHeight = height;}
	public void setCloudBlockSize(int size) {cloudBlockSize = size;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
	public void setCloudColor(int cloudColor) {this.cloudColor = cloudColor;}
	public void setDensityThreshold(float density) {densityThreshold = density;}
	public void setThresholdMaxReduction(float multiplier) {
		thresholdMaxReduction = multiplier;}
	public void setEnableDynamic(boolean isEnable) {
		enableDynamic = isEnable;}
	public void setWeatherPreDetectTime(int time) {weatherPreDetectTime = time;}
	public void setCloudDensityPercent(int density) {cloudDensityPercent = density;}
	public void setRainDensityPercent(int density) {rainDensityPercent = density;}
	public void setThunderDensityPercent(int density) {thunderDensityPercent = density;}
	public void setDensityAtNight(float density) {this.densityAtNight = density;}
	public void setDensityChangingSpeed(CloudRefreshSpeed speed) {densityChangingSpeed = speed;}
	public void setSnowDensity(int density) {snowDensity = density;}
	public void setRainDensity(int density) {rainDensity = density;}
	public void setNoneDensity(int density) {noneDensity = density;}
	public void setBiomeDensityByChunk(boolean isEnable) {isBiomeDensityByChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = Math.min(distance, 192);}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}
	public void setEnableTerrainDodge(boolean isEnable) {enableTerrainDodge = isEnable;}
	public void setEnableBottomDim(boolean enableBottomDim) {this.enableBottomDim = enableBottomDim;}
	public void setEnableDuskBlush(boolean enableDuskBlush) {this.enableDuskBlush = enableDuskBlush;}
	public void setEnableCloudRain(boolean enableCloudRain) {this.isEnableCloudRain = enableCloudRain;}
	public void setSeasonDensityPercentMap(List<String> list) {
		this.seasonDensityPercentMap = list;
		if (Common.seasonHandler != null)
			Common.seasonHandler.setDensityMapFromString(list.get(0));
	}

	public boolean isFilterListHasBiome(Holder<Biome> biome) {
		boolean isHas = false;
		if (this.getBiomeFilterList().contains(biome.unwrapKey().orElse(Biomes.THE_VOID).identifier().toString())) {
			isHas = true;
		} else {
			for (TagKey<Biome> tag : biome.tags().toList()) {
				if (this.getBiomeFilterList().contains("#" + tag.location().toString())) {
					isHas = true;
					break;
				}
			}
		}
		return isHas;
	}

	public float getDownfall(Biome.Precipitation i) {
		if (i.equals(Biome.Precipitation.SNOW)) {
			return this.getSnowDensity() / 100f;
		} else if (i.equals(Biome.Precipitation.RAIN)) {
			return this.getRainDensity() / 100f;
		} else {
			return this.getNoneDensity() / 100f;
		}
	}

	/**
	 * @return a SharedConfig JSON string
	 */
	@Override
	public String toString() {
		return GSON.toJson(this, SharedConfig.class);
	}

	/**
	 * @throws JsonSyntaxException if input string cannot convert to SharedConfig
	 * @return {@code this}
	 */
	public SharedConfig fromString(String s) throws JsonSyntaxException {
		SharedConfig config = GSON.fromJson(s, SharedConfig.class);
		if (config == null)
			throw new JsonSyntaxException("input is empty!");
		set(config);
		return this;
	}

	/*
	 * -----IO-----
	 */

	/**
	 * It must be {@code .minecraft/config/sfcr/sfcr.json} in normally.
	 */
	private static final Path DEFAULT_PATH = Platform.getConfigFolder().resolve(MOD_ID).resolve(MOD_ID + ".json");
	public static final String OVERWORLD = "minecraft:overworld";

	/**
	 * Trans dimensionName to specific config file path.<br>
	 * If param is {@code minecraft:overworld}, {@link #DEFAULT_PATH} will be present.
	 * @param dimensionName syntax like {@code minecraft:overworld}
	 * @return syntax like {@code .minecraft/config/sfcr/sfcr_modName_dimensionName.json}
	 */
	private static Path getDimensionConfigPath(String dimensionName) {
		if (dimensionName.equals(OVERWORLD))
			return DEFAULT_PATH;
		// .minecraft/config/sfcr/sfcr_modName_dimensionName.json
		dimensionName = "_" + dimensionName.replace(":", "_");
		return DEFAULT_PATH.getParent().resolve(MOD_ID + dimensionName + ".json");
	}

	/**
	 * Load dimensionName specific config then {@link #set} to this instance.<br>
	 * If specific config not exist, it'll load default config then {@link #set}.<br>
	 * File path like 'sfcr_modName_dimensionName.json'<br>
	 * Since x.9.2 we modify the config path, this func will automatically detect old file then move it into new path.
	 * @param dimensionNamespace syntax like "minecraft:overworld" from RegistryKey.getRegistry().getValue().toString()
	 * @return {@code true} if success to load dimension specific config,<br>{@code false} if not or dimensionName is {@code minecraft:overworld}.
	 */
	public boolean load(String dimensionNamespace) {
		Path path = getDimensionConfigPath(dimensionNamespace);
		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				_load(reader, path);
				return path != DEFAULT_PATH;
			} catch (IOException | JsonParseException e) {
				Common.LOGGER.error("{} failed to read config file: {}, is the file written by older version?", MOD_ID, path.getFileName());
				return false;
			}
		}
		Path path_1_9_1 = path.getParent().getParent().resolve(path.getFileName());
		if (Files.exists(path_1_9_1)) {
			try {
				Files.createDirectories(path.getParent());
				Files.move(path_1_9_1, path);  //move old config file to new folder
				return load(dimensionNamespace);
			} catch (IOException ignore) {}
		}
		if (path == DEFAULT_PATH)
			save(OVERWORLD);  //write default file
		return false;
	}

	protected void _load(BufferedReader reader, Path path) throws JsonSyntaxException {
		set(GSON.fromJson(reader, SharedConfig.class));
	}

	/**
	 * Write this config instance as a file into {@link #getDimensionConfigPath(String)}
	 * @param dimensionNamespace syntax like {@code minecraft:overworld} from {@code Level.dimension().location().toString()}
	 */
	public void save(String dimensionNamespace) {
		Path path = getDimensionConfigPath(dimensionNamespace);
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				if (path != DEFAULT_PATH)
					GSON.toJson(this, SharedConfig.class, writer);
				else
					GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Common.LOGGER.error("{} failed to write config file: {}", MOD_ID, path.getFileName());
		}
	}

	/**
	 * Delete specific dimension config file from {@link #getDimensionConfigPath(String)}
	 */
	public static void delete(String dimensionName) {
		try {
			Files.deleteIfExists(getDimensionConfigPath(dimensionName));
		} catch (IOException ignored) {}
	}

}
