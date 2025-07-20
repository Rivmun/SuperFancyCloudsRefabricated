package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.nio.ByteBuffer;

public class Renderer {

	private final Config CONFIG;
	private final Data DATA;

	private SimplexNoiseSampler sampler;
	private int gridX, gridY, gridZ;  //camera position in cloudGrid
	private float cloudHeight;
	private boolean[][][] cloudGrid;  //replace this.cells
	private int renderDistance;
	private int cloudGridWidth;
	private int cloudLayerHeight;

	//default cloudSize, see at net.minecraft.client.render.WorldRenderer:266
	private static final float CLOUD_BLOCK_WIDTH = 12.0f;
	private static final float CLOUD_BLOCK_HEIGHT = 4.0f;

	private float vanillaCloudHeight;
	private int vanillaCloudRenderDistance;
	private int vanillaViewDistance;

	Renderer(Config config, Data data) {
		this.CONFIG = config;
		this.DATA = data;
	}

	public synchronized void setRenderer(Config config) {
		cloudHeight = CONFIG.getCloudHeight() == -1 ? vanillaCloudHeight : (CONFIG.getCloudHeight()
				+ 0.33f  //see at net.minecraft.client.render.WorldRenderer:466
		);
		cloudLayerHeight = config.getCloudLayerHeight();
		renderDistance = CONFIG.isEnableRenderDistanceFitToView() ?
				vanillaViewDistance * 6 :
				CONFIG.getRenderDistance() >= 32 ?
						CONFIG.getRenderDistance() :
						vanillaCloudRenderDistance;
		cloudGridWidth = renderDistance * 2 + 1;
		cloudGrid = initCloudGrid(cloudLayerHeight, cloudGridWidth);
	}

	public void setGridPos(int x, int y, int z) {
		gridX = x;
		gridY = y;
		gridZ = z;
	}

	public void setRenderDistance(Integer viewDistance, Integer cloudRenderDistance) {
		this.vanillaViewDistance = viewDistance;
		this.vanillaCloudRenderDistance = cloudRenderDistance;
		setRenderer(CONFIG);
	}

	public void setCloudHeight(float height) {
		vanillaCloudHeight = height;
	}

	protected void setSampler(long seed) {
		this.sampler = new SimplexNoiseSampler(Random.create(seed));
	}

	private boolean[][][] initCloudGrid(int h, int w) {
		return new boolean[w][w][h];
	}

