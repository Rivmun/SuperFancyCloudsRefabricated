package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class Data {
	private final Config CONFIG;
	protected Weather currentWeather = Weather.CLEAR;
	protected Weather nextWeather = Weather.CLEAR;
	protected double time = 0.0;
	protected float densityByWeather = 0f;
	protected float densityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	private int normalRefreshSpeed;  //use to control rebuild interval
	private int weatheringRefreshSpeed;  //(same as above)
	private int densityChangingSpeed;

	Data(Config config) {
		CONFIG = config;
		setConfig(config);
	}

	public void setConfig(Config config) {
		normalRefreshSpeed = config.getRefreshSpeed().getValue();
		weatheringRefreshSpeed = config.getWeatherRefreshSpeed().getValue();
		densityChangingSpeed = config.getDensityChangingSpeed().getValue();
	}

	public void updateTime() {
		time += 1 / 20f;
	}

	public void updateWeather(World world) {
		if (world == null)
			return;
		nextWeather = world.isThundering() ? Weather.THUNDER :
				world.isRaining() ? Weather.RAIN :
						Weather.CLEAR;
	}

	public void updateDensity(PlayerEntity player) {
		if (player == null)
			return;
		World world = player.getWorld();
		if (world == null)
			return;

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
				if (!CONFIG.isFilterListHasBiome(world.getBiome(player.getBlockPos())))
					targetDownFall = world.getBiome(player.getBlockPos()).value().weather.downfall();
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
		return Math.abs(target - current) > 1f / speed ? (target > current ? current + 1f / speed : current - 1f / speed) : target;
	}

}
