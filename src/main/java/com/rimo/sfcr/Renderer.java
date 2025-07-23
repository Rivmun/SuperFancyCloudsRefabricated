package com.rimo.sfcr;

import com.rimo.sfcr.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.nio.ByteBuffer;
import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class Renderer {
	private SimplexNoiseSampler sampler;
	private int gridX, gridY, gridZ;  //camera position in cloudGrid
	private float cloudHeight;
	private CloudGrid cloudGrid;  //replace this.cells
	private Thread resamplingThread;
	private boolean isResampling = false;
	private int renderDistance;
	private int cloudGridWidth;
	private int cloudLayerHeight;

	//default cloudSize, see at net.minecraft.client.render.WorldRenderer:266
	private static final float CLOUD_BLOCK_WIDTH = 12.0f;
	private static final float CLOUD_BLOCK_HEIGHT = 4.0f;

	private float vanillaCloudHeight;
	private int vanillaCloudRenderDistance;
	private int vanillaViewDistance;

	private record CloudGrid(boolean[][][] grid, int centerX, int centerZ) {}

	public synchronized void setRenderer(Config config) {
		cloudHeight = vanillaCloudHeight + config.getCloudHeightOffset();
		cloudLayerHeight = config.getCloudLayerHeight();
		renderDistance = config.isEnableRenderDistanceFitToView() ?
				vanillaViewDistance * 6 :
				config.getRenderDistance() >= 32 ?
						config.getRenderDistance() :
						vanillaCloudRenderDistance;
		cloudGridWidth = renderDistance * 2 + 1;
		if (cloudGrid != null)  //ensure gridX/Z isn't null. init grid with null x/z is useless
			cloudGrid = getCloudGrid(gridX, gridZ);
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
		cloudHeight = vanillaCloudHeight + CONFIG.getCloudHeightOffset();
	}

	protected void setSampler(long seed) {
		this.sampler = new SimplexNoiseSampler(Random.create(seed));
	}

	private CloudGrid getCloudGrid(int x, int z) {
		boolean[][][] grid = new boolean[cloudGridWidth][cloudGridWidth][cloudLayerHeight];
		int sx = x - renderDistance;
		int sz = z - renderDistance;
		World world = MinecraftClient.getInstance().player.getWorld();
		double time = world.getTime() / 20.0;
		//float threshold = 0.5f;  //original
		float threshold = CONFIG.getDensityThreshold();

		for(int cx = 0; cx < cloudGridWidth; cx++) {
			for(int cz = 0; cz < cloudGridWidth; cz++) {

				if (CONFIG.isEnableDynamic()) {
					float bx = (sx + cx + 0.5f) * CLOUD_BLOCK_WIDTH;  //transform gridPos to blockPos
					float bz = (sz + cz + 0.5f) * CLOUD_BLOCK_WIDTH;

					// calculating density...
					if (CONFIG.isEnableWeatherDensity()) {
						if (CONFIG.isEnableBiomeDensityByChunk()) {
							if (CONFIG.isEnableBiomeDensityUseLoadedChunk()) {
								Vec2f vec = new Vec2f(cx - cloudGridWidth / 2f, cz - cloudGridWidth / 2f).normalize();  // stepping pos near towards to player
								float bx2 = bx;
								float bz2 = bz;
								while (! world.getChunkManager().isChunkLoaded((int) bx2 / 16, (int) bz2 / 16)
										&& Math.abs(sx * cloudGridWidth - bx) + Math.abs(sz * cloudGridWidth - bz) > CLOUD_BLOCK_WIDTH * 4) {  //jump if too close
									bx2 -= vec.x * CLOUD_BLOCK_WIDTH;
									bz2 -= vec.y * CLOUD_BLOCK_WIDTH;
								}
								threshold = CONFIG.isFilterListHasNoBiome(world.getBiome(new BlockPos((int) bx2, 80, (int) bz2)))
										? getDensityThreshold(DATA.densityByWeather, world.getBiome(new BlockPos((int) bx2, 80, (int) bz2)).value().weather.downfall())
										: getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
							} else {
								threshold = CONFIG.isFilterListHasNoBiome(world.getBiome(new BlockPos((int) bx, 80, (int) bz)))
										? getDensityThreshold(DATA.densityByWeather, world.getBiome(new BlockPos((int) bx, 80, (int) bz)).value().weather.downfall())
										: getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
							}
						} else {
							threshold = getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
						}
					}

					// sampling...
					if (CONFIG.isEnableTerrainDodge()) {
						for (int cy = 0; cy < cloudLayerHeight; cy++) {

							// terrain dodge (detect light level)
							grid[cx][cz][cy] = world.getLightLevel(LightType.SKY, new BlockPos(
									(int) bx,
									(int) (cloudHeight + (cy - 1.5f) * CLOUD_BLOCK_HEIGHT),
									(int) bz  // cloud is moving, fix Z pos
							)) == 15 && getCloudSample(sampler, sx, sz, 0, time, cx, cy, cz, CONFIG.getSampleSteps()) > threshold;
						}
					} else {
						for (int cy = 0; cy < cloudLayerHeight; cy++) {
							grid[cx][cz][cy] = getCloudSample(sampler, sx, sz, 0, time, cx, cy, cz, CONFIG.getSampleSteps()) > threshold;
						}
					}
				} else {
					for (int cy = 0; cy < cloudLayerHeight; cy++) {
						grid[cx][cz][cy] = getCloudSample(sampler, sx, sz, 0, 0, cx, cy, cz, CONFIG.getSampleSteps()) > threshold;
					}
				}
			}
		}
		return new CloudGrid(grid, x, z);
	}

	private float getDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * densityByBiome;
	}

	private void updateCloudGrid() {
		CloudGrid newGrid = getCloudGrid(gridX, gridZ);
		synchronized (this) {
			if (cloudGrid != null) {
				int oldOffset = Math.abs(gridX - cloudGrid.centerX) + Math.abs(gridZ - cloudGrid.centerZ);
				int newOffset = Math.abs(gridX - newGrid.centerX) + Math.abs(gridZ - newGrid.centerZ);
				if (newOffset > oldOffset)
					return;  //pick grid closer to player
			}
			cloudGrid = newGrid;
			isResampling = false;
		}
	}

	public void stop() {
		try {
			if (resamplingThread != null)
				resamplingThread.join();
		} catch (Exception e) {
			//Ignore...
		}
	}

	/*
		- - - - - - - - - - - -
		Overwritten vanilla method
		to add height axis for render multi layer clouds.
		- - - - - - - - - - - -
	 */

	public void buildCloudCells(ByteBuffer byteBuffer, boolean isFancy) {
		if (cloudGrid == null)
			cloudGrid = getCloudGrid(gridX, gridZ);  //direct resample at first time
		if (gridX != cloudGrid.centerX || gridZ != cloudGrid.centerZ && !isResampling) {
			resamplingThread = new Thread(this::updateCloudGrid);
			resamplingThread.start();
			isResampling = true;
		}

		for(int l = 0; l <= 2 * renderDistance; ++l) {
			for(int xOffset = -l; xOffset <= l; ++xOffset) {
				int zOffset = l - Math.abs(xOffset);
				if (zOffset >= 0 && zOffset <= renderDistance && xOffset * xOffset + zOffset * zOffset <= renderDistance * renderDistance) {
					if (zOffset != 0) {
						int thickness = 0;  //sum thickness to change cloud brightness
						for (int h = cloudLayerHeight - 1; h >= 0; h--) {  //insert height traverse
							if (byteBuffer.remaining() < 24)
								return;  //java.nio.BufferOverflow Check, 24 is max put amount in single cell.
							if (this.method_72155(byteBuffer, isFancy, xOffset, h, -zOffset, renderDistance, thickness) && CONFIG.isEnableBottomDim()) {
								thickness++;
							} else {
								if (thickness > 0)
									thickness--;
							}
							if (!isFancy)
								break;  //FAST_CLOUD only draw single layer.
						}
					}
					int thickness = 0;
					for (int h = cloudLayerHeight - 1; h >= 0; h--) {
						if (byteBuffer.remaining() < 24)
							return;
						if (this.method_72155(byteBuffer, isFancy, xOffset, h, zOffset, renderDistance, thickness) && CONFIG.isEnableBottomDim()) {
							thickness++;
						} else {
							if (thickness > 0)
								thickness--;
						}
						if (!isFancy)
							break;
					}
				}
			}
		}

	}

	//return true if this grid has cells, to sum cloud thickness
	private boolean method_72155(ByteBuffer byteBuffer, boolean isFancy, int xOffset, int h, int zOffset, int renderDistance, int thickness) {
		int x = xOffset + renderDistance + gridX - cloudGrid.centerX;  //transform to grid pos
		int z = zOffset + renderDistance + gridZ - cloudGrid.centerZ;
		if (x < 0 || x >= cloudGrid.grid.length || z < 0 || z >= cloudGrid.grid.length)  //check bound
			return false;

		boolean state = cloudGrid.grid[x][z][h];
		if (!state)
			return false;  //jumping empty cell

		//check neighbor and push it to next
		boolean borderTop    = !(h + 1 <  cloudLayerHeight      && cloudGrid.grid[x][z][h + 1]);  //outOfBound || Not has neighbor -> built border
		boolean borderBottom = !(h - 1 >= 0                     && cloudGrid.grid[x][z][h - 1]);
		boolean borderEast   = !(x + 1 <  cloudGrid.grid.length && cloudGrid.grid[x + 1][z][h]);
		boolean borderWest   = !(x - 1 >= 0                     && cloudGrid.grid[x - 1][z][h]);
		boolean borderSouth  = !(z + 1 <  cloudGrid.grid.length && cloudGrid.grid[x][z + 1][h]);
		boolean borderNorth  = !(z - 1 >= 0                     && cloudGrid.grid[x][z - 1][h]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0);
		if (cellState == 0)
			return true;  //jumping cell which fully around by neighbor cells

		cellState |= (thickness << 6);
		h += (cloudHeight - vanillaCloudHeight) / CLOUD_BLOCK_HEIGHT;  //trans to offset
		if (isFancy) {
			this.buildCloudCellFancy(byteBuffer, xOffset, h, zOffset, cellState);
		} else {
			this.buildCloudCellFast(byteBuffer, xOffset, h, zOffset);
		}
		return true;
	}

	private void buildCloudCellFast(ByteBuffer byteBuffer, int x, int h, int z) {
		this.method_71098(byteBuffer, x, h, z, Direction.DOWN, 32, 0);
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
	private void method_71098(ByteBuffer byteBuffer, int x, int h, int z, Direction direction, int i, int thickness) {
		int l = direction.getIndex() | i;
		l |= (x & 1) << 7;
		l |= (z & 1) << 6;
		l |= (h & 1) << 3;  //add height odevity to l
		byteBuffer.put((byte)(x >> 1))
				.put((byte)(z >> 1))
				.put((byte)(h >> 1))
				.put((byte)l)
				.put((byte)thickness);
	}

	private void buildCloudCellFancy(ByteBuffer byteBuffer, int x, int h, int z, int cellState) {
		int thickness = cellState >> 6;
		if (hasBorderTop(cellState) && h <= this.gridY) {  //TODO: why needs "="?
			this.method_71098(byteBuffer, x, h, z, Direction.UP, 0, thickness);
		}
		if (hasBorderBottom(cellState) && h > this.gridY) {
			this.method_71098(byteBuffer, x, h, z, Direction.DOWN, 0, thickness);
		}
		if (hasBorderNorth(cellState) && z > 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.NORTH, 0, thickness);
		}
		if (hasBorderSouth(cellState) && z < 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.SOUTH, 0, thickness);
		}
		if (hasBorderWest(cellState) && x > 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.WEST, 0, thickness);
		}
		if (hasBorderEast(cellState) && x < 0) {
			this.method_71098(byteBuffer, x, h, z, Direction.EAST, 0, thickness);
		}
		if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && Math.abs(h) <= this.gridY) {  //inner faces
			Direction[] directions = Direction.values();
			for (Direction direction : directions) {
				this.method_71098(byteBuffer, x, h, z, direction, 16, thickness);
			}
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