	private void updateCloudGrid(int sx, int sz) {
		World world = MinecraftClient.getInstance().player.getWorld();
		long time = world instanceof ServerWorld ? world.getTime() : 0L;
		float threshold = 0.5f;

		for(int cx = 0; cx < cloudGridWidth; cx++) {
			for(int cz = 0; cz < cloudGridWidth; cz++) {

				float bx = (sx + cx + 0.5f) * CLOUD_BLOCK_WIDTH;  //transform gridPos to blockPos
				float bz = (sz + cz + 0.5f) * CLOUD_BLOCK_WIDTH;

				// calculating density...
				if (CONFIG.isEnableWeatherDensity() && CONFIG.isEnableBiomeDensityByChunk()) {
					if (CONFIG.isEnableBiomeDensityUseLoadedChunk()) {
						Vec2f vec = new Vec2f(cx - cloudGridWidth / 2f, cz - cloudGridWidth / 2f).normalize();  // stepping pos near towards to player
						float bx2 = bx;
						float bz2 = bz;
						while (!world.getChunkManager().isChunkLoaded((int) bx2 / 16, (int) bz2 / 16)
								&& Math.abs(sx * cloudGridWidth - bx) + Math.abs(sz * cloudGridWidth - bz) > CLOUD_BLOCK_WIDTH * 4) {  //jump if too close
							bx2 -= vec.x * CLOUD_BLOCK_WIDTH;
							bz2 -= vec.y * CLOUD_BLOCK_WIDTH;
						}
						threshold = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos((int) bx2, 80, (int) bz2)))
								? getDensityThreshold(DATA.densityByWeather, world.getBiome(new BlockPos((int)bx2, 80, (int)bz2)).value().weather.downfall())
								: getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
					} else {
						threshold = !CONFIG.isFilterListHasBiome(world.getBiome(new BlockPos((int) bx, 80, (int) bz)))
								? getDensityThreshold(DATA.densityByWeather, world.getBiome(new BlockPos((int)bx, 80, (int)bz)).value().weather.downfall())
								: getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
					}
				} else {
					threshold = getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
				}

				// sampling...
				if (CONFIG.isEnableTerrainDodge()) {
					for (int cy = 0; cy < cloudLayerHeight; cy++) {

						// terrain dodge (detect light level)
						cloudGrid[cx][cz][cy] = world.getLightLevel(LightType.SKY, new BlockPos(
								(int) bx,
								(int) (cloudHeight + (cy - 1.5f) * CLOUD_BLOCK_HEIGHT),
								(int) bz  // cloud is moving, fix Z pos
						)) == 15 && getCloudSample(sampler, sx, sz, 0, time, cx, cy, cz, CONFIG.getSampleSteps()) > threshold;
					}
				} else {
					for(int cy = 0; cy < cloudLayerHeight; cy++) {
						cloudGrid[cx][cz][cy] = getCloudSample(sampler, sx, sz, 0, time, cx, cy, cz, CONFIG.getSampleSteps()) > threshold;
					}
				}
			}
		}

	}

	private float getDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * densityByBiome;
	}

	/*
		- - - - - - - - - - - -
		Overwritten vanilla method
		to add height axis for render multi layer clouds.
		- - - - - - - - - - - -
	 */

	public void buildCloudCells(ByteBuffer byteBuffer, boolean isFancy, int oldDistance) {
		updateCloudGrid(this.gridX - renderDistance, this.gridZ - renderDistance);  //update "cells"

		for(int l = 0; l <= 2 * renderDistance; ++l) {
			for(int xOffset = -l; xOffset <= l; ++xOffset) {
				int zOffset = l - Math.abs(xOffset);
				if (zOffset >= 0 && zOffset <= renderDistance && xOffset * xOffset + zOffset * zOffset <= renderDistance * renderDistance) {
					//insert height traverse
					for(int h = cloudLayerHeight; h == 1; --h) {
						if (byteBuffer.remaining() < 24)
							return;  //java.nio.BufferOverflow Check, 24 is max put amount in single cell.

						if (zOffset != 0)
							this.method_72155(byteBuffer, isFancy, xOffset, h, -zOffset, renderDistance);
						this.method_72155(byteBuffer, isFancy, xOffset, h, zOffset, renderDistance);

						if (!isFancy)
							break;  //FAST_CLOUD only draw single layer.
					}
				}
			}
		}

	}

	private void method_72155(ByteBuffer byteBuffer, boolean isFancy, int xOffset, int h, int zOffset, int renderDistance) {
		int x = xOffset + renderDistance;  //transform to grid pos
		int z = zOffset + renderDistance;
		if (x < 0 || x >= cloudGridWidth || z < 0 || z >= cloudGridWidth)  //check bound
			return;

		boolean state = this.cloudGrid[x][z][h];
		if (!state)
			return;  //jumping empty cell

		//check neighbor and push it to next
		boolean borderTop    = !(h + 1 <  cloudLayerHeight && cloudGrid[x][z][h + 1]);  //outOfBound || Not has neighbor -> built border
		boolean borderBottom = !(h - 1 >= 0                && cloudGrid[x][z][h - 1]);
		boolean borderEast   = !(x + 1 <  cloudGridWidth   && cloudGrid[x + 1][z][h]);
		boolean borderWest   = !(x - 1 >= 0                && cloudGrid[x - 1][z][h]);
		boolean borderSouth  = !(z + 1 <  cloudGridWidth   && cloudGrid[x][z + 1][h]);
		boolean borderNorth  = !(z - 1 >= 0                && cloudGrid[x][z - 1][h]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0);
		if (cellState == 0)
			return;  //jumping cell which fully around by neighbor cells

		h += (cloudHeight - vanillaCloudHeight) / CLOUD_BLOCK_HEIGHT;  //trans to offset
		if (isFancy) {
			this.buildCloudCellFancy(byteBuffer, xOffset, h, zOffset, cellState);
		} else {
			this.buildCloudCellFast(byteBuffer, xOffset, h, zOffset);
		}
	}

	private void buildCloudCellFast(ByteBuffer byteBuffer, int x, int h, int z) {
		this.method_71098(byteBuffer, x, h, z, Direction.DOWN, 32);
	}

	/*
		original byte l structure
		1 0 0 1 0 0 1 0
		│ │ └┬┘ │ └─┬─┘
		│ │  │  │   └─ direction (shader only mask 3 bit)
		│ │  │  └──── !unused!
		│ │  └────── i (16 or 32)
		│ └──────── odevity of z
		└───────── odevity of x
		we try save h's odevity to 4th bit
	 */
	private void method_71098(ByteBuffer byteBuffer, int x, int h, int z, Direction direction, int i) {
		int l = direction.getIndex() | i;
		l |= (x & 1) << 7;
		l |= (z & 1) << 6;
		l |= (h & 1) << 3;  //add height
		byteBuffer.put((byte)(x >> 1)).put((byte)(z >> 1)).put((byte)(h >> 1)).put((byte)l);
	}

	private void buildCloudCellFancy(ByteBuffer byteBuffer, int x, int h, int z, int cellState) {
		if (hasBorderTop(cellState) && h <= this.gridY) {  //TODO: why needs "="?
			this.method_71098(byteBuffer, x, h, z, Direction.UP, 0);
		}
		if (hasBorderBottom(cellState) && h > this.gridY) {
			this.method_71098(byteBuffer, x, h, z, Direction.DOWN, 0);
		}
		if (hasBorderNorth(cellState) && z > 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.NORTH, 0);
		}
		if (hasBorderSouth(cellState) && z < 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.SOUTH, 0);
		}
		if (hasBorderWest(cellState) && x > 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.WEST, 0);
		}
		if (hasBorderEast(cellState) && x < 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.EAST, 0);
		}
