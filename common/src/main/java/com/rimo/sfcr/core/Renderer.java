package com.rimo.sfcr.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.rimo.sfcr.config.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.*;

public class Renderer {
	//Create custom renderPipeline
	//when renderClouds init cloud pipeline, we redirect it by Mixin.
	@SuppressWarnings("RedundantArrayCreation")
	public static final RenderPipeline SUPER_FANCY_CLOUDS = RenderPipeline
			.builder(new RenderPipeline.Snippet[]{RenderPipeline
					.builder(new RenderPipeline.Snippet[]{RenderPipelines.MATRICES_FOG_SNIPPET})
					.withVertexShader("core/rendertype_superfancyclouds")  //use our own .vsh
					.withFragmentShader("core/rendertype_clouds")
					.withBlend(BlendFunction.TRANSLUCENT)
					.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.QUADS)
					.withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
					.withUniform("CloudFaces",UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
					.buildSnippet()
			})
			.withLocation("pipeline/clouds")
			.build();
	@SuppressWarnings("RedundantArrayCreation")
	public static final RenderPipeline SUPER_FANCY_CLOUDS_NOTHICKNESS = RenderPipeline
			.builder(new RenderPipeline.Snippet[]{RenderPipeline
					.builder(new RenderPipeline.Snippet[]{RenderPipelines.MATRICES_FOG_SNIPPET})
					.withVertexShader("core/rendertype_superfancyclouds_nth")
					.withFragmentShader("core/rendertype_clouds")
					.withBlend(BlendFunction.TRANSLUCENT)
					.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.QUADS)
					.withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
					.withUniform("CloudFaces",UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
					.buildSnippet()
			})
			.withLocation("pipeline/clouds")
			.build();

	private SimplexNoise sampler;
	protected int gridX, gridY, gridZ;  //camera position in cloudGrid
	private float cloudHeight;
	protected volatile CloudGrid cloudGrid;  //replace vanilla CloudRenderer.cells
	private Thread resamplingThread;
	protected volatile boolean isResampling = false;
	private volatile double resamplingTimer = 0.0;
	private int renderDistance;
	private int cloudGridWidth;
	private int cloudThickness;
	public int debugBuiltCounter = 0;
	public int debugCullCounter = 0;

	private float vanillaCloudHeight;
	private int vanillaCloudRenderDistance;
	private int vanillaViewDistance;

	// static size
	public float getCloudBlockWidth() {return 12F;}
	public float getCloudBlockHeight() {return 4F;}

	protected record CloudGrid(boolean[][][] grids, int centerX, int centerZ) {}

	public Renderer() {}

	//copy constructor: use to convert renderer type
	public Renderer(Renderer renderer) {
		renderer.stop();
		this.sampler = renderer.sampler;
		this.setGridPos(renderer.gridX, renderer.gridY, renderer.gridZ);
		this.setCloudHeight(renderer.cloudHeight - CONFIG.getCloudHeightOffset() * getCloudBlockHeight());
		this.setRenderDistance(renderer.vanillaViewDistance, renderer.vanillaCloudRenderDistance);
	}

