package com.rimo.sfcr.core;

import com.rimo.sfcr.core.Renderer.FACING;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.*;

public class CloudData {
	private static SimplexNoiseSampler cloudNoise;
	private final Type dataType;
	private float lifeTime;
	protected ArrayList<Integer> meshData = new ArrayList<>();
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
		dataType = Type.NORMAL;
		width = CONFIG.getCloudRenderDistance() * 2 + 1;
		height = CONFIG.getCloudLayerThickness();
		gridCenterX = x;
		gridCenterZ = z;
		gridYFromClouds = minusCloudGridHeight(y);
		_cloudData = new boolean[width][height][width];

		collectCloudData(x, z, densityByWeather, densityByBiome);
	}

	// for child
	public CloudData(Type type) {
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
	public Type getDataType() {return dataType;}
	public float getLifeTime() {return lifeTime;}

	boolean isHasCloud(double x, double y, double z) {
		Vec3d camPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		int cbSize = CONFIG.getCloudBlockSize();
		int gx = (int) (width / 2F - (camPos.getX() - x) / cbSize);
		int gy = (int) (y / cbSize * 2);
		int gz = (int) (width / 2F - (camPos.getZ() - z) / cbSize);
		for (int i = 0; i < height; i ++) {
			if (_cloudData[gx][i][gz]) {
				return gridYFromClouds <= i;
			}
		}
		return false;
	}

	private float getCloudDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * densityByBiome;
	}

	private void collectCloudData(int x, int z, float densityByWeather, float densityByBiome) {
		World world = MinecraftClient.getInstance().world;
		if (world == null)
			return;

		float f = 0.5f;		// origin threshold
		final double time = world.getTime() / 20.0;
		final float sx = x - width / 2f;  //sampling start pos
		final float sz = z - width / 2f;

		boolean isEnableDynamic = CONFIG.isEnableWeatherDensity();
		boolean isBiomeByChunk = CONFIG.isBiomeDensityByChunk();
		boolean isBiomeUseLoadedChunk = CONFIG.isBiomeDensityUseLoadedChunk();
		boolean isEnableTerrainDodge = CONFIG.isEnableTerrainDodge();
		float densityMultiplier = isEnableDynamic ? getDensityMultiplier(world.getTimeOfDay()) : 1;
		int steps = CONFIG.getSampleSteps();

		for (int cx = 0; cx < width; cx++) {
			for (int cz = 0; cz < width; cz++) {

				int bx = (int) (sx + cx + 0.5f) * CONFIG.getCloudBlockSize();		// transform cloudpos to blockpos
				int bz = (int) (sz + cz + 0.5f) * CONFIG.getCloudBlockSize();
				final int h = world.getTopY(Heightmap.Type.MOTION_BLOCKING, bx, bz);  //height sampling use to get biome

				// calculating density...
				if (isEnableDynamic && isBiomeByChunk) {
					BlockPos pos;
					if (isBiomeUseLoadedChunk) {
						Vec2f cellPos = new Vec2f(bx, bz);
						Vec2f unit = cellPos.normalize().multiply(16);  //measure as chunk
						while (!world.getChunkManager().isChunkLoaded((int) cellPos.x / 16, (int) cellPos.y / 16)
								&& unit.dot(cellPos) > 0)  //end when cellPos was reversed.
							cellPos = cellPos.add(unit.negate());  // stepping pos near towards to player

						pos = new BlockPos((int) cellPos.x, h, (int) cellPos.y);
					} else {
						pos = new BlockPos(bx, h, bz);
					}
					RegistryEntry<Biome> biome = world.getBiome(pos);
					f = ! CONFIG.isFilterListHasBiome(biome)
							? getCloudDensityThreshold(densityByWeather, CONFIG.getDownfall(biome.value().getPrecipitation(pos)))
							: getCloudDensityThreshold(densityByWeather, densityByBiome);
				} else {
					f = getCloudDensityThreshold(densityByWeather, densityByBiome);
				}

				// sampling...
				for (int cy = 0; cy < height; cy++) {
					_cloudData[cx][cy][cz] = getCloudSampleProxy(world, sx, sz, 0, time, steps, cx, cy, cz) * densityMultiplier > f && (
							// terrain dodge (detect light level)
							! isEnableTerrainDodge || world.getLightLevel(LightType.SKY, new BlockPos(
									bx,
									(int) (RENDERER.getCloudHeight() + (cy - 2) * CONFIG.getCloudBlockSize() / 2f),
									bz
							)) == 15
					);
				}
			}
		}
	}

	private float getDensityMultiplier(long worldTime) {
		float m = 1F;
		float time = (worldTime % 24000L);
		if (time > 13000F || time < 1000F) {  // decreased density at night
			float remapTime = (time < 1000F ? time + 11000F : time - 13000F) / 12000F;
			float curveFactor = (float) Math.pow(4 * remapTime * (1 - remapTime), 0.5);  //smooth it...
			m = 1 - curveFactor * (1 - CONFIG.getDensityAtNight());
		}
		return m;
	}

	private double getCloudSampleProxy(World world, double startX, double startZ, double zOffset, double timeOffset, int steps, double cx, double cy, double cz) {
		double sample = getCloudSample(startX, startZ, zOffset, timeOffset, steps, cx, cy, cz);
		if (world.isRaining() && CONFIG.isEnableWeatherDensity()) {  //make cloud top more continuous when rain
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
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;		// ((sin((((1-(x+1)/32)*180+302)*1.15)/3.1415926)^0.28)+(1-(x+1)/32)-0.5)*2
	}

	private double getCloudSample(double startX, double startZ, double zOffset, double timeOffset, int steps, double cx, double cy, double cz) {
		double cloudVal = cloudNoise.sample(
				(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				(startZ + cz + zOffset) * baseFreq
		);
		if (steps > 1) {
			double cloudVal1 = cloudNoise.sample(
					(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					(startZ + cz + zOffset) * l1Freq
			);
			double cloudVal2 = 1;
			if (steps > 2) {
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
		buildMesh(meshData);
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
				ArrayList<Integer> newMeshData = new ArrayList<>();
				buildMesh(newMeshData);
				synchronized (this) {
					meshData = newMeshData;
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

	/*
	 * try port 1.21.6+ vanilla mesh build function here...
	 */
	private void buildMesh(ArrayList<Integer> meshData) {
		int cy = gridYFromClouds;
		if (cy >= 0 && cy < height) {
			int cx = width / 2;
			if (_cloudData[cx][cy][cx]) {  //build inner faces then return
				encodeFace(meshData, -1, cy, 0, FACING.EAST, 0);
				encodeFace(meshData, 1, cy, 0, FACING.WEST, 0);
				encodeFace(meshData, 0, cy - 1, 0, FACING.TOP, 0);
				encodeFace(meshData, 0, cy + 1, 0, FACING.BOTTOM, 0);
				encodeFace(meshData, 0, cy, 1, FACING.NORTH, 0);
				encodeFace(meshData, 0, cy, -1, FACING.SOUTH, 0);
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
						int thickness = 0;  //these 2 y loop may run in same time, so thickness must sum separately
						for (int y = height - 1; y >= 0; -- y) {
							if (tryBuildCell(meshData, xOffset, y, - zOffset, renderDistance, thickness)) {
								thickness++;
							} else {
								if (thickness > 0)
									thickness --;
							}
						}
					}
					int thickness = 0;
					for (int y = height - 1; y >= 0; -- y) {
						if (tryBuildCell(meshData, xOffset, y, zOffset, renderDistance, thickness)) {
							thickness ++;
						} else {
							if (thickness > 0)
								thickness --;
						}
					}
				}
			}
		}
	}

	/*
	   Compress structure:
	   0 0 0 0 0 0 0 0  0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0
	                 |  | └─────┬─────┘   └──────┬──────┘   └──────┬──────┘
	         sign of x, z.      y             unsign x          unsign z
	   That's 1 vertex, we need 4 for 1 face.
	   Y no have negative value, and yet don't need up to 200+, so just store max to 127 (bit 01111111) save 1 bit for thickness
	   FACING information needs 3 bit (max ordinal 5 = bit 00000101), we compress at 1st vertex.
	   Thickness max base to y, we compress it in 2nd vertex int head.
	 */
	private void encodeFace(ArrayList<Integer> meshData, int x, int y, int z, FACING facing, int thickness) {
		switch (facing) {
			case EAST -> {
				meshData.add(compressToHead(compressVertex(x + 1, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z + 1), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x + 1, y + 1, z));
			}
			case WEST -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x, y, z + 1), thickness));
				meshData.add(compressVertex(x, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z));
			}
			case TOP -> {
				meshData.add(compressToHead(compressVertex(x, y + 1, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y + 1, z), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z + 1));
			}
			case BOTTOM -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z), thickness));
				meshData.add(compressVertex(x + 1, y, z + 1));
				meshData.add(compressVertex(x, y, z + 1));
			}
			case SOUTH -> {
				meshData.add(compressToHead(compressVertex(x, y, z + 1), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z + 1), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z + 1));
			}
			case NORTH -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z));
				meshData.add(compressVertex(x, y + 1, z));
			}
		}
	}
	private static int compressVertex(int x, int y, int z) {
		int i = 0;
		i |= (255 & x) << 8;  // 00000000 00000000 00000000 11111111
		i |= (127 & y) << 16;    // 00000000 00000000 00000000 01111111
		i |= (255 & z);
		i |= (Integer.MIN_VALUE & x) >>> 7;  // 10000000 00000000 00000000 00000000
		i |= (Integer.MIN_VALUE & z) >>> 8;
		return i;
	}
	private static int compressToHead(int i, int j) {
		return i | j << 25;
	}
	static int[] depressVertex(int i) {
		int x, y, z;
		x = (i << 7 & Integer.MIN_VALUE) == 0 ?  //is positive?
				 i >> 8 & 255 :
				(i >> 8 & 255) - 256;  // -256 equals to | 0xFFFFFF00;
		y = i >> 16 & 127;
		z = (i << 8 & Integer.MIN_VALUE) == 0 ?
				 i & 255 :
				(i & 255) - 256;
		return new int[]{x, y, z};
	}
	static int depressFromHead(int i) {
		return i >> 25;
	}

	/**
	 * @return true if a cell was built, false if not.
	 */
	private boolean tryBuildCell(ArrayList<Integer> meshData, int xOffset, int y, int zOffset, int renderDistance, int thickness) {
		int x = xOffset + renderDistance;  //trans to list index
		int z = zOffset + renderDistance;
		if (!_cloudData[x][y][z])
			return false;  //ignore empty...
		boolean borderTop    = !(y + 1 <  height            && _cloudData[x][y + 1][z]);  // outOfBound check || isNotNeighbor
		boolean borderBottom = !(y - 1 >= 0                 && _cloudData[x][y - 1][z]);
		boolean borderSouth  = !(z + 1 <  _cloudData.length && _cloudData[x][y][z + 1]);
		boolean borderNorth  = !(z - 1 >= 0                 && _cloudData[x][y][z - 1]);
		boolean borderEast   = !(x + 1 <  _cloudData.length && _cloudData[x + 1][y][z]);
		boolean borderWest   = !(x - 1 >= 0                 && _cloudData[x -1 ][y][z]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0) |
				(thickness << 6);
		buildExtrudedCell(meshData, xOffset, y, zOffset, cellState);
		return true;
	}

	private void buildExtrudedCell(ArrayList<Integer> meshData, int x, int y, int z, int cellState) {
		int thickness = cellState >> 6;
		if (hasBorderTop(cellState) && y < gridYFromClouds)  //facing (normals) culling...
			encodeFace(meshData, x, y, z, FACING.TOP, thickness);
		if (hasBorderBottom(cellState) && y > gridYFromClouds)
			encodeFace(meshData, x, y, z, FACING.BOTTOM, thickness);
		if (hasBorderSouth(cellState) && z < 0)
			encodeFace(meshData, x, y, z, FACING.SOUTH, thickness);
		if (hasBorderNorth(cellState) && z > 0)
			encodeFace(meshData, x, y, z, FACING.NORTH, thickness);
		if (hasBorderEast(cellState) && x < 0)
			encodeFace(meshData, x, y, z, FACING.EAST, thickness);
		if (hasBorderWest(cellState) && x > 0)
			encodeFace(meshData, x, y, z, FACING.WEST, thickness);
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

	public enum Type {
		NORMAL,
		TRANS_IN,
		TRANS_MID_BODY,
		TRANS_OUT
	}
}
