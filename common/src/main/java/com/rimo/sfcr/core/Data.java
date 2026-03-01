package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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
		int rainTime = worldProperties.getRainTime();
		int thunderTime = worldProperties.getThunderTime();
		int preDetectTime = CONFIG.getWeatherPreDetectTime();
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				nextWeather = thunderTime / 20 < preDetectTime ? Weather.RAIN : Weather.THUNDER;
			} else {
				nextWeather = thunderTime / 20 < preDetectTime && thunderTime != rainTime ?
						Weather.THUNDER :
						rainTime / 20 < preDetectTime ? Weather.CLEAR : Weather.RAIN;
			}
		} else {
			int clearWeatherTime = worldProperties.getClearWeatherTime();
			if (clearWeatherTime != 0) {
				nextWeather = clearWeatherTime / 20 < preDetectTime ? Weather.RAIN : Weather.CLEAR;
			} else {
				nextWeather = Math.min(rainTime, thunderTime) / 20 < preDetectTime ?
						rainTime < thunderTime ? Weather.RAIN : Weather.THUNDER :
						Weather.CLEAR;
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
	public void updateWeatherClient(World world) {
		nextWeather = world.isThundering() ? Weather.THUNDER : world.isRaining() ? Weather.RAIN : Weather.CLEAR;
	}

	public void updateDensity(ClientPlayerEntity player) {
		World world = player.getWorld();

		//Detect Weather Change
		float thunderDensity = CONFIG.getThunderDensityPercent() / 100f;
		float rainDensity = CONFIG.getRainDensityPercent() / 100f;
		float clearDensity = CONFIG.getCloudDensityPercent() / 100f;
		boolean enableDynamic = CONFIG.isEnableWeatherDensity();
		boolean biomeDensityByChunk = CONFIG.isBiomeDensityByChunk();
		if (enableDynamic) {
			boolean isPreDetectOn = CONFIG.getWeatherPreDetectTime() != 0;
			if (world.isThundering()) {
				isWeatherChange = nextWeather != Weather.THUNDER && isPreDetectOn || densityByWeather < thunderDensity;
			} else if (world.isRaining()) {
				isWeatherChange = nextWeather != Weather.RAIN && isPreDetectOn || densityByWeather != rainDensity;
			} else {		//Clear...
				isWeatherChange = nextWeather != Weather.CLEAR && isPreDetectOn || densityByWeather > clearDensity;
			}
			//Detect Biome Change
			if (! biomeDensityByChunk) {		//Hasn't effected if use chunk data.
				BlockPos pos = player.getBlockPos();
				if (CONFIG.isFilterListHasBiome(world.getBiome(pos)))
					targetDownFall = CONFIG.getDownfall(world.getBiome(pos).value().getPrecipitation(pos));
				isBiomeChange = densityByBiome != targetDownFall;
			}
		} else {
			isWeatherChange = false;
			isBiomeChange = false;
		}

		//Density Change by Weather
		if (enableDynamic) {
			if (isWeatherChange) {
				switch (nextWeather) {
					case THUNDER -> densityByWeather = stepDensity(thunderDensity, densityByWeather, densityChangingSpeed);
					case RAIN -> densityByWeather = stepDensity(rainDensity, densityByWeather, densityChangingSpeed);
					case CLEAR -> densityByWeather = stepDensity(clearDensity, densityByWeather, densityChangingSpeed);
				}
			} else {
				switch (nextWeather) {
					case THUNDER -> densityByWeather = thunderDensity;
					case RAIN -> densityByWeather = rainDensity;
					case CLEAR -> densityByWeather = clearDensity;
				}
			}
			//Density Change by Biome
			if (! biomeDensityByChunk) {
				densityByBiome = isBiomeChange ? stepDensity(targetDownFall, densityByBiome, densityChangingSpeed) : targetDownFall;
			} else {
				densityByBiome = 0.5f;		//Output common value if use chunk.
			}
		} else {		//Initialize if disabled detect in rain/thunder.
			densityByWeather = clearDensity;
			densityByBiome = 0f;
		}

		if (CONFIG.isEnableDebug() && (isWeatherChange || isBiomeChange))
			player.sendMessage(Text.of("[SFCRe Debug] wc:" + isWeatherChange + ", bc:" + isBiomeChange + ", wd:" + densityByWeather + ", bd:" + densityByBiome), true);
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
