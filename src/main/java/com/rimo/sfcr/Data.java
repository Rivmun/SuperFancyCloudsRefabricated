package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.level.ServerWorldProperties;

import static com.rimo.sfcr.Common.CONFIG;

public class Data {
	protected Weather currentWeather = Weather.CLEAR;
	protected Weather nextWeather = Weather.CLEAR;
	protected float densityByWeather = 0f;
	protected float densityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	private int normalRefreshSpeed;  //use to control rebuild interval in old code
	private int weatheringRefreshSpeed;  //(same as above)
	private int densityChangingSpeed;

	Data(Config config) {
		setConfig(config);
	}

	public void setConfig(Config config) {
		normalRefreshSpeed = config.getRefreshSpeed().getValue();
		weatheringRefreshSpeed = config.getWeatherRefreshSpeed().getValue();
		densityChangingSpeed = config.getDensityChangingSpeed().getValue();
	}

	// return true if weather changed
	public boolean updateWeather(ServerWorld world) {
		// Weather Pre-detect
		ServerWorldProperties worldProperties = ((ServerWorldAccessor) world).getWorldProperties();
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.RAIN : Data.Weather.THUNDER;
			} else {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
						? Data.Weather.THUNDER
						: worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.CLEAR : Data.Weather.RAIN;
			}
		} else {
			if (worldProperties.getClearWeatherTime() != 0) {
				nextWeather = worldProperties.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Data.Weather.RAIN : Data.Weather.CLEAR;
			} else {
				nextWeather = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
						? worldProperties.getRainTime() < worldProperties.getThunderTime() ? Data.Weather.RAIN : Data.Weather.THUNDER
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
	public void updateWeatherClient(World world) {
		if (world == null)
			return;
		nextWeather = world.isThundering() ? Weather.THUNDER :
				world.isRaining() ? Weather.RAIN :
						Weather.CLEAR;
	}

	public void updateDensity(PlayerEntity player) {
		if (player == null)
			return;
		World world = player.getEntityWorld();
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
				if (CONFIG.isFilterListHasNoBiome(world.getBiome(player.getBlockPos())))
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

	public enum Weather {
		CLEAR,
		RAIN,
		THUNDER
	}
}
