package com.rimo.sfcr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.Client;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.ArrayList;
import java.util.List;

public class SharedConfig {
	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final List<String> DEF_BIOME_FILTER_LIST = new ArrayList<>(List.of(
			"#minecraft:is_river"
	));

	private boolean isEnableRender = true;
	private boolean isEnableCloudRain = false;
	private int cloudRenderDistance = 31;
	private boolean cloudRenderDistanceFitToView = false;
	private int cloudHeightOffset = 0;
	private int cloudLayerThickness = 10;
	private boolean enableTerrainDodge = true;
	private int sampleSteps = 2;
	private int cloudColor = 0xFFFFFFFF;
	private boolean enableBottomDim = true;
	private boolean enableDuskBlush = true;
	private float densityThreshold = 1.3f;
	private float thresholdMultiplier = 1.5f;
	private boolean enableWeatherDensity = true;
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
	private boolean isBiomeDensityUseLoadedChunk = false;
	private List<String> biomeFilterList = DEF_BIOME_FILTER_LIST;

	public SharedConfig() {}
	public void setSharedConfig(SharedConfig config) {
		this.isEnableRender               = config.isEnableRender;
		this.isEnableCloudRain            = config.isEnableCloudRain;
		this.cloudRenderDistance          = config.cloudRenderDistance;
		this.cloudRenderDistanceFitToView = config.cloudRenderDistanceFitToView;
		this.cloudHeightOffset            = config.cloudHeightOffset;
		this.cloudLayerThickness          = config.cloudLayerThickness;
		this.enableTerrainDodge           = config.enableTerrainDodge;
		this.sampleSteps                  = config.sampleSteps;
		this.cloudColor                   = config.cloudColor;
		this.enableBottomDim              = config.enableBottomDim;
		this.enableDuskBlush              = config.enableDuskBlush;
		this.densityThreshold             = config.densityThreshold;
		this.thresholdMultiplier          = config.thresholdMultiplier;
		this.enableWeatherDensity         = config.enableWeatherDensity;
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
		this.isBiomeDensityUseLoadedChunk = config.isBiomeDensityUseLoadedChunk;
		this.biomeFilterList              = config.biomeFilterList;
	}

	public boolean isEnableRender() {return isEnableRender && ! Client.isIrisLoadedShader;}
	public int getCloudHeightOffset() {return cloudHeightOffset;}
	public int getCloudLayerThickness() {return cloudLayerThickness;}
	public int getSampleSteps() {return sampleSteps;}
	public int getCloudColor() {return cloudColor;}
	public float getDensityThreshold() {return densityThreshold;}
	public float getThresholdMultiplier() {return thresholdMultiplier;}
	public boolean isEnableWeatherDensity() {return enableWeatherDensity;}
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
	public boolean isBiomeDensityUseLoadedChunk() {return isBiomeDensityUseLoadedChunk;}
	public List<String> getBiomeFilterList() {return biomeFilterList;}
	public int getCloudRenderDistance() {return cloudRenderDistance;}
	public boolean isCloudRenderDistanceFitToView() {return cloudRenderDistanceFitToView;}
	public CloudRefreshSpeed getNormalRefreshSpeed() {return normalRefreshSpeed;}
	public boolean isEnableTerrainDodge() {return enableTerrainDodge;}
	public CloudRefreshSpeed getWeatherRefreshSpeed() {return weatherRefreshSpeed;}
	public boolean isEnableBottomDim() {return this.enableBottomDim;}
	public boolean isEnableDuskBlush() {return this.enableDuskBlush;}
	public boolean isEnableCloudRain() {return isEnableCloudRain && isEnableRender;}

	public void setEnableRender(boolean isEnable) {isEnableRender = isEnable && ! Client.isIrisLoadedShader;}
	public void setCloudHeightOffset(int height) {cloudHeightOffset = height;}
	public void setCloudLayerThickness(int thickness) {cloudLayerThickness = thickness;}
	public void setSampleSteps(int steps) {sampleSteps = steps;}
	public void setCloudColor(int cloudColor) {this.cloudColor = cloudColor;}
	public void setDensityThreshold(float density) {densityThreshold = density;}
	public void setThresholdMultiplier(float multiplier) {thresholdMultiplier = multiplier;}
	public void setEnableWeatherDensity(boolean isEnable) {enableWeatherDensity = isEnable;}
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
	public void setBiomeDensityUseLoadedChunk(boolean isEnable) {isBiomeDensityUseLoadedChunk = isEnable;}
	public void setBiomeFilterList(List<String> list) {biomeFilterList = list;}
	public void setCloudRenderDistance(int distance) {cloudRenderDistance = distance;}
	public void setCloudRenderDistanceFitToView(boolean isEnable) {cloudRenderDistanceFitToView = isEnable;}
	public void setNormalRefreshSpeed(CloudRefreshSpeed speed) {normalRefreshSpeed = speed;}
	public void setWeatherRefreshSpeed(CloudRefreshSpeed speed) {weatherRefreshSpeed = speed;}
	public void setEnableTerrainDodge(boolean isEnable) {enableTerrainDodge = isEnable;}
	public void setEnableBottomDim(boolean enableBottomDim) {this.enableBottomDim = enableBottomDim;}
	public void setEnableDuskBlush(boolean enableDuskBlush) {this.enableDuskBlush = enableDuskBlush;}
	public void setEnableCloudRain(boolean enableCloudRain) {this.isEnableCloudRain = enableCloudRain;}

	public boolean isFilterListHasBiome(Holder<Biome> biome) {
		var isHas = false;
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
	 */
	public void fromString(String s) throws JsonSyntaxException {
		setSharedConfig(GSON.fromJson(s, SharedConfig.class));
	}
}