//		if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && Math.abs(h) <= this.gridY) {  //inner faces
//			Direction[] directions = Direction.values();
//			for (Direction direction : directions) {
//				this.method_71098(byteBuffer, x, h, z, direction, 16);
//			}
//		}
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


	/* - - - - - Sampler Core - - - - - */

	private static final float baseFreq = 0.05f;
	private static final float baseTimeFactor = 0.01f;

	private static final float l1Freq = 0.09f;
	private static final float l1TimeFactor = 0.02f;

	private static final float l2Freq = 0.001f;
	private static final float l2TimeFactor = 0.1f;

	private static double remappedValue(double noise) {
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;		// ((sin((((1-(x+1)/32)*180+302)*1.15)/3.1415926)^0.28)+(1-(x+1)/32)-0.5)*2
	}

	public static double getCloudSample(SimplexNoiseSampler sampler, double startX, double startZ, double zOffset, double timeOffset, double cx, double cy, double cz, int step) {
		double cloudVal = sampler.sample(
				(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				(startZ + cz - zOffset) * baseFreq
		);
		if (step > 1) {
			double cloudVal1 = sampler.sample(
					(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					(startZ + cz - zOffset) * l1Freq
			);
			double cloudVal2 = 1;
			if (step > 2) {
				cloudVal2 = sampler.sample(
						(startX + cx + (timeOffset * l2TimeFactor)) * l2Freq,
						0,
						(startZ + cz - zOffset) * l2Freq
				);
				//Smooth floor function...
				cloudVal2 *= 3;
				cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;		// (3*x-sin(2*3.1415926*3*x/(2*3.1415926)))/2
			}
			cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
		}
		return cloudVal * remappedValue(1 - (cy + 1) / 32);		//range likely [-1, 2]
	}

}
