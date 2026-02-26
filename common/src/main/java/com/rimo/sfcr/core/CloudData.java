package com.rimo.sfcr.core;

import com.rimo.sfcr.Common;
import com.rimo.sfcr.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.ArrayList;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.exceptionCatcher;

public class CloudData {
	private static SimplexNoiseSampler cloudNoise;
	protected final Config CONFIG = Common.CONFIG;
	private final CloudDataType dataType;
	private float lifeTime;
	protected ArrayList<Float> vertexList = new ArrayList<>();
	protected ArrayList<Byte> normalList = new ArrayList<>();
	protected boolean[][][] _cloudData;
	protected int width;
	protected int height;
	protected int gridCenterX;
	protected int gridCenterZ;
	// We want build inner faces earlier (no through culling equation to build useless faces), so this arg place here instead of Renderer.
	protected int gridYFromClouds;
	private boolean isOnBuild = false;
	private Thread buildThread;

	// Normal constructor
	public CloudData(int x, int y, int z, float densityByWeather, float densityByBiome) {
		dataType = CloudDataType.NORMAL;
		width = CONFIG.getCloudRenderDistance() * 2 + 1;
		height = CONFIG.getCloudLayerThickness();
		gridCenterX = x;
		gridCenterZ = z;
		gridYFromClouds = minusCloudGridHeight(y);
		_cloudData = new boolean[width][height][width];

		collectCloudData(x, z, densityByWeather, densityByBiome);
	}

	// for child
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

	private int minusCloudGridHeight(int y) {
		return y - (int)(RENDERER.getCloudHeight() / CONFIG.getCloudBlockSize() * 2);
	}

	// Access
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
								(int) (RENDERER.getCloudHeight() + (cy - 2) * CONFIG.getCloudBlockSize() / 2f),
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

	public CloudData buildMesh() {
		buildMesh(vertexList, normalList);
		return this;
	}

	public void tryRebuildMesh(int y) {
		if (isOnBuild)
  			return;
		int newGridY = minusCloudGridHeight(y);
		if (newGridY == gridYFromClouds)
			return;
		isOnBuild = true;
		gridYFromClouds = newGridY;
		buildThread = new Thread(() -> {
			try {
				ArrayList<Float> newVertexList = new ArrayList<>();
				ArrayList<Byte> newNormalList = new ArrayList<>();
				buildMesh(newVertexList, newNormalList);
				synchronized (this) {
					vertexList = newVertexList;
					normalList = newNormalList;
				}
			} catch (Exception e) {
				exceptionCatcher(e);
			} finally {
				RENDERER.markForRebuild();
				isOnBuild = false;
			}
		});
		buildThread.start();
	}

	public void stop() {
		try {
			if (buildThread != null)
				buildThread.join();
		} catch (Exception e) {
			//
		}
	}

	private void addVertex(ArrayList<Float> vertexList, float x, float y, float z) {
		vertexList.add(x);
		vertexList.add(y);
		vertexList.add(z);
	}

	/*
	 * try port 1.21.6+ vanilla mesh build function here...
	 */
	private void buildMesh(ArrayList<Float> vertexList, ArrayList<Byte> normalList) {
		int cy = gridYFromClouds;
		if (cy >= 0 && cy < height) {
			int cx = width / 2;
			if (_cloudData[cx][cy][cx]) {  //build inner faces then return
				addVertex(vertexList, 0, cy, 0);
				addVertex(vertexList, 0, cy, 1);
				addVertex(vertexList, 0, cy + 1, 1);
				addVertex(vertexList, 0, cy + 1, 0);
				normalList.add((byte) 0);
				addVertex(vertexList, 1, cy, 0);
				addVertex(vertexList, 1, cy, 1);
				addVertex(vertexList, 1, cy + 1, 1);
				addVertex(vertexList, 1, cy + 1, 0);
				normalList.add((byte) 1);
				addVertex(vertexList, 0, cy, 1);
				addVertex(vertexList, 1, cy, 1);
				addVertex(vertexList, 1, cy + 1, 1);
				addVertex(vertexList, 0, cy + 1, 1);
				normalList.add((byte) 5);
				addVertex(vertexList, 0, cy, 0);
				addVertex(vertexList, 1, cy, 0);
				addVertex(vertexList, 1, cy + 1, 0);
				addVertex(vertexList, 0, cy + 1, 0);
				normalList.add((byte) 4);
				addVertex(vertexList, 0, cy, 0);
				addVertex(vertexList, 1, cy, 0);
				addVertex(vertexList, 1, cy, 1);
				addVertex(vertexList, 0, cy, 1);
				normalList.add((byte) 2);
				addVertex(vertexList, 0, cy + 1, 0);
				addVertex(vertexList, 1, cy + 1, 0);
				addVertex(vertexList, 1, cy + 1, 1);
				addVertex(vertexList, 0, cy + 1, 1);
				normalList.add((byte) 3);
				return;
			}
		}

		int renderDistance = (width - 1) / 2;
		for(int l = 0; l <= 2 * renderDistance; ++l) {
			for (int xOffset = - l; xOffset <= l; ++ xOffset) {
				int zOffset = l - Math.abs(xOffset);
				// circular-like culling...
				if (zOffset >= 0 && zOffset <= renderDistance && xOffset * xOffset + zOffset * zOffset <= renderDistance * renderDistance) {
					if (zOffset != 0) {
						for (int y = 0; y < height; ++ y) {
							tryBuildCell(vertexList, normalList, xOffset, y, - zOffset, renderDistance);
						}
					}
					for (int y = 0; y < height; ++ y) {
						tryBuildCell(vertexList, normalList, xOffset, y, zOffset, renderDistance);
					}
				}
			}
		}
	}

