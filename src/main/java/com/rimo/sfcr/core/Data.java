package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerLevelAccessor;
import net.minecraft.core.BlockPos;
//? if ! 1.16.5
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ServerLevelData;

import static com.rimo.sfcr.Common.CONFIG;

public class Data {
	protected Weather currentWeather = Weather.CLEAR;
	protected Weather nextWeather = Weather.CLEAR;
	protected float densityByWeather = 0f;
	protected float densityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	private int normalRefreshSpeed;
	private int weatheringRefreshSpeed;
	private int densityChangingSpeed;

	public Data(Config config) {
		setConfig(config);
	}

	public int getResamplingInterval() {
		return ((isWeatherChange || isBiomeChange) ? weatheringRefreshSpeed : normalRefreshSpeed) / 5;
	}

	public void setConfig(Config config) {
		normalRefreshSpeed = config.getNormalRefreshSpeed().getValue();
		weatheringRefreshSpeed = config.getWeatherRefreshSpeed().getValue();
		densityChangingSpeed = config.getDensityChangingSpeed().getValue();
	}

	public boolean updateWeather(ServerLevel level) {
		// Weather Pre-detect
		ServerLevelData levelData = ((ServerLevelAccessor) level).getServerLevelData();
		int rainTime = levelData.getRainTime() / 20;
		int thunderTime = levelData.getThunderTime() / 20;
		int preDetectTime = CONFIG.getWeatherPreDetectTime();
		if (levelData.isRaining()) {
			if (levelData.isThundering()) {
				nextWeather = thunderTime < preDetectTime ? Weather.RAIN : Weather.THUNDER;
			} else {
				nextWeather = rainTime < preDetectTime ? Weather.CLEAR :
						thunderTime < preDetectTime ? Weather.THUNDER : Weather.RAIN;
			}
		} else {  //clear...
			int clearWeatherTime = levelData.getClearWeatherTime() / 20;
			if (clearWeatherTime != 0) {  // Notice that only '/weather clear' can set clearTime to non-zero
				nextWeather = clearWeatherTime < preDetectTime ? Weather.RAIN : Weather.CLEAR;
			} else {
				nextWeather = rainTime > preDetectTime ? Weather.CLEAR :
						rainTime < thunderTime ? Weather.RAIN : Weather.THUNDER;
			}
		}
		if (nextWeather != currentWeather) {
			currentWeather = nextWeather;
			return true;
		}
		return false;
	}

	/**
	 * Should run when connected to a server but without sync
	 */
	public void updateWeatherClient(Level level) {
		nextWeather = level.isThundering() ? Weather.THUNDER : level.isRaining() ? Weather.RAIN : Weather.CLEAR;
	}

	/**
	 * !! Only call in client because server is multiplayer that cannot ensure what pos will use.
	 */
	public void updateBiomeDensity(Player player) {
		Level level = player.level();
		boolean isDynamic = CONFIG.isEnableDynamic();
		boolean isBiomeByChunk = CONFIG.isBiomeDensityByChunk();

		if (isDynamic) {  //Detect Biome Change
			if (! isBiomeByChunk) {		//Hasn't effected if use chunk data.
				BlockPos pos = player.blockPosition();
				Holder<Biome> biome = level.getBiome(pos);
				if (CONFIG.isFilterListHasBiome(biome))
					targetDownFall = CONFIG.getDownfall(biome.value().getPrecipitationAt(pos, level.getSeaLevel()));
				isBiomeChange = densityByBiome != targetDownFall;
			}
		} else {
			isBiomeChange = false;
		}

		if (isDynamic) {  //Density Change by Biome
			if (! isBiomeByChunk) {
				densityByBiome = isBiomeChange ? stepDensity(targetDownFall, densityByBiome, densityChangingSpeed) : targetDownFall;
			} else {
				densityByBiome = 0.5f;		//Output common value if use chunk.
			}
		} else {
			densityByBiome = 0f;
		}
	}

	/**
	 * Run on both side, but be careful to run twice in single tick.
	 */
	public void updateWeatherDensity(Level level) {
		float thunderDensity = CONFIG.getThunderDensityPercent() / 100f;
		float rainDensity = CONFIG.getRainDensityPercent() / 100f;
		float clearDensity = CONFIG.getCloudDensityPercent() / 100f;
		boolean enableDynamic = CONFIG.isEnableDynamic();

		if (enableDynamic) {
			boolean isPreDetectOn = CONFIG.getWeatherPreDetectTime() != 0;
			if (level.isThundering()) {
				isWeatherChange = nextWeather != Weather.THUNDER && isPreDetectOn || densityByWeather < thunderDensity;
			} else if (level.isRaining()) {
				isWeatherChange = nextWeather != Weather.RAIN && isPreDetectOn || densityByWeather != rainDensity;
			} else {		//Clear...
				isWeatherChange = nextWeather != Weather.CLEAR && isPreDetectOn || densityByWeather > clearDensity;
			}
		} else {
			isWeatherChange = false;
		}

		//Density Change by Weather
		if (enableDynamic) {
			if (isWeatherChange) {
				switch (nextWeather) {
					case THUNDER: densityByWeather = stepDensity(thunderDensity, densityByWeather, densityChangingSpeed); break;
					case RAIN: densityByWeather = stepDensity(rainDensity, densityByWeather, densityChangingSpeed); break;
					case CLEAR: densityByWeather = stepDensity(clearDensity, densityByWeather, densityChangingSpeed); break;
				}
			} else {
				switch (nextWeather) {
					case THUNDER: densityByWeather = thunderDensity; break;
					case RAIN: densityByWeather = rainDensity; break;
					case CLEAR: densityByWeather = clearDensity; break;
				}
			}
		} else {		//Initialize if disabled detect in rain/thunder.
			densityByWeather = clearDensity;
		}
	}

	private float stepDensity(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ?
				(target > current ?
						current + 1f / speed :
						current - 1f / speed) :
				target;
	}

	public Weather getNextWeather() {
		return nextWeather;
	}

	public void setNextWeather(Weather nextWeather) {
		this.nextWeather = nextWeather;
	}

	public String getDebugString() {
		return "[SFCR] wc:" + isWeatherChange + ", bc:" + isBiomeChange + ", wd:" + densityByWeather + ", bd:" + densityByBiome;
	}

	public enum Weather {
		CLEAR,
		RAIN,
		THUNDER
	}

}
