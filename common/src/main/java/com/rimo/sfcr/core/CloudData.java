package com.rimo.sfcr.core;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.config.CommonConfig;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class CloudData {
	private static SimplexNoiseSampler cloudNoise;
	protected final CommonConfig CONFIG = Common.CONFIG;

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
	public CloudData(int x, int z, float densityByWeather, float densityByBiome) {
		dataType = CloudDataType.NORMAL;
		width = CONFIG.getCloudRenderDistance();
		height = CONFIG.getCloudLayerThickness();
		startX = x;
		startZ = z;
		_cloudData = new boolean[width][height][width];

		collectCloudData(x, z, densityByWeather, densityByBiome);
	}

	// Overload
	public CloudData(CloudDataType type) {
		dataType = type;
		lifeTime = CONFIG.getNormalRefreshSpeed().getValue() / 5f;
	}

	public static void initSampler(long seed) {
		cloudNoise = new SimplexNoiseSampler(Random.create(seed));
	}

	public void tick() {
		lifeTime -= MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
	}

	// Access
	public FloatArrayList getVertexList() {return vertexList;}
	public ByteArrayList getNormalList() {return normalList;}
	public CloudDataType getDataType() {return dataType;}
	public float getLifeTime() {return lifeTime;}

	private float getCloudDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * densityByBiome;
	}

	private void collectCloudData(int x, int z, float densityByWeather, float densityByBiome) {
		World world = MinecraftClient.getInstance().world;
		if (world == null)
			return;

		float f = 0.5f;		// origin threshold
		final double time = world.getTime() / 20.0;
		final int h = 80;  //height sampling use to get biome
		final float sx = x - width / 2f;  //sampling start pos
		final float sz = z - width / 2f;

		for (int cx = 0; cx < width; cx++) {
			for (int cz = 0; cz < width; cz++) {

				int bx = (int) (sx + cx + 0.5f) * CONFIG.getCloudBlockSize();		// transform cloudpos to blockpos
				int bz = (int) (sz + cz + 0.5f) * CONFIG.getCloudBlockSize();

				// calculating density...
				if (CONFIG.isEnableWeatherDensity()) {
					if (CONFIG.isBiomeDensityByChunk()) {
						if (CONFIG.isBiomeDensityUseLoadedChunk()) {
							Vec2f cellPos = new Vec2f(bx, bz);
							Vec2f unit = cellPos.normalize().multiply(16);  //measure as chunk
							while (!world.getChunkManager().isChunkLoaded((int) cellPos.x / 16, (int) cellPos.y / 16)
									&& unit.dot(cellPos) > 0)  //end when cellPos was reversed.
								cellPos = cellPos.add(unit.negate());  // stepping pos near towards to player

							f = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos((int) cellPos.x, h, (int) cellPos.y)))
									? getCloudDensityThreshold(densityByWeather, CONFIG.getDownfall(world.getBiome(new BlockPos((int) cellPos.x, h, (int) cellPos.y)).value().getPrecipitation(new BlockPos((int) cellPos.x, h, (int) cellPos.y))))
									: getCloudDensityThreshold(densityByWeather, densityByBiome);
						} else {
							f = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos(bx, h, bz)))
									? getCloudDensityThreshold(densityByWeather, CONFIG.getDownfall(world.getBiome(new BlockPos(bx, h, bz)).value().getPrecipitation(new BlockPos(bx, h, bz))))
									: getCloudDensityThreshold(densityByWeather, densityByBiome);
						}
					} else {
						f = getCloudDensityThreshold(densityByWeather, densityByBiome);
					}
				}

				// sampling...
				if (CONFIG.isEnableTerrainDodge()) {
					for (int cy = 0; cy < height; cy++) {
						// terrain dodge (detect light level)
						_cloudData[cx][cy][cz] = world.getLightLevel(LightType.SKY, new BlockPos(
								bx,
								(int) (Client.RENDERER.getCloudHeight() + (cy - 2) * CONFIG.getCloudBlockSize() / 2f),
								bz
						)) == 15 && getCloudSample(sx, sz, 0, time, cx, cy, cz) > f;
					}
				} else {
					for (int cy = 0; cy < height; cy++) {
						_cloudData[cx][cy][cz] = getCloudSample(sx, sz, 0, time, cx, cy, cz) > f;
					}
				}
			}
		}

		computingCloudMesh();
	}

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

	private double getCloudSample(double startX, double startZ, double zOffset, double timeOffset, double cx, double cy, double cz) {
		double cloudVal = cloudNoise.sample(
				(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				(startZ + cz + zOffset) * baseFreq
		);
		if (CONFIG.getSampleSteps() > 1) {
			double cloudVal1 = cloudNoise.sample(
					(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					(startZ + cz + zOffset) * l1Freq
			);
			double cloudVal2 = 1;
			if (CONFIG.getSampleSteps() > 2) {
				cloudVal2 = cloudNoise.sample(
						(startX + cx + (timeOffset * l2TimeFactor)) * l2Freq,
						0,
						(startZ + cz + zOffset) * l2Freq
				);
				//Smooth floor function...
				cloudVal2 *= 3;
				cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;		// (3*x-sin(2*3.1415926*3*x/(2*3.1415926)))/2
			}
			cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
		}
		return cloudVal * remappedValue(1 - (cy + 1) / 32);		//cloudVal ~ [-1, 2]
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

	public enum CloudDataType {
		NORMAL,
		TRANS_IN,
		TRANS_MID_BODY,
		TRANS_OUT
	}
}