	public synchronized void setRenderer(Config config) {
		cloudHeight = vanillaCloudHeight + config.getCloudHeightOffset() * getCloudBlockHeight();
		cloudThickness = config.getCloudLayerThickness();
		renderDistance = config.isCloudRenderDistanceFitToView() ?
				vanillaViewDistance * 6 :
				config.getCloudRenderDistance() >= 32 ?
						config.getCloudRenderDistance() :
						vanillaCloudRenderDistance;
		cloudGridWidth = this.getRenderDistance() * 2 + 1;
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
	protected int getRenderDistance() {
		return this.renderDistance;
	}

	public void setCloudHeight(float height) {
		vanillaCloudHeight = height;
		cloudHeight = vanillaCloudHeight + CONFIG.getCloudHeightOffset() * getCloudBlockHeight();
	}
	public float getCloudHeight() {
		return this.cloudHeight;
	}

	public void initSampler(long seed) {
		this.sampler = new SimplexNoise(RandomSource.create(seed));
	}

	public void counting(double time) {
		this.resamplingTimer += time;
	}

	public boolean isCloudCovered(double x, double y, double z) {
		CloudGrid cloudGrid = this.cloudGrid;
		if (cloudGrid == null)
			return false;
		Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		int gx = (int) (cloudGrid.grids.length / 2F - (camPos.x() - x) / getCloudBlockWidth());
		int gy = (int) ((y - cloudHeight) / getCloudBlockHeight());
		int gz = (int) (cloudGrid.grids.length / 2F - (camPos.z() - z) / getCloudBlockWidth());
		if (gx >= 0 && gx < cloudGrid.grids.length && gz >= 0 && gz < cloudGrid.grids.length) {
			for (int i = 0; i < cloudGrid.grids[0][0].length; i++) {
				if (cloudGrid.grids[gx][gz][i])
					return gy <= i;
			}
		}
		return false;
	}

	protected @Nullable CloudGrid getCloudGrid(int x, int z) {
		if (Minecraft.getInstance().player == null)
			return null;

		boolean[][][] grid = new boolean[cloudGridWidth][cloudGridWidth][cloudThickness];
		int sx = x - this.getRenderDistance();  //make gridX/gridZ offsets to center of cloudGrid
		int sz = z - this.getRenderDistance();
		Level world = Minecraft.getInstance().player.level();
		double time = world.getGameTime() / 20.0;
		float threshold = 0.5f;  //original

		boolean isEnableDynamic = CONFIG.isEnableWeatherDensity();
		boolean isBiomeByChunk = CONFIG.isBiomeDensityByChunk();
		boolean isBiomeUseLoadedChunk = CONFIG.isBiomeDensityUseLoadedChunk();
		boolean isEnableTerrainDodge = CONFIG.isEnableTerrainDodge();
		float densityMultiplier = isEnableDynamic ? getDensityMultiplier(world.getDayTime()) : 1;
		int steps = CONFIG.getSampleSteps();

		for(int cx = 0; cx < cloudGridWidth; cx++) {
			for(int cz = 0; cz < cloudGridWidth; cz++) {

				int bx = (int) ((sx + cx + 0.5f) * getCloudBlockWidth());		// transform cloudpos to blockpos
				int bz = (int) ((sz + cz + 0.5f) * getCloudBlockWidth());
				final int h = world.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);  //height sampling use to get biome

				// calculating density...
				if (isEnableDynamic && isBiomeByChunk) {
					BlockPos pos;
					if (isBiomeUseLoadedChunk) {
						Vec2 cellPos = new Vec2(bx, bz);
						Vec2 unit = cellPos.normalized().scale(16);  //measure as chunk
						while (!world.hasChunk((int) cellPos.x / 16, (int) cellPos.y / 16)
								&& unit.dot(cellPos) > 0)  //end when cellPos was reversed.
							cellPos = cellPos.add(unit.negated());  // stepping pos near towards to player

						pos = new BlockPos((int) cellPos.x, h, (int) cellPos.y);
					} else {
						pos = new BlockPos(bx, h, bz);
					}
					Holder<Biome> biome = world.getBiome(pos);
					threshold = ! CONFIG.isFilterListHasBiome(biome)
							? getDensityThreshold(DATA.densityByWeather, CONFIG.getDownfall(biome.value().getPrecipitationAt(pos, world.getSeaLevel())))
							: getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
				} else {
					threshold = getDensityThreshold(DATA.densityByWeather, DATA.densityByBiome);
				}

				// sampling...
				for (int cy = 0; cy < cloudThickness; cy++) {
					grid[cx][cz][cy] = getCloudSampleProxy(world, sx, sz, 0, time, steps, cx, cy, cz) * densityMultiplier > threshold && (
							// terrain dodge (detect light level)
							! isEnableTerrainDodge || world.getBrightness(LightLayer.SKY, new BlockPos(
									bx,
									(int) (getCloudHeight() + (cy - 2) * getCloudBlockHeight()),
									bz
							)) == 15
					);
				}
			}
		}
		return new CloudGrid(grid, x, z);
	}