	private void tryBuildCell(ArrayList<Float> vertexList, ArrayList<Byte> normalList, int xOffset, int y, int zOffset, int renderDistance) {
		int x = xOffset + renderDistance;  //trans to list index
		int z = zOffset + renderDistance;
		if (!_cloudData[x][y][z])
			return;  //ignore empty...
		boolean borderTop    = !(y + 1 <  height            && _cloudData[x][y + 1][z]);  // outOfBound || isNotNeighbor
		boolean borderBottom = !(y - 1 >= 0                 && _cloudData[x][y - 1][z]);
		boolean borderSouth  = !(z + 1 <  _cloudData.length && _cloudData[x][y][z + 1]);
		boolean borderNorth  = !(z - 1 >= 0                 && _cloudData[x][y][z - 1]);
		boolean borderEast   = !(x + 1 <  _cloudData.length && _cloudData[x + 1][y][z]);
		boolean borderWest   = !(x - 1 >= 0                 && _cloudData[x -1 ][y][z]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0);
		buildExtrudedCell(vertexList, normalList, xOffset, y, zOffset, cellState);
	}

	private void buildExtrudedCell(ArrayList<Float> vertexList, ArrayList<Byte> normalList, int x, int y, int z, int cellState) {
		if (hasBorderTop(cellState) && y < gridYFromClouds) {  //facing (normals) culling...
			addVertex(vertexList, x, y + 1, z);
			addVertex(vertexList, x + 1, y + 1, z);
			addVertex(vertexList, x + 1, y + 1, z + 1);
			addVertex(vertexList, x, y + 1, z + 1);
			normalList.add((byte) 2);
		}
		if (hasBorderBottom(cellState) && y > gridYFromClouds) {
			addVertex(vertexList, x, y, z);
			addVertex(vertexList, x + 1, y, z);
			addVertex(vertexList, x + 1, y, z + 1);
			addVertex(vertexList, x, y, z + 1);
			normalList.add((byte) 3);
		}
		if (hasBorderSouth(cellState) && z < 0) {
			addVertex(vertexList, x, y, z + 1);
			addVertex(vertexList, x + 1, y, z + 1);
			addVertex(vertexList, x + 1, y + 1, z + 1);
			addVertex(vertexList, x, y + 1, z + 1);
			normalList.add((byte) 4);
		}
		if (hasBorderNorth(cellState) && z > 0) {
			addVertex(vertexList, x, y, z);
			addVertex(vertexList, x + 1, y, z);
			addVertex(vertexList, x + 1, y + 1, z);
			addVertex(vertexList, x, y + 1, z);
			normalList.add((byte) 5);
		}
		if (hasBorderEast(cellState) && x < 0) {
			addVertex(vertexList, x + 1, y, z);
			addVertex(vertexList, x + 1, y, z + 1);
			addVertex(vertexList, x + 1, y + 1, z + 1);
			addVertex(vertexList, x + 1, y + 1, z);
			normalList.add((byte) 0);
		}
		if (hasBorderWest(cellState) && x > 0) {
			addVertex(vertexList, x, y, z);
			addVertex(vertexList, x, y, z + 1);
			addVertex(vertexList, x, y + 1, z + 1);
			addVertex(vertexList, x, y + 1, z);
			normalList.add((byte) 1);
		}
	}
	private static boolean hasBorderTop(int packed) {
		return (packed >> 5 & 1) != 0;
	}
	private static boolean hasBorderBottom(int packed) {
		return (packed >> 4 & 1) != 0;
	}
	private static boolean hasBorderEast(int packed) {
		return (packed >> 3 & 1) != 0;
	}
	private static boolean hasBorderWest(int packed) {
		return (packed >> 2 & 1) != 0;
	}
	private static boolean hasBorderSouth(int packed) {
		return (packed >> 1 & 1) != 0;
	}
	private static boolean hasBorderNorth(int packed) {
		return (packed >> 0 & 1) != 0;
	}

	public enum CloudDataType {
		NORMAL,
		TRANS_IN,
		TRANS_MID_BODY,
		TRANS_OUT
	}
}
