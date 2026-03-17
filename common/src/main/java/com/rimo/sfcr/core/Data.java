package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ServerWorldProperties;

import static com.rimo.sfcr.Common.CONFIG;

public class Data {
	protected Weather currentWeather = Weather.CLEAR;
	protected Weather nextWeather = Weather.CLEAR;
	protected float densityByWeather = 0f;
	protected float densityByBiome = 0f;
	private float targetDownFall = 1f;
	private float densityBySeason = 1F;
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

	/**
	 * !! Only call in client because server is multiplayer that cannot ensure what pos will use.
	 */
	public void updateBiomeDensity(PlayerEntity player) {
		World world = player.getWorld();
		boolean isDynamic = CONFIG.isEnableDynamic();
		boolean isBiomeByChunk = CONFIG.isBiomeDensityByChunk();

		if (isDynamic) {  //Detect Biome Change
			if (! isBiomeByChunk) {		//Hasn't effected if use chunk data.
				BlockPos pos = player.getBlockPos();
				RegistryEntry<Biome> biome = world.getBiome(pos);
				if (CONFIG.isFilterListHasBiome(biome))
					targetDownFall = CONFIG.getDownfall(biome.value().getPrecipitation(pos));
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
	public void updateWeatherDensity(World world) {
		float thunderDensity = CONFIG.getThunderDensityPercent() / 100f;
		float rainDensity = CONFIG.getRainDensityPercent() / 100f;
		float clearDensity = CONFIG.getCloudDensityPercent() / 100f;
		boolean enableDynamic = CONFIG.isEnableDynamic();

		if (enableDynamic) {
			boolean isPreDetectOn = CONFIG.getWeatherPreDetectTime() != 0;
			if (world.isThundering()) {
				isWeatherChange = nextWeather != Weather.THUNDER && isPreDetectOn || densityByWeather < thunderDensity;
			} else if (world.isRaining()) {
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

	public float getDensityBySeason() {
		return densityBySeason;
	}

	public void setDensityBySeason(float percent) {
		this.densityBySeason = percent / 100F;
	}

	public Weather getNextWeather() {
		return nextWeather;
	}

	public void setNextWeather(Weather nextWeather) {
		this.nextWeather = nextWeather;
	}

	public String getDebugString() {
		return "[SFCR] wc:" + isWeatherChange + ", bc:" + isBiomeChange + ", wd:" + densityByWeather + ", bd:" + densityByBiome + ", sd:" + densityBySeason;
	}

	public enum Weather {
		CLEAR,
		RAIN,
		THUNDER
	}

}
