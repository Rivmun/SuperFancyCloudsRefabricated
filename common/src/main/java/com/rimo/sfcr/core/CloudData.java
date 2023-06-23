package com.rimo.sfcr.core;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.util.CloudDataType;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.gen.random.SimpleRandom;

public class CloudData implements CloudDataImplement {

	private static SimplexNoiseSampler cloudNoise;

	protected final Runtime RUNTIME = SFCReMod.RUNTIME;
	protected final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

	private final CloudDataType dataType;
	private float lifeTime;

	protected FloatArrayList vertexList = new FloatArrayList();
	protected ByteArrayList normalList = new ByteArrayList();
	protected boolean[][][] _cloudData;

	protected int width;
	protected int height;
	protected int startX;
	protected int startZ;

	// Normal constructor
	public CloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		dataType = CloudDataType.NORMAL;
		width = CONFIG.getCloudRenderDistance();
		height = CONFIG.getCloudLayerThickness();
		startX = (int) (scrollX / CONFIG.getCloudBlockSize());
		startZ = (int) (scrollZ / CONFIG.getCloudBlockSize()) - RUNTIME.fullOffset;
		_cloudData = new boolean[width][height][width];
		
		collectCloudData(scrollX, scrollZ, densityByWeather, densityByBiome);
	}

	// Overload
	public CloudData(CloudData prevData, CloudData nextData, CloudDataType type) {
		dataType = type;
		lifeTime = CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed()) / 5f;
	}

	public static void initSampler(long seed) {
		cloudNoise = new SimplexNoiseSampler(new SimpleRandom(seed));
	}

	public void tick() {
		lifeTime -= MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
	}

	// Access
	public FloatArrayList getVertexList() {return vertexList;}
	public ByteArrayList getNormalList() {return normalList;}
	public CloudDataType getDataType() {return dataType;}
	public float getLifeTime() {return lifeTime;}

	/* - - - - - Sampler Core - - - - - */

	private static final float baseFreq = 0.05f;
	private static final float baseTimeFactor = 0.01f;

	private static final float l1Freq = 0.09f;
	private static final float l1TimeFactor = 0.02f;

	private static final float l2Freq = 0.001f;
	private static final float l2TimeFactor = 0.1f;

	private double remappedValue(double noise) {
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;		// ((sin((((1-(x+1)/32)*180+302)*1.15)/3.1415926)^0.28)+(1-(x+1)/32)-0.5)*2
	}

	private double getCloudSample(double startX, double startZ, double timeOffset, double cx, double cy, double cz) {
		double cloudVal = cloudNoise.sample(
				(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				(startZ + cz - RUNTIME.fullOffset) * baseFreq
		);
		if (CONFIG.getSampleSteps() > 1) {
			double cloudVal1 = cloudNoise.sample(
					(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					(startZ + cz - RUNTIME.fullOffset) * l1Freq
			);
			double cloudVal2 = 1;
			if (CONFIG.getSampleSteps() > 2) {
				cloudVal2 = cloudNoise.sample(
						(startX + cx + (timeOffset * l2TimeFactor)) * l2Freq,
						0,
						(startZ + cz - RUNTIME.fullOffset) * l2Freq
				);
				//Smooth floor function...
				cloudVal2 *= 3;
				cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;		// (3*x-sin(2*3.1415926*3*x/(2*3.1415926)))/2
			}
			cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
		}
		return cloudVal * remappedValue(1 - (cy + 1) / 32);		//cloudVal ~ [-1, 2]
	}

	private float getCloudDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * (1 - (1 - densityByBiome) * CONFIG.getBiomeDensityMultiplier() / 100f);
	}

	@Override
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		try {
			double startX = scrollX / CONFIG.getCloudBlockSize();
			double startZ = scrollZ / CONFIG.getCloudBlockSize();

			double timeOffset = Math.floor(RUNTIME.time / 6) * 6;

			var world = MinecraftClient.getInstance().world;
			var f = 0.5f;		// origin

			for (int cx = 0; cx < width; cx++) {
				for (int cz = 0; cz < width; cz++) {

					var px = (startX - width / 2f + cx + 0.5f) * CONFIG.getCloudBlockSize();		// transform cloudpos to blockpos
					var pz = (startZ - width / 2f + cz + 0.5f) * CONFIG.getCloudBlockSize();

					// calculating density...
					if (CONFIG.isEnableWeatherDensity() && CONFIG.isBiomeDensityByChunk()) {
						if (CONFIG.isBiomeDensityUseLoadedChunk()) {
							var vec = new Vec2f(cx - width / 2, cz - width / 2).normalize();		// stepping pos near towards to player
							var px2 = px;
							var pz2 = pz;
							while (!world.getChunkManager().isChunkLoaded((int) px2 / 16, (int) pz2 / 16) && Math.abs(scrollX - px) + Math.abs(scrollZ - pz) > CONFIG.getCloudBlockSize() * 4) {
								px2 -= vec.x * CONFIG.getCloudBlockSize();
								pz2 -= vec.y * CONFIG.getCloudBlockSize();
							}
							f = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos(px2, 80, pz2)))
									? getCloudDensityThreshold(densityByWeather, world.getBiome(new BlockPos(px2, 80, pz2)).value().getDownfall())
									: getCloudDensityThreshold(densityByWeather, densityByBiome);
						} else {
							f = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos(px, 80, pz)))
									? getCloudDensityThreshold(densityByWeather, world.getBiome(new BlockPos(px, 80, pz)).value().getDownfall())
									: getCloudDensityThreshold(densityByWeather, densityByBiome);
						}
					} else {
						f = getCloudDensityThreshold(densityByWeather, densityByBiome);
					}

					// sampling...
					if (CONFIG.isEnableTerrainDodge()) {
						for (int cy = 0; cy < height; cy++) {
							// terrain dodge (detect light level)
							_cloudData[cx][cy][cz] = world.getLightLevel(new BlockPos(
									px,
									SFCReMod.RENDERER.cloudHeight + (cy - 2) * CONFIG.getCloudBlockSize() / 2f,
									pz + CONFIG.getCloudBlockSize() / 4f        // cloud is moving...fix Z pos
							)) == 15 && getCloudSample(startX, startZ, timeOffset, cx, cy, cz) > f;
						}
					} else {
						for (int cy = 0; cy < height; cy++) {
							_cloudData[cx][cy][cz] = getCloudSample(startX, startZ, timeOffset, cx, cy, cz) > f;
						}
					}
				}
			}
		} catch (Exception e) {
			SFCReMod.exceptionCatcher(e);
		}

		computingCloudMesh();
	}

	@Override
	public void collectCloudData(CloudData prevData, CloudData nextData) {
		// Leave empty here for child.
	}

	/* - - - - - Mesh Computing - - - - - */

	protected void addVertex(float x, float y, float z) {
		vertexList.add(x - width / 2f);
		vertexList.add(y);
		vertexList.add(z - width / 2f);
	}

	protected void computingCloudMesh() {
		for (int cx = 0; cx < width; cx++) {
			for (int cy = 0; cy < height; cy++) {
				for (int cz = 0; cz < width; cz++) {
					if (!_cloudData[cx][cy][cz])
						continue;

					//Right
					if (cx == width - 1 || !_cloudData[cx + 1][cy][cz]) {
						addVertex(cx + 1, cy, cz);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx + 1, cy + 1, cz);

						normalList.add((byte) 0);
					}

					//Left....
					if (cx == 0 || !_cloudData[cx - 1][cy][cz]) {
						addVertex(cx, cy, cz);
						addVertex(cx, cy, cz + 1);
						addVertex(cx, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz);

						normalList.add((byte) 1);
					}

					//Up....
					if (cy == height - 1 || !_cloudData[cx][cy + 1][cz]) {
						addVertex(cx, cy + 1, cz);
						addVertex(cx + 1, cy + 1, cz);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz + 1);

						normalList.add((byte) 2);
					}

					//Down
					if (cy == 0 || !_cloudData[cx][cy - 1][cz]) {
						addVertex(cx, cy, cz);
						addVertex(cx + 1, cy, cz);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx, cy, cz + 1);

						normalList.add((byte) 3);
					}


					//Forward....
					if (cz == width - 1 || !_cloudData[cx][cy][cz + 1]) {
						addVertex(cx, cy, cz + 1);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz + 1);

						normalList.add((byte) 4);
					}

					//Backward
					if (cz == 0 || !_cloudData[cx][cy][cz - 1]) {
						addVertex(cx, cy, cz);
						addVertex(cx + 1, cy, cz);
						addVertex(cx + 1, cy + 1, cz);
						addVertex(cx, cy + 1, cz);

						normalList.add((byte) 5);
					}
				}
			}
		}
	}
}