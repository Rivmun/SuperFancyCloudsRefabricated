package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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

	public boolean updateWeather(ServerWorld world) {
		// Weather Pre-detect
		ServerWorldProperties worldProperties = ((ServerWorldAccessor) world).getWorldProperties();
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				setNextWeather(worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.RAIN : Weather.THUNDER);
			} else {
				setNextWeather(worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
						? Weather.THUNDER
						: worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.CLEAR : Weather.RAIN);
			}
		} else {
			if (worldProperties.getClearWeatherTime() != 0) {
				setNextWeather(worldProperties.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? Weather.RAIN : Weather.CLEAR);
			} else {
				setNextWeather(Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
						? worldProperties.getRainTime() < worldProperties.getThunderTime() ? Weather.RAIN : Weather.THUNDER
						: Weather.CLEAR);
			}
		}
		if (getNextWeather() != currentWeather) {
			currentWeather = getNextWeather();
			return true;
		}
		return false;
	}

	//Only runs when connected to a server without sync
	public void updateWeatherClient(World world) {
		if (!MinecraftClient.getInstance().isIntegratedServerRunning() && CONFIG.isEnableServerConfig())
			setNextWeather(world.isThundering() ? Weather.THUNDER : world.isRaining() ? Weather.RAIN : Weather.CLEAR);
	}

	public void updateDensity(ClientPlayerEntity player) {
		World world = player.getWorld();

		//Detect Weather Change
		if (CONFIG.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = getNextWeather() != Weather.THUNDER && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather < CONFIG.getThunderDensityPercent() / 100f;
			} else if (world.isRaining()) {
				isWeatherChange = getNextWeather() != Weather.RAIN && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather != CONFIG.getRainDensityPercent() / 100f;
			} else {		//Clear...
				isWeatherChange = getNextWeather() != Weather.CLEAR && CONFIG.getWeatherPreDetectTime() != 0
						|| densityByWeather > CONFIG.getCloudDensityPercent() / 100f;
			}
			//Detect Biome Change
			if (!CONFIG.isBiomeDensityByChunk()) {		//Hasn't effected if use chunk data.
				if (CONFIG.isFilterListHasBiome(world.getBiome(player.getBlockPos())))
					targetDownFall = CONFIG.getDownfall(world.getBiome(player.getBlockPos()).value().getPrecipitation(player.getBlockPos()));
				isBiomeChange = densityByBiome != targetDownFall;
			}
		} else {
			isWeatherChange = false;
			isBiomeChange = false;
		}

		//Density Change by Weather
		if (CONFIG.isEnableWeatherDensity()) {
			if (isWeatherChange) {
				switch (getNextWeather()) {
					case THUNDER -> densityByWeather = stepDensity(CONFIG.getThunderDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
					case RAIN -> densityByWeather = stepDensity(CONFIG.getRainDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
					case CLEAR -> densityByWeather = stepDensity(CONFIG.getCloudDensityPercent() / 100f, densityByWeather, densityChangingSpeed);
				}
			} else {
				switch (getNextWeather()) {
					case THUNDER -> densityByWeather = CONFIG.getThunderDensityPercent() / 100f;
					case RAIN -> densityByWeather = CONFIG.getRainDensityPercent() / 100f;
					case CLEAR -> densityByWeather = CONFIG.getCloudDensityPercent() / 100f;
				}
			}
			//Density Change by Biome
			if (!CONFIG.isBiomeDensityByChunk()) {
				densityByBiome = isBiomeChange ? stepDensity(targetDownFall, densityByBiome, densityChangingSpeed) : targetDownFall;
			} else {
				densityByBiome = 0.5f;		//Output common value if use chunk.
			}
		} else {		//Initialize if disabled detect in rain/thunder.
			densityByWeather = CONFIG.getCloudDensityPercent() / 100f;
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

	public Weather getNextWeather() {
		return nextWeather;
	}

	public void setNextWeather(Weather nextWeather) {
		this.nextWeather = nextWeather;
	}

	public enum Weather {
		CLEAR,
		RAIN,
		THUNDER
	}

}
