package com.rimo.sfcr.core;

import com.rimo.sfcr.Common;
import com.rimo.sfcr.config.SharedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
//? if > 1.18
import net.minecraft.core.Holder;
//? if > 1.19 {
import net.minecraft.util.RandomSource;
//? } else if ! 1.16.5 {
/*import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
*///? } else
//import java.util.Random;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

/**
 * Independent sampler for any side uses
 * @since 1.9
 */
public class Sampler {
	private Level level;
	private SimplexNoise cloudNoise;
	private int cloudThick;
	private int cloudBlockSize;
	private float cloudHeight;
	private boolean isEnableDynamic;
	private boolean isBiomeByChunk;
	private boolean isEnableTerrainDodge;
	private int steps;
	private float threshold;
	private float reduction;
	private float densityBySeason = 1F;

	public Sampler setSeed(long seed) {
		//? if > 1.19 {
		cloudNoise = new SimplexNoise(RandomSource.create(seed));
		//? } else if ! 1.16.5 {
		/*cloudNoise = new SimplexNoise(new SingleThreadedRandomSource(seed));
		*///? } else
		//cloudNoise = new SimplexNoise(new Random(seed));
		return this;
	}

	public Sampler setLevel(Level level) {
		this.level = level;
		return this;
	}

	public Sampler setConfig(SharedConfig config) {
		cloudThick = config.getCloudLayerThickness();
		cloudBlockSize = config.getCloudBlockSize();
		cloudHeight = config.getCloudHeight() < 0 ? 192 : config.getCloudHeight();
		isEnableDynamic = config.isEnableDynamic();
		isBiomeByChunk = config.isBiomeDensityByChunk();
		isEnableTerrainDodge = config.isEnableTerrainDodge();
		steps = config.getSampleSteps();
		threshold = config.getDensityThreshold();
		reduction = config.getThresholdMaxReduction();
		return this;
	}

	public void setDensityBySeason(float percent) {
		this.densityBySeason = percent / 100F;
	}

	public boolean isCloudCovered(double x, double y, double z) {
		int gx = (int) Math.floor((x + level.getGameTime() * 0.03F) / cloudBlockSize);
		int gz = (int) Math.floor(z / cloudBlockSize + 0.33F);
		for (int i = cloudThick - 1; i >= 0; i --) {
			if (isGridHasCloud(gx, i, gz, Common.DATA.densityByWeather, 0.5F)) {
				return (y - cloudHeight) / cloudBlockSize * 2 <= i;
			}
		}
		return false;
	}

	private float thresholdFormula(float threshold, float reduction, float weather, float biome) {
		return threshold - reduction * weather * biome;
	}

	private int oldX, oldZ;
	private float densityMultiplier = 1F;
	private double time = 0.0;
	private float f = 0.5F;

	/**
	 * @see #isCloudCovered(double, double, double)
	 * @see CloudData#collectCloudData(int, int, float, float)
	 */
	boolean isGridHasCloud(int x, int y, int z, float densityByWeather, float densityByBiome) {
		if (level == null || cloudNoise == null || Float.isNaN(cloudHeight))
			return false;

		if (oldX != x || oldZ != z) {
			oldX = x;
			oldZ = z;

			densityMultiplier = 1F;
			time = 0.0;
			f = threshold;
			if (isEnableDynamic) {
				densityMultiplier = getDensityMultiplier(level.getGameTime());
				time = level.getGameTime() / 20.0;
				f = thresholdFormula(threshold, reduction, densityByWeather, densityByBiome);
			}

			int bx = x * cloudBlockSize;
			int bz = z * cloudBlockSize;

			// biome detect by chunk
			if (isEnableDynamic && isBiomeByChunk && level.hasChunk(bx / 16, bz / 16)) {
				BlockPos pos = new BlockPos(
						bx,
						level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz),
						bz
				);
				//? if ! 1.16.5 {
				Holder<Biome> biome = level.getBiome(pos);
				if (! CONFIG.isFilterListHasBiome(biome))
				//? } else {
				/*Biome biome = level.getBiome(pos);
				if (! CONFIG.isFilterListHasBiome(biome.getBiomeCategory()))
				*///? }
					//? if > 1.20 {
					f = thresholdFormula(threshold, reduction, densityByWeather, CONFIG.getDownfall(biome.value().getPrecipitationAt(pos)));
					//? } else {
					/*//~ if ! 1.16.5 'biome.' -> 'biome.value().'
					f = thresholdFormula(threshold, reduction, densityByWeather, biome.value().getDownfall());
					*///? }
			}
		}

		return getCloudSampleProxy(time, steps, x, y, z) * densityMultiplier > f && (
				// terrain dodge (detect light level)
				! isEnableTerrainDodge || level.getBrightness(LightLayer.SKY, new BlockPos(
						x,
						(int) (cloudHeight + (y - 2) * cloudBlockSize / 2f),  //turns to exactly height
						z
				)) == 15
		);
	}

	private float getDensityMultiplier(long worldTime) {
		float m = 1F;
		float time = (worldTime % 24000L);
		if (time > 13000F || time < 1000F) {  // decreased density at night
			float remapTime = (time < 1000F ? time + 11000F : time - 13000F) / 12000F;
			float curveFactor = (float) Math.pow(4 * remapTime * (1 - remapTime), 0.5);  //smooth it...
			m = 1 - curveFactor * (1 - CONFIG.getDensityAtNight());
		}
		return m * densityBySeason;
	}

	private double getCloudSampleProxy(double timeOffset, int steps, double cx, double cy, double cz) {
		double sample = getCloudSample(timeOffset, steps, cx, cy, cz);
		if (level.isRaining() && CONFIG.isEnableDynamic()) {  //make cloud top more continuous when rain
			float clearDensity = CONFIG.getCloudDensityPercent() / 100f + 1;
			float currentDensity = DATA.densityByWeather + 1;
			float cyMax = CONFIG.getCloudLayerThickness();
			final float MAX_ADD = 3F;
			sample += Math.pow(cy / cyMax, 2) * Math.max((1 - clearDensity / Math.max(clearDensity, currentDensity)) * MAX_ADD, 0);
		}
		return sample;
	}

	/* - - - - - Sampler Core - - - - - */

	private static final float baseFreq = 0.05f;
	private static final float baseTimeFactor = 0.01f;

	private static final float l1Freq = 0.09f;
	private static final float l1TimeFactor = 0.02f;

	private static final float l2Freq = 0.001f;
	private static final float l2TimeFactor = 0.1f;

	private double remappedValue(double noise) {
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;
		// ((sin((((1-(x+1)/32)*180+302)*1.15)/3.1415926)^0.28)+(1-(x+1)/32)-0.5)*2, 32=height
	}

	private double getCloudSample(double timeOffset, int steps, double cx, double cy, double cz) {
		double cloudVal = cloudNoise.getValue(
				(cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				cz * baseFreq
		);
		if (steps > 1) {
			double cloudVal1 = cloudNoise.getValue(
					(cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					cz * l1Freq
			);
			double cloudVal2 = 1;
			if (steps > 2) {
				cloudVal2 = cloudNoise.getValue(
						(cx + (timeOffset * l2TimeFactor)) * l2Freq,
						0,
						cz * l2Freq
				);
				//Smooth floor function...
				cloudVal2 *= 3;
				cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;		// (3*x-sin(2*3.1415926*3*x/(2*3.1415926)))/2
			}
			cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
		}
		return cloudVal * remappedValue(1 - (cy + 1) / cloudThick);		//cloudVal ~ [-1, 2]
	}
}
