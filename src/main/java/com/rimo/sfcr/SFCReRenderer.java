package com.rimo.sfcr;

import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.mixin.ServerWorldAccessor;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.gen.random.SimpleRandom;
import net.minecraft.util.math.Vec3d;

public class SFCReRenderer {
	
	private SFCReConfig config = SFCReMod.CONFIG.getConfig();
	private static final boolean hasCloudsHeightModifier
			= FabricLoader.getInstance().isModLoaded("sodiumextra")
			||FabricLoader.getInstance().isModLoaded("raisedclouds");
	
	private static final float DENSITY_GATE_RANGE = 0.0500000f;
	private float cloudDensityByWeather = 0f;
	private float cloudDensityByBiome = 0f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	
	private int cloudRenderDistance = config.getCloudRenderDistance();
	private int cloudLayerThickness = config.getCloudLayerThickness();
	private int normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
	private int weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
	private int densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed()); 

	private final Identifier whiteTexture = new Identifier("sfcr", "white.png");

	public SimplexNoiseSampler cloudNoise = new SimplexNoiseSampler(new SimpleRandom(new Random().nextLong()).derive());

	public VertexBuffer cloudBuffer;

	public boolean[][][] _cloudData = new boolean[cloudRenderDistance][cloudLayerThickness][cloudRenderDistance];

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double partialOffset = 0;
	public double partialOffsetSecondary = 0;
	public int cloudRenderDistanceOffset = (cloudRenderDistance - 96) / 2 * 16;		//Idk why "*16" but it work fine.

	public double time;

	public int fullOffset = 0;

	public double xScroll;
	public double zScroll;

	public BufferBuilder cb;

	public void init() {
		cloudNoise = new SimplexNoiseSampler(new SimpleRandom(new Random().nextLong()).derive());
		isProcessingData = false;
	}

	@SuppressWarnings("resource")
	public void tick() {

		if (MinecraftClient.getInstance().player == null)
			return;
		
		if (!config.isEnableMod())
			return;
		
		if (!MinecraftClient.getInstance().world.getDimension().hasSkyLight())
			return;

		//If already processing, don't start up again.
		if (isProcessingData)
			return;

		var player = MinecraftClient.getInstance().player;
		var world = MinecraftClient.getInstance().isIntegratedServerRunning() ? 
				MinecraftClient.getInstance().getServer().getWorld(MinecraftClient.getInstance().world.getRegistryKey()) : 
				MinecraftClient.getInstance().world.getServer().getWorld(MinecraftClient.getInstance().world.getRegistryKey());
		var worldProperties = ((ServerWorldAccessor)world).getWorldProperties();
		var currentBiomeDownFall = world.getBiome(player.getBlockPos()).value().getDownfall();
		var xScroll = MathHelper.floor(player.getX() / 16) * 16;
		var zScroll = MathHelper.floor(player.getZ() / 16) * 16;

		int timeOffset = (int) (Math.floor(time / 6) * 6);
		
		//Detect Weather Change
		if (config.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = worldProperties.getThunderTime() / 20 < config.getWeatherPreDetectTime() 
						|| cloudDensityByWeather < config.getThunderDensityPercent() / 50f - DENSITY_GATE_RANGE;
			} else if (world.isRaining()) {
				isWeatherChange = worldProperties.getRainTime() / 20 < config.getWeatherPreDetectTime() 
						|| cloudDensityByWeather > config.getRainDensityPercent() / 50f + DENSITY_GATE_RANGE 
						|| cloudDensityByWeather < config.getRainDensityPercent() / 50f - DENSITY_GATE_RANGE;
			} else {		//Clear...
				if (worldProperties.getClearWeatherTime() != 0) {
					isWeatherChange = worldProperties.getClearWeatherTime() / 20 < config.getWeatherPreDetectTime();
				} else {
					isWeatherChange = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < config.getWeatherPreDetectTime() 
							|| cloudDensityByWeather > config.getCloudDensityPercent() / 50f + DENSITY_GATE_RANGE;
				}
			}
		} else {
			isWeatherChange = false;
		}
		//Detect Biome Change
		isBiomeChange = cloudDensityByBiome > currentBiomeDownFall + DENSITY_GATE_RANGE || cloudDensityByBiome < currentBiomeDownFall - DENSITY_GATE_RANGE; 

		//Refresh Processing...
		if (timeOffset != moveTimer || xScroll != this.xScroll || zScroll != this.zScroll) {
			moveTimer = timeOffset;
			isProcessingData = true;

			dataProcessThread = new Thread(() -> collectCloudData(xScroll, zScroll));
			dataProcessThread.start();
			
			//Density Change by Weather
			if (config.isEnableWeatherDensity()) {
				if (world.isThundering()) {
					if (worldProperties.getThunderTime() / 20 < config.getWeatherPreDetectTime()) {		//How to figure next weather rain or clear?
						cloudDensityByWeather += (config.getRainDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed;		//Ignored clearing...
					} else if (cloudDensityByWeather < config.getThunderDensityPercent() / 50f - DENSITY_GATE_RANGE) {
						cloudDensityByWeather += (config.getThunderDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed;
					} else {
						cloudDensityByWeather = config.getThunderDensityPercent() / 50f;
					}
				} else if (world.isRaining()) {
					if (worldProperties.getRainTime() / 20 < config.getWeatherPreDetectTime()) {		//How to figure next weather thunder or clear?
						cloudDensityByWeather += (config.getCloudDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed;		//Ignored thundering...
					} else {
						cloudDensityByWeather = cloudDensityByWeather > config.getRainDensityPercent() / 50f + DENSITY_GATE_RANGE 
								|| cloudDensityByWeather < config.getRainDensityPercent() / 50f - DENSITY_GATE_RANGE 
								? cloudDensityByWeather + (config.getRainDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed 
								: config.getRainDensityPercent() / 50f;
					}
				} else {		//Clear...
					if (worldProperties.getClearWeatherTime() != 0) {		//It's complex because if use /weather clear, time of thunder & rain always return 1; time of clear in neutral clear always return 0.
						cloudDensityByWeather = worldProperties.getClearWeatherTime() / 20 < config.getWeatherPreDetectTime() 
								? cloudDensityByWeather + (config.getRainDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed 
								: config.getCloudDensityPercent() / 50f;
					} else if (Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < config.getWeatherPreDetectTime()) {
						cloudDensityByWeather = worldProperties.getRainTime() < worldProperties.getThunderTime() 
								? cloudDensityByWeather + (config.getRainDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed 
								: cloudDensityByWeather + (config.getThunderDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed;
					} else {
						cloudDensityByWeather = cloudDensityByWeather > config.getCloudDensityPercent() / 50f + DENSITY_GATE_RANGE 
								? cloudDensityByWeather + (config.getCloudDensityPercent() / 50f - cloudDensityByWeather) / (float)densityChangingSpeed 
								: config.getCloudDensityPercent() / 50f;
					}
				}
			} else if (cloudDensityByWeather != config.getCloudDensityPercent() / 50) {		//Initialize if disabled detect in rain/thunder.
				cloudDensityByWeather = config.getCloudDensityPercent() / 50;
			}
			//Density Change by Biome
			cloudDensityByBiome = isBiomeChange ? cloudDensityByBiome + (currentBiomeDownFall - cloudDensityByBiome) / (float)densityChangingSpeed : currentBiomeDownFall;
				
			if (config.isEnableDebug()) {
				player.sendMessage(Text.of("[SFCRe] pre-time nT: " + worldProperties.getThunderTime() + ", nR: " + worldProperties.getRainTime() + ", nC: " + worldProperties.getClearWeatherTime()), false);
				player.sendMessage(Text.of("[SFCRe] changing W: " + isWeatherChange + ", B: " + isBiomeChange), false);
			}
		}
	}

	public void render(ClientWorld world, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) {
		
		float f = hasCloudsHeightModifier ? world.getDimensionEffects().getCloudsHeight() : config.getCloudHeight();
		
		if (!Float.isNaN(f)) {
			//Setup render system
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.blendFuncSeparate(
					GlStateManager.SrcFactor.SRC_ALPHA,
					GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SrcFactor.ONE,
					GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
			);
			RenderSystem.depthMask(true);

			Vec3d cloudColor = world.getCloudsColor(tickDelta);

			synchronized (this) {
				//Fix up partial offset...
				partialOffset += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
				partialOffsetSecondary += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;

				if ((!isWeatherChange || cloudDensityByBiome == 0) && (!isBiomeChange || config.getBiomeDensityMultipler() == 0)) {
					time += MinecraftClient.getInstance().getLastFrameDuration() / normalRefreshSpeed;		//20.0f for origin
				} else {
					time += MinecraftClient.getInstance().getLastFrameDuration() / weatheringRefreshSpeed;
				}
				
				var cb = cloudBuffer;

				if (cb == null && this.cb != null && !isProcessingData) {
					cloudBuffer = new VertexBuffer();
					cloudBuffer.bind();
					cloudBuffer.upload(this.cb);
					cb = cloudBuffer;
					VertexBuffer.unbind();
				}

				if (cb != null) {
					//Setup shader
					RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
					RenderSystem.setShaderTexture(0, whiteTexture);
					if (config.isEnableFog()) {
						BackgroundRenderer.setFogBlack();
						if (!config.isFogAutoDistance()) {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * config.getFogMinDistance());
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * config.getFogMaxDistance());
						} else {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * (float)Math.pow(cloudRenderDistance / 48, 2) / 2);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * (float)Math.pow(cloudRenderDistance / 48, 2));
						}
					} else {
						BackgroundRenderer.clearFog();
					}

					RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1);

					matrices.push();
					matrices.translate(-cameraX, -cameraY, -cameraZ);
					matrices.translate(xScroll - cloudRenderDistanceOffset, f - 15, zScroll + partialOffset - cloudRenderDistanceOffset);
					cb.bind();

					for (int s = 0; s < 2; ++s) {
						if (s == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						Shader shaderProgram = RenderSystem.getShader();
						cb.setShader(matrices.peek().getPositionMatrix(), projectionMatrix, shaderProgram);
					}

					VertexBuffer.unbind();
					matrices.pop();

					//Restore render system
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableCull();
					RenderSystem.disableBlend();
				}
			}
		}
	}

	public void clean() {

		try {
			if (dataProcessThread != null)
				dataProcessThread.join();
		} catch (Exception e) {
			//Ignore...
		}
	}

	public BufferBuilder builder = new BufferBuilder(2097152);
	public FloatArrayList vertexList = new FloatArrayList();
	public ByteArrayList normalList = new ByteArrayList();

	private final float[][] normals = {
			{1, 0, 0},
			{-1, 0, 0},
			{0, 1, 0},
			{0, -1, 0},
			{0, 0, 1},
			{0, 0, -1},
	};

	private final int[] colors = {
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), (int) (255 * 0.95f), (int) (255 * 0.9f), (int) (255 * 0.9f)),
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), (int) (255 * 0.75f), (int) (255 * 0.75f), (int) (255 * 0.75f)),
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), 255, 255, 255),
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), (int) (255 * 0.6f), (int) (255 * 0.6f), (int) (255 * 0.6f)),
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), (int) (255 * 0.92f), (int) (255 * 0.85f), (int) (255 * 0.85f)),
			ColorHelper.Argb.getArgb((int) (255 * 0.8f), (int) (255 * 0.8f), (int) (255 * 0.8f), (int) (255 * 0.8f)),
	};

	double remappedValue(double noise) {
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;
	}

	@SuppressWarnings("resource")
	private void collectCloudData(double scrollX, double scrollZ) {

		try {
			double startX = scrollX / 16;
			double startZ = scrollZ / 16;

			double timeOffset = Math.floor(time / 6) * 6;

			synchronized (this) {
				while (partialOffsetSecondary >= 16) {
					partialOffsetSecondary -= 16;
					fullOffset++;
				}
			}
			
			float baseFreq = 0.05f;
			float baseTimeFactor = 0.01f;

			float l1Freq = 0.09f;
			float l1TimeFactor = 0.02f;

			float l2Freq = 0.001f;
			float l2TimeFactor = 0.1f;
			
			var f = 1.5 - cloudDensityByWeather * (1 - (1 - cloudDensityByBiome) * config.getBiomeDensityMultipler() / 100);
			if (config.isEnableDebug())
				MinecraftClient.getInstance().player.sendMessage(Text.of("[SFCRe] density W: " + cloudDensityByWeather + ", B: " + cloudDensityByBiome + ", f: " + f), false);

			for (int cx = 0; cx < cloudRenderDistance; cx++) {
				for (int cy = 0; cy < cloudLayerThickness; cy++) {
					for (int cz = 0; cz < cloudRenderDistance; cz++) {
						double cloudVal = cloudNoise.sample(
								(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
								(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
								(startZ + cz - fullOffset) * baseFreq
						);
						if (config.getSampleSteps() > 1) {
							double cloudVal1 = cloudNoise.sample(
									(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
									(cy - (timeOffset * l1TimeFactor)) * l1Freq,
									(startZ + cz - fullOffset) * l1Freq
							);
							double cloudVal2 = 1;
							if (config.getSampleSteps() > 2) {
								cloudVal2 = cloudNoise.sample(
										(startX + cx + (timeOffset * l2TimeFactor)) * l2Freq,
										0,
										(startZ + cz - fullOffset) * l2Freq
								);
								
								//Smooth floor function...
								cloudVal2 *= 3;
								cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;
							}
	
							cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
						}

						cloudVal = cloudVal * remappedValue(1 - ((double) (cy + 1) / 32));		//cloudVal ~ [-1, 2]

						_cloudData[cx][cy][cz] = cloudVal > f;		//Original is 0.5f.
					}
				}
			}

			var tmp = rebuildCloudMesh();

			synchronized (this) {
				cb = tmp;
				cloudBuffer = null;

				this.xScroll = scrollX;
				this.zScroll = scrollZ;

				while (partialOffset >= 16) {
					partialOffset -= 16;
				}
			}
		} catch (Exception e) {
			// -- Ignore...
		}

		isProcessingData = false;
	}

	public void addVertex(float x, float y, float z) {
		vertexList.add(x - 48);
		vertexList.add(y);
		vertexList.add(z - 48);
	}

	private BufferBuilder rebuildCloudMesh() {

		vertexList.clear();
		normalList.clear();

		for (int cx = 0; cx < cloudRenderDistance; cx++) {
			for (int cy = 0; cy < cloudLayerThickness; cy++) {
				for (int cz = 0; cz < cloudRenderDistance; cz++) {
					if (!_cloudData[cx][cy][cz])
						continue;

					//Right
					if (cx == cloudRenderDistance - 1 || !_cloudData[cx + 1][cy][cz]) {
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
					if (cy == cloudLayerThickness - 1 || !_cloudData[cx][cy + 1][cz]) {
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
					if (cz == cloudRenderDistance - 1 || !_cloudData[cx][cy][cz + 1]) {
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

		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

		try {
			int vertCount = vertexList.size() / 3;

			for (int i = 0; i < vertCount; i++) {
				int origin = i * 3;
				var x = vertexList.getFloat(origin) * 16;
				var y = vertexList.getFloat(origin + 1) * 8;
				var z = vertexList.getFloat(origin + 2) * 16;

				int normIndex = normalList.getByte(i / 4);
				var norm = normals[normIndex];
				var nx = norm[0];
				var ny = norm[1];
				var nz = norm[2];

				builder.vertex(x, y, z).texture(0.5f, 0.5f).color(colors[normIndex]).normal(nx, ny, nz).next();
			}
		} catch (Exception e) {
			// -- Ignore...
			SFCReMod.LOGGER.error(e.toString());
		}

		builder.end();
		return builder;
	}
	
	//Update Setting.
	public void updateRenderData(SFCReConfig newConfig) {
		config = newConfig;
		cloudRenderDistance = config.getCloudRenderDistance();
		cloudLayerThickness = config.getCloudLayerThickness();
		normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
		weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
		densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed()); 
		
		_cloudData = new boolean[cloudRenderDistance][cloudLayerThickness][cloudRenderDistance];
		
		cloudRenderDistanceOffset = (cloudRenderDistance - 96) / 2 * 16;
	}
	
	//Push to Mixin.
	public int getFogDistance() {
		if (config.isEnableFog()) {
			return config.getFogMaxDistance();
		}
		return config.getMaxFogDistanceWhenNoFog();
	}
	//Push to Mixin.
	public boolean getModEnabled() {
		return config.isEnableMod();
	}
	
}