	private float getDensityThreshold(float densityByWeather, float densityByBiome) {
		return CONFIG.getDensityThreshold() - CONFIG.getThresholdMultiplier() * densityByWeather * densityByBiome;
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

	private double getCloudSampleProxy(Level world, double startX, double startZ, double zOffset, double timeOffset, int steps, double cx, double cy, double cz) {
		double sample = getCloudSample(sampler, startX, startZ, zOffset, timeOffset, cx, cy, cz, steps);
		if (world.isRaining() && CONFIG.isEnableWeatherDensity()) {  //make cloud top more continuous when rain
			float clearDensity = CONFIG.getCloudDensityPercent() / 100f + 1;
			float currentDensity = DATA.densityByWeather + 1;
			float cyMax = CONFIG.getCloudLayerThickness();
			final float MAX_ADD = 3F;
			sample += Math.pow(cy / cyMax, 2) * Math.max((1 - clearDensity / Math.max(clearDensity, currentDensity)) * MAX_ADD, 0);
		}
		return sample;
	}

	//method below extract from getCloudGrid use to override by dhCompat, to redirect these value.
//	protected int getVanillaViewDistance() {return this.vanillaViewDistance;}
//	protected boolean isChunkLoaded(Level world, int x, int z) {return world.hasChunk(x, z);}
//	protected Holder<Biome> getBiome(Level world, int x, int z) {return world.getBiome(new BlockPos(x, 80, z));}

	//thread-ify invoke is a better way to reduce lag.
	protected void updateCloudGrid() {
		CloudGrid newGrid = getCloudGrid(gridX, gridZ);
		if (newGrid == null)
			return;
		if (cloudGrid != null) {
			int oldOffset = Math.abs(gridX - cloudGrid.centerX) + Math.abs(gridZ - cloudGrid.centerZ);
			int newOffset = Math.abs(gridX - newGrid.centerX) + Math.abs(gridZ - newGrid.centerZ);
			if (newOffset > oldOffset)
				return;  //pick grids closer to player
		}
		synchronized (this) {
			cloudGrid = newGrid;
		}
	}

	protected void tryStartGridUpdateThread() {
		if (isResampling)
			return;
		isResampling = true;
		resamplingThread = new Thread(() -> {
			try {
				this.updateCloudGrid();
			} catch (Exception e) {
				exceptionCatcher(e);
			} finally {
				synchronized (this) {
					isResampling = false;
					resamplingTimer = 0.0;
				}
			}
		});
		resamplingThread.start();
	}

	protected boolean isGridNeedToUpdate() {
		return gridX != cloudGrid.centerX || gridZ != cloudGrid.centerZ;
	}

	public boolean isTimeToResampling() {
		return resamplingTimer > DATA.getResamplingInterval();
	}

	public void stop() {
		try {
			if (resamplingThread != null)
				resamplingThread.join();
		} catch (Exception e) {
			//Ignore...
		}
		cloudGrid = null;
	}

	/*
		- - - - - - - - - - - -
		Overwritten vanilla method
		to add height axis for render multi layer clouds.
		these method only use by vanilla renderClouds.
		- - - - - - - - - - - -
	 */

	public void buildMesh(ByteBuffer byteBuffer) {
		if (this.cloudGrid == null)
			this.cloudGrid = getCloudGrid(gridX, gridZ);  //resampling directly at first time
		CloudGrid cloudGrid;  //make a snapshot to prevent concurrent violate
		cloudGrid = this.cloudGrid;
		if (cloudGrid == null)
			return;
		if (RENDERER.isTimeToResampling())
			tryStartGridUpdateThread();  //vanilla renderer already checked grid pos
		debugBuiltCounter = 0;
		debugCullCounter = 0;

		Vec3 look = null, up = null, left = null;
		double tanHalfFov = 0, tanHalfFovHorizontal = 0;
		if (CONFIG.getEnableViewCulling()) {
			Minecraft client = Minecraft.getInstance();
			Camera cam = client.gameRenderer.getMainCamera();
			look = new Vec3(cam.forwardVector());
			up =   new Vec3(cam.upVector());
			left = new Vec3(cam.leftVector());
			float multiplier = CONFIG.getCullRadianMultiplier();
			if (client.player != null)
				multiplier *= client.player.getFieldOfViewModifier(true, client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			tanHalfFov = Math.tan(Math.toRadians(client.options.fov().get() * multiplier) / 2F);
			tanHalfFovHorizontal = tanHalfFov * client.getWindow().getWidth() / client.getWindow().getHeight();
		}

		for(int l = 0; l <= 2 * renderDistance; ++l) {
			for(int xOffset = -l; xOffset <= l; ++xOffset) {
				int zOffset = l - Math.abs(xOffset);
				if (zOffset >= 0 && zOffset <= renderDistance && xOffset * xOffset + zOffset * zOffset <= renderDistance * renderDistance) {
					if (zOffset != 0) {
						tryBuildCellProxy(byteBuffer, xOffset, -zOffset, renderDistance, cloudGrid,
								look, up, left, tanHalfFov, tanHalfFovHorizontal);
					}
					tryBuildCellProxy(byteBuffer, xOffset, zOffset, renderDistance, cloudGrid,
							look, up, left, tanHalfFov, tanHalfFovHorizontal);
				}
			}
		}
	}

	private void tryBuildCellProxy(ByteBuffer byteBuffer, int xOffset, int zOffset, int renderDistance, CloudGrid cloudGrid,
	                               Vec3 look, Vec3 up, Vec3 left, double tanHalfFov, double tanHalfFovHorizontal) {
		int thickness = 0;
		int x = xOffset + renderDistance + gridX - cloudGrid.centerX;  //transform to grids index
		int z = zOffset + renderDistance + gridZ - cloudGrid.centerZ;
		for (int h = cloudThickness - 1; h >= 0; h--) {
			if (byteBuffer.remaining() < 30)
				return;  //java.nio.BufferOverflow Check, it must have 30 bytes (.put() amount x 6 faces in encodeFaces()).
			if (x < 0 || x >= cloudGrid.grids.length || z < 0 || z >= cloudGrid.grids.length || h >= cloudGrid.grids[x][z].length)   //check bound
				continue;
			if (! cloudGrid.grids[x][z][h]) {  //jumping empty cell
				if (thickness > 0)
					thickness --;
				continue;
			}
			if (CONFIG.getEnableViewCulling() && ! isInView(xOffset, h, zOffset, look, up, left, tanHalfFov, tanHalfFovHorizontal)) {
				debugCullCounter++;
				continue;
			}
			tryBuildCell(byteBuffer, x, h, z, renderDistance, thickness, cloudGrid);
			thickness ++;
		}
	}

	// view culling (on block, not accurate)
	// call stack is too deep, I'm lazy to do culling on face...
	private boolean isInView(int x, int y, int z, Vec3 look, Vec3 up, Vec3 left, double tanHalfFov, double tanHalfFovHorizontal) {
		x *= getCloudBlockWidth();
		y = (int) ((y - gridY) * getCloudBlockHeight());
		z *= getCloudBlockWidth();
		Vec3 cloudVec = new Vec3(x, y, z);
		double depth = look.dot(cloudVec);
		return depth > 0.05F &&
				(Math.abs(up.dot(cloudVec)) / depth < tanHalfFov) &&
				(Math.abs(left.dot(cloudVec)) / depth < tanHalfFovHorizontal);
	}

	private void tryBuildCell(ByteBuffer byteBuffer, int x, int h, int z, int renderDistance, int thickness, CloudGrid cloudGrid) {
		//check neighbor and push it to next
		boolean borderTop    = !(h + 1 <  cloudGrid.grids[x][z].length && cloudGrid.grids[x][z][h + 1]);  //outOfBound || Not has neighbor -> built border
		boolean borderBottom = !(h - 1 >= 0                            && cloudGrid.grids[x][z][h - 1]);
		boolean borderEast   = !(x + 1 <  cloudGrid.grids.length       && cloudGrid.grids[x + 1][z][h]);
		boolean borderWest   = !(x - 1 >= 0                            && cloudGrid.grids[x - 1][z][h]);
		boolean borderSouth  = !(z + 1 <  cloudGrid.grids.length       && cloudGrid.grids[x][z + 1][h]);
		boolean borderNorth  = !(z - 1 >= 0                            && cloudGrid.grids[x][z - 1][h]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0);

		cellState |= (thickness << 6);
		h += CONFIG.getCloudHeightOffset();
		x -= renderDistance + gridX - cloudGrid.centerX;  //transform to relative pos
		z -= renderDistance + gridZ - cloudGrid.centerZ;
		this.buildExtrudedCell(byteBuffer, x, h, z, cellState);
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
	private void encodeFace(ByteBuffer byteBuffer, int x, int h, int z, Direction direction, int i, int thickness) {
		int l = direction.ordinal() | i;
		l |= (x & 1) << 7;
		l |= (z & 1) << 6;
		l |= (h & 1) << 3;  //add height odevity to l
		byteBuffer.put((byte)(x >> 1))
				.put((byte)(z >> 1))
				.put((byte)(h >> 1))
				.put((byte)l);
		if (CONFIG.isEnableBottomDim())
			byteBuffer.put((byte)thickness);
		debugBuiltCounter ++;
	}

	private void buildExtrudedCell(ByteBuffer byteBuffer, int x, int h, int z, int cellState) {
		int thickness = cellState >> 6;
		if (hasBorderTop(cellState) && h < this.gridY) {
			this.encodeFace(byteBuffer, x, h, z, Direction.UP, 0, thickness);
		}
		if (hasBorderBottom(cellState) && h > this.gridY) {
			this.encodeFace(byteBuffer, x, h, z, Direction.DOWN, 0, thickness);
		}
		if (hasBorderNorth(cellState) && z > 0) {
			this.encodeFace(byteBuffer, x, h, z, Direction.NORTH, 0, thickness);
		}
		if (hasBorderSouth(cellState) && z < 0) {
			this.encodeFace(byteBuffer, x, h, z, Direction.SOUTH, 0, thickness);
		}
		if (hasBorderWest(cellState) && x > 0) {
			this.encodeFace(byteBuffer, x, h, z, Direction.WEST, 0, thickness);
		}
		if (hasBorderEast(cellState) && x < 0) {
			this.encodeFace(byteBuffer, x, h, z, Direction.EAST, 0, thickness);
		}
		if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && Math.abs(h) <= this.gridY) {  //inner faces
			Direction[] directions = Direction.values();
			for (Direction direction : directions) {
				this.encodeFace(byteBuffer, x, h, z, direction, 16, thickness);
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

	private static double getCloudSample(SimplexNoise sampler, double startX, double startZ, double zOffset, double timeOffset, double cx, double cy, double cz, int step) {
		double cloudVal = sampler.getValue(
				(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
				(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
				(startZ + cz - zOffset) * baseFreq
		);
		if (step > 1) {
			double cloudVal1 = sampler.getValue(
					(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
					(cy - (timeOffset * l1TimeFactor)) * l1Freq,
					(startZ + cz - zOffset) * l1Freq
			);
			double cloudVal2 = 1;
			if (step > 2) {
				cloudVal2 = sampler.getValue(
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
