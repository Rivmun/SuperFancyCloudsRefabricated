package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;

import static com.rimo.sfcr.Common.CONFIG;

public class Data {
	private Weather currentWeather = Weather.CLEAR;
	public Weather nextWeather = Weather.CLEAR;
	protected float densityByWeather = 0f;
	protected float densityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	private int normalRefreshSpeed;  //use to control rebuild interval in old code
	private int weatheringRefreshSpeed;  //(same as above)
	private int densityChangingSpeed;

	public Data(Config config) {
		setConfig(config);
	}

	public int getResamplingInterval() {
		return ((isWeatherChange || isBiomeChange) ? weatheringRefreshSpeed : normalRefreshSpeed) / 5;
	}

	public void setConfig(Config config) {
		normalRefreshSpeed = config.getRefreshSpeed().getValue();
		weatheringRefreshSpeed = config.getWeatherRefreshSpeed().getValue();
		densityChangingSpeed = config.getDensityChangingSpeed().getValue();
	}

	// return true if weather changed
	public boolean updateWeather(MinecraftServer server) {
		// Weather Pre-detect
		WeatherData weatherData = server.getWeatherData();
		if (weatherData.isRaining()) {
			if (weatherData.isThundering()) {
				nextWeather = weatherData.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.RAIN : Data.Weather.THUNDER;
			} else {
				nextWeather = weatherData.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && weatherData.getThunderTime() != weatherData.getRainTime()
						? Data.Weather.THUNDER
						: weatherData.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.CLEAR : Data.Weather.RAIN;
			}
		} else {
			if (weatherData.getClearWeatherTime() != 0) {
				nextWeather = weatherData.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.RAIN : Data.Weather.CLEAR;
			} else {
				nextWeather = Math.min(weatherData.getRainTime(), weatherData.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
						? weatherData.getRainTime() < weatherData.getThunderTime() ? Data.Weather.RAIN : Data.Weather.THUNDER
						: Data.Weather.CLEAR;
			}
		}
		if (currentWeather != nextWeather) {
			currentWeather = nextWeather;
			return true;
		}
		return false;
	}

	//only runs when connect to dedicated server without sfcr
	public void updateWeatherClient(Level world) {
		if (world == null)
			return;
		nextWeather = world.isThundering() ? Weather.THUNDER :
				world.isRaining() ? Weather.RAIN :
						Weather.CLEAR;
	}

	public void updateDensity(Player player) {
		if (player == null)
			return;
		Level world = player.level();

		//Detect Weather Change
		if (CONFIG.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = nextWeather != Weather.THUNDER && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather < CONFIG.getThunderDensityPercent() / 100f;
			} else if (world.isRaining()) {
				isWeatherChange = nextWeather != Weather.RAIN && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather != CONFIG.getRainDensityPercent() / 100f;
			} else {		//Clear...
				isWeatherChange = nextWeather != Weather.CLEAR && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather > CONFIG.getDensityPercent() / 100f;
			}
			//Detect Biome Change
			if (!CONFIG.isEnableBiomeDensityByChunk()) {		//Hasn't effected if use chunk data.
				if (CONFIG.isFilterListHasNoBiome(world.getBiome(player.blockPosition())))
					targetDownFall = world.getBiome(player.blockPosition()).value().climateSettings.downfall;
				isBiomeChange = densityByBiome != targetDownFall;
			}
		} else {
			isWeatherChange = false;
			isBiomeChange = false;
		}

		//Density Change by Weather
		if (CONFIG.isEnableWeatherDensity()) {
			if (isWeatherChange) {
				switch (nextWeather) {
					case THUNDER -> densityByWeather = stepDensity(CONFIG.getThunderDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
					case RAIN -> densityByWeather = stepDensity(CONFIG.getRainDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
					case CLEAR -> densityByWeather = stepDensity(CONFIG.getDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
				}
			} else {
				switch (nextWeather) {
					case THUNDER -> densityByWeather = CONFIG.getThunderDensityPercent() / 100f;
					case RAIN -> densityByWeather = CONFIG.getRainDensityPercent() / 100f;
					case CLEAR -> densityByWeather = CONFIG.getDensityPercent() / 100f;
				}
			}
			//Density Change by Biome
			if (!CONFIG.isEnableBiomeDensityByChunk()) {
				densityByBiome = isBiomeChange ? stepDensity(targetDownFall, densityByBiome, densityChangingSpeed) : targetDownFall;
			} else {
				densityByBiome = 0.5f;		//Output common value if use chunk.
			}
		} else {		//Initialize if disabled detect in rain/thunder.
			densityByWeather = CONFIG.getDensityPercent() / 100f;
			densityByBiome = 0f;
		}
	}

	private float stepDensity(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ?
				(target > current ?
						current + 1f / speed :
						current - 1f / speed) :
				target;
	}

	public enum Weather {
		CLEAR,
		RAIN,
		THUNDER
	}
}
