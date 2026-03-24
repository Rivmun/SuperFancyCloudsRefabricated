package com.rimo.sfcr.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.rimo.sfcr.VersionUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

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

	public static Sampler sampler = new Sampler();
	protected volatile CloudGrid cloudGrid;  //replace vanilla CloudRenderer.cells
	private Thread resamplingThread;
	protected volatile boolean isResampling = false;
	protected double resamplingTimer = 0.0;
	private int rebuildTick = 0;
	protected float cloudBlockWidth = 12F;
	protected float cloudBlockHeight = 4F;
	protected int gridX, gridY, gridZ;  //camera position in cloudGrid
	protected float xOffset, zOffset;

	protected int debugBuiltCounter = 0;
	protected int debugCullCounter = 0;
	protected double debugBuiltTime = 0;

	protected record CloudGrid(boolean[][][] grids, int centerX, int centerZ) {}

	public Renderer() {}

	//copy constructor: use to convert renderer type
	public Renderer(Renderer renderer) {
		renderer.stop();
		this.cloudGrid = renderer.cloudGrid;
	}

	public boolean isCloudCovered(double x, double y, double z) {
		CloudGrid cloudGrid = this.cloudGrid;
		if (cloudGrid == null)
			return false;
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 camPos = camera.position();
		x = camPos.x - x - (xOffset + (gridX - cloudGrid.centerX - 0.33F) * cloudBlockWidth);  //trans to cloud relative...
		z = camPos.z - z - (zOffset + (gridZ - cloudGrid.centerZ - 0.33F) * cloudBlockWidth);
		int gx = (int) (cloudGrid.grids.length / 2F - x / cloudBlockWidth);
		int gy = (int) ((y - camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, VersionUtil.getLastFrameDuration())) / cloudBlockHeight);
		int gz = (int) (cloudGrid.grids.length / 2F - z / cloudBlockWidth);
		if (gx >= 0 && gx < cloudGrid.grids.length && gz >= 0 && gz < cloudGrid.grids.length) {
			for (int i = 0; i < cloudGrid.grids[0][0].length; i++) {
				if (cloudGrid.grids[gx][gz][i])
					return gy <= i;
			}
		}
		return false;
	}

	protected @Nullable CloudGrid getCloudGrid(int x, int z, int renderRange) {
		if (Minecraft.getInstance().player == null)
			return null;

		int gridWidth = renderRange * 2 + 1;
		boolean[][][] grid = new boolean[gridWidth][gridWidth][CONFIG.getCloudLayerThickness()];
		int sx = x - renderRange;  //make gridX/gridZ offsets to center of cloudGrid
		int sz = z - renderRange;
		float densityByWeather = DATA.densityByWeather;
		float densityByBiome = DATA.densityByBiome;

		for(int cx = 0; cx < grid.length; cx++) {
			for(int cz = 0; cz < grid.length; cz++) {
				for (int cy = 0; cy < grid[0][0].length; cy++) {
					grid[cx][cz][cy] = sampler.isGridHasCloud(sx + cx, cy, sz + cz, densityByWeather, densityByBiome);
				}
			}
		}
		return new CloudGrid(grid, x, z);
	}

	//thread-ify invoke is a better way to reduce lag.
	protected void updateCloudGrid(int renderRange) {
		CloudGrid newGrid = getCloudGrid(gridX, gridZ, renderRange);
		if (newGrid == null)
			return;
		if (cloudGrid != null) {
			int oldOffset = Math.abs(gridX - cloudGrid.centerX) + Math.abs(gridZ - cloudGrid.centerZ);
			int newOffset = Math.abs(gridX - newGrid.centerX) + Math.abs(gridZ - newGrid.centerZ);
			if (newOffset > oldOffset)
				return;  //pick grids closer to player
		}
		cloudGrid = newGrid;
	}

	protected void tryStartGridUpdateThread(int renderRange) {
		if (isResampling)
			return;
		isResampling = true;
		resamplingThread = new Thread(() -> {
			try {
				this.updateCloudGrid(renderRange);
			} catch (Exception e) {
				exceptionCatcher(e);
			} finally {
				synchronized (this) {
					isResampling = false;
					resamplingTimer = 0;
					rebuildTick = 99;
				}
			}
		});
		resamplingThread.start();
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

	public String getDebugString() {
		return "[SFCR] encode " + debugBuiltCounter + " face(s), " + debugCullCounter + " cell(s) skipped, cost " + debugBuiltTime + "ms.";
	}

	/*
		- - - - - - - - - - - -
		Overwritten vanilla method
		to add height axis for render multi layer clouds.
		these method only use by vanilla renderClouds.
		- - - - - - - - - - - -
	 */

	int quadCount = 0;

	public void render(int cloudColor, float cloudHeight, Vec3 camPos, float partialTick, MappableRingBuffer infoBuffer, MappableRingBuffer faceBuffer, int renderRange, Level level) {
		final boolean isPaused = Minecraft.getInstance().isPaused();
		cloudBlockWidth = CONFIG.getCloudBlockSize();
		cloudBlockHeight = cloudBlockWidth / 2;

		float timeOffset = level.getGameTime() + partialTick;
		double cloudX = camPos.x + timeOffset * 0.030000001F;
		double cloudZ = camPos.z + 3.96F;
		int gridX = Mth.floor(cloudX / cloudBlockWidth);
		int gridY = Mth.floor((camPos.y - cloudHeight) / cloudBlockHeight);
		int gridZ = Mth.floor(cloudZ / cloudBlockWidth);
		float offsetX = (float)(cloudX - (double)((float)gridX * cloudBlockWidth));
		float offsetY = (float)(cloudHeight - camPos.y);
		float offsetZ = (float)(cloudZ - (double)((float)gridZ * cloudBlockWidth));
		RenderPipeline renderPipeline = CONFIG.isEnableBottomDim() ? SUPER_FANCY_CLOUDS : SUPER_FANCY_CLOUDS_NOTHICKNESS;

		this.xOffset = offsetX;
		this.zOffset = offsetZ;

		// resampling check
		resamplingTimer += VersionUtil.getLastFrameDuration() * 0.25 * 0.25;
		if (! isPaused &&
				(gridX != this.gridX || gridZ != this.gridZ || isTimeToResampling())) {
			if (cloudGrid == null) {
				cloudGrid = getCloudGrid(gridX, gridZ, renderRange);  //resampling directly at first time
			} else {
				tryStartGridUpdateThread(renderRange);
			}
		}

		// rebuild check
		boolean enableCulling = CONFIG.getEnableViewCulling();
		if (! isPaused &&
				(gridX != this.gridX || gridZ != this.gridZ || (this.gridY != gridY && this.gridY >= 0 && this.gridY < CONFIG.getCloudLayerThickness() / 2) ||
				(enableCulling && ++ rebuildTick > CONFIG.getRebuildInterval()) || ! enableCulling && rebuildTick == 99)) {
			rebuildTick = 0;
			this.gridX = gridX;
			this.gridY = gridY;
			this.gridZ = gridZ;

			faceBuffer.rotate();
			try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(faceBuffer.currentBuffer(), false, true)) {
				buildMesh(mappedView.data(), cloudGrid, renderRange);
				quadCount = mappedView.data().position() / (renderPipeline == SUPER_FANCY_CLOUDS ? 5 : 4);
			}
		}

		// render
		if (quadCount != 0) {
			try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(infoBuffer.currentBuffer(), false, true)) {
				Std140Builder.intoBuffer(mappedView.data()).putVec4(ARGB.vector4fFromARGB32(cloudColor)).putVec3(-offsetX, offsetY, -offsetZ).putVec3(cloudBlockWidth, cloudBlockHeight, cloudBlockWidth);
			}

			GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
			RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
			RenderTarget renderTarget2 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
			RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
			GpuBuffer gpuBuffer = autoStorageIndexBuffer.getBuffer(6 * quadCount);
			GpuTextureView gpuTextureView;
			GpuTextureView gpuTextureView2;
			if (renderTarget2 != null) {
				gpuTextureView = renderTarget2.getColorTextureView();
				gpuTextureView2 = renderTarget2.getDepthTextureView();
			} else {
				gpuTextureView = renderTarget.getColorTextureView();
				gpuTextureView2 = renderTarget.getDepthTextureView();
			}

			try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Clouds", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
				renderPass.setPipeline(renderPipeline);
				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
				renderPass.setIndexBuffer(gpuBuffer, autoStorageIndexBuffer.type());
				renderPass.setUniform("CloudInfo", infoBuffer.currentBuffer());
				renderPass.setUniform("CloudFaces", faceBuffer.currentBuffer());
				renderPass.drawIndexed(0, 0, 6 * quadCount, 1);
			}
		}
	}

	private void buildMesh(ByteBuffer byteBuffer, CloudGrid cloudGrid, int renderRange) {
		if (cloudGrid == null)
			return;
		debugBuiltCounter = 0;
		debugCullCounter = 0;
		debugBuiltTime = System.nanoTime();

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
				multiplier *= client.player.getFieldOfViewModifier(true, VersionUtil.getLastFrameDuration());
			tanHalfFov = Math.tan(Math.toRadians(client.options.fov().get() * multiplier) / 2F);
			tanHalfFovHorizontal = tanHalfFov * client.getWindow().getWidth() / client.getWindow().getHeight();
		}

		for(int l = 0; l <= 2 * renderRange; ++l) {
			for(int xOffset = -l; xOffset <= l; ++xOffset) {
				int zOffset = l - Math.abs(xOffset);
				if (zOffset >= 0 && zOffset <= renderRange && xOffset * xOffset + zOffset * zOffset <= renderRange * renderRange) {
					if (zOffset != 0) {
						tryBuildCellProxy(byteBuffer, xOffset, -zOffset, renderRange, cloudGrid,
								look, up, left, tanHalfFov, tanHalfFovHorizontal);
					}
					tryBuildCellProxy(byteBuffer, xOffset, zOffset, renderRange, cloudGrid,
							look, up, left, tanHalfFov, tanHalfFovHorizontal);
				}
			}
		}
		debugBuiltTime = (System.nanoTime() - debugBuiltTime) / 1000000F;
	}

	private void tryBuildCellProxy(ByteBuffer byteBuffer, int xOffset, int zOffset, int renderDistance, CloudGrid cloudGrid,
	                               Vec3 look, Vec3 up, Vec3 left, double tanHalfFov, double tanHalfFovHorizontal) {
		int thickness = 0;
		int x = xOffset + renderDistance + gridX - cloudGrid.centerX;  //transform to grids index
		int z = zOffset + renderDistance + gridZ - cloudGrid.centerZ;
		for (int h = cloudGrid.grids[0][0].length - 1; h >= 0; h--) {
			if (byteBuffer.remaining() < 30)
				return;  //java.nio.BufferOverflow Check, it must have 30 bytes (.put() amount x 6 faces in encodeFaces()).
			if (x < 0 || x >= cloudGrid.grids.length || z < 0 || z >= cloudGrid.grids.length || h >= cloudGrid.grids[x][z].length)   //check bound
				continue;
			if (! cloudGrid.grids[x][z][h]) {  //jumping empty cell
				if (thickness > 0)
					thickness --;
				continue;
			}
			if (look != null && ! isInView(xOffset, h, zOffset, look, up, left, tanHalfFov, tanHalfFovHorizontal)) {
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
		x *= cloudBlockWidth;
		y = (int) ((y - gridY) * cloudBlockHeight);
		z *= cloudBlockWidth;
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
		if (hasBorderTop(cellState) && h < gridY)
			encodeFace(byteBuffer, x, h, z, Direction.UP, 0, thickness);
		if (hasBorderBottom(cellState) && h > gridY)
			encodeFace(byteBuffer, x, h, z, Direction.DOWN, 0, thickness);
		if (hasBorderNorth(cellState) && z > 0)
			encodeFace(byteBuffer, x, h, z, Direction.NORTH, 0, thickness);
		if (hasBorderSouth(cellState) && z < 0)
			encodeFace(byteBuffer, x, h, z, Direction.SOUTH, 0, thickness);
		if (hasBorderWest(cellState) && x > 0)
			encodeFace(byteBuffer, x, h, z, Direction.WEST, 0, thickness);
		if (hasBorderEast(cellState) && x < 0)
			encodeFace(byteBuffer, x, h, z, Direction.EAST, 0, thickness);
		if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && h == gridY) {  //inner faces
			Direction[] directions = Direction.values();
			for (Direction direction : directions) {
				encodeFace(byteBuffer, x, h, z, direction, 16, thickness);
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

}
