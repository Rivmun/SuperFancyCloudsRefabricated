package com.rimo.sfcr;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.config.WeatherType;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Vec3d;

public class SFCReRenderer {
	
	private static final SFCReRuntimeData RUNTIME = SFCReMain.RUNTIME;
	private static final SFCReConfig CONFIG = SFCReMain.CONFIGHOLDER.getConfig();

	private static final boolean hasCloudsHeightModifier
			= FabricLoader.getInstance().isModLoaded("sodium-extra");
	
	private static final float DENSITY_GATE_RANGE = 0.0500000f;
	private float cloudDensityByWeather = 0f;
	private float cloudDensityByBiome = 0f;
	private float targetDownFall = 1f;
	private int cloudColorModifier = -1;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	
	private int cloudRenderDistance = CONFIG.getCloudRenderDistance();
	private int cloudLayerThickness = CONFIG.getCloudLayerThickness();
	private int normalRefreshSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed());
	private int weatheringRefreshSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getWeatherRefreshSpeed()) / 2;
	private int densityChangingSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getDensityChangingSpeed()) / 2;

	private final Identifier whiteTexture = new Identifier("sfcr", "white.png");

	public SimplexNoiseSampler cloudNoise = new SimplexNoiseSampler(Random.create(RUNTIME.seed));

	public VertexBuffer cloudBuffer;

	public boolean[][][] _cloudData = new boolean[cloudRenderDistance][cloudLayerThickness][cloudRenderDistance];

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double partialOffsetSecondary = 0;
	public double cloudRenderDistanceOffset = (cloudRenderDistance - 96) / 2f * 16;

	public double time;

	public double xScroll;
	public double zScroll;

	public BufferBuilder.BuiltBuffer cb;

	public void init() {
		cloudNoise = new SimplexNoiseSampler(Random.create(RUNTIME.seed));
		isProcessingData = false;
	}

	@SuppressWarnings("resource")
	public void tick() {

		if (MinecraftClient.getInstance().player == null)
			return;
		
		if (!CONFIG.isEnableMod())
			return;
		
		if (MinecraftClient.getInstance().isIntegratedServerRunning() && MinecraftClient.getInstance().isPaused())
			return;

		if (!MinecraftClient.getInstance().world.getDimension().hasSkyLight())
			return;

		//If already processing, don't start up again.
		if (isProcessingData)
			return;

		var player = MinecraftClient.getInstance().player;
		var world = MinecraftClient.getInstance().world;
		var xScroll = MathHelper.floor(player.getX() / 16) * 16;
		var zScroll = MathHelper.floor(player.getZ() / 16) * 16;

		int timeOffset = (int) (Math.floor(time / 6) * 6);
		
		//Detect Weather Change
		if (!MinecraftClient.getInstance().isIntegratedServerRunning())
			RUNTIME.clientTick(world);
		if (CONFIG.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.THUNDER && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather < CONFIG.getThunderDensityPercent() / 50f - DENSITY_GATE_RANGE;
			} else if (world.isRaining()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.RAIN && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather > CONFIG.getRainDensityPercent() / 50f + DENSITY_GATE_RANGE || cloudDensityByWeather < CONFIG.getRainDensityPercent() / 50f - DENSITY_GATE_RANGE;
			} else {		//Clear...
				isWeatherChange = RUNTIME.nextWeather != WeatherType.CLEAR && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather > CONFIG.getCloudDensityPercent() / 50f + DENSITY_GATE_RANGE;
			}
		} else {
			isWeatherChange = false;
		}
		
		//Detect Biome Change (why biome registry name so difficult to access...
		if (!CONFIG.getBiomeFilterList().contains(world.getBiome(player.getBlockPos()).getKey().get().getValue().toString()))
			targetDownFall = world.getBiome(player.getBlockPos()).value().getDownfall();
		isBiomeChange = cloudDensityByBiome > targetDownFall + DENSITY_GATE_RANGE || cloudDensityByBiome < targetDownFall - DENSITY_GATE_RANGE; 

		//Refresh Processing...
		if (timeOffset != moveTimer || xScroll != this.xScroll || zScroll != this.zScroll) {
			moveTimer = timeOffset;
			isProcessingData = true;

			dataProcessThread = new Thread(() -> collectCloudData(xScroll, zScroll));
			dataProcessThread.start();
			
			//Density Change by Weather
			if (CONFIG.isEnableWeatherDensity()) {
				if (isWeatherChange) {
					switch (RUNTIME.nextWeather) {
					case THUNDER:
						cloudDensityByWeather = nextDensityStep(CONFIG.getThunderDensityPercent() / 50f, cloudDensityByWeather, densityChangingSpeed);
						break;
					case RAIN:
						cloudDensityByWeather = nextDensityStep(CONFIG.getRainDensityPercent() / 50f, cloudDensityByWeather, densityChangingSpeed);
						break;
					case CLEAR:
						cloudDensityByWeather = nextDensityStep(CONFIG.getCloudDensityPercent() / 50f, cloudDensityByWeather, densityChangingSpeed);
						break;
					}
				}
			} else if (cloudDensityByWeather != CONFIG.getCloudDensityPercent() / 50f) {		//Initialize if disabled detect in rain/thunder.
				cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 50f;
			}
			
			//Density Change by Biome
			cloudDensityByBiome = isBiomeChange ? nextDensityStep(targetDownFall, cloudDensityByBiome, densityChangingSpeed) : targetDownFall;
			
			//Color Change by Time
			var t = (int)(world.getTimeOfDay() % 24000);
			if (t > 22500 || t < 500) {		//Dawn, scale param in [0, 2000]
				t = t > 22500 ? t - 22000 : t + 1500;
				cloudColorModifier = ColorHelper.Argb.getArgb(
						(int)(255 * 1f), 
						(int)(255 * (1 - Math.sin(t / 2000d * Math.PI) / 8)), 
						(int)(255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1)), 
						(int)(255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6))
					);
			} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
				t -= 11500;
				cloudColorModifier = ColorHelper.Argb.getArgb(
						(int)(255 * 1f), 
						(int)(255 * (1 - Math.sin(t / 2000d * Math.PI) / 8)), 
						(int)(255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1)), 
						(int)(255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6))
					);
			} else {
				cloudColorModifier = ColorHelper.Argb.getArgb(255, 255, 255, 255);
			}
			
			if (CONFIG.isEnableDebug()) {
				//SFCReMain.LOGGER.info("[SFCRe] pre-time nT: " + worldProperties.getThunderTime() + ", nR: " + worldProperties.getRainTime() + ", nC: " + worldProperties.getClearWeatherTime());
				player.sendMessage(Text.of("[SFCRe] changing W: " + isWeatherChange + ", B: " + isBiomeChange));
				//SFCReMain.LOGGER.info("[SFCRe] color: " + world.getTimeOfDay() + ", " + t + ", " + ColorHelper.Argb.getRed(cloudColorModifier) + ", " + ColorHelper.Argb.getGreen(cloudColorModifier) + ", " + ColorHelper.Argb.getBlue(cloudColorModifier));
			}
		}
	}

	public void render(ClientWorld world, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) {
		
		float f;
		if ((!MinecraftClient.getInstance().isIntegratedServerRunning() && CONFIG.isEnableServerConfig())
				|| (!MinecraftClient.getInstance().isIntegratedServerRunning() && !CONFIG.isEnableServerConfig() && !hasCloudsHeightModifier)
				|| (MinecraftClient.getInstance().isIntegratedServerRunning() && !hasCloudsHeightModifier)) {
			f = CONFIG.getCloudHeight();
		} else {
			f = world.getDimensionEffects().getCloudsHeight();
		}
		
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
				if ((isWeatherChange && cloudDensityByBiome != 0) || (isBiomeChange && CONFIG.getBiomeDensityMultipler() != 0) || (isBiomeChange && cloudDensityByWeather != 0)) {
					time += MinecraftClient.getInstance().getLastFrameDuration() / weatheringRefreshSpeed;
				} else {
					time += MinecraftClient.getInstance().getLastFrameDuration() / normalRefreshSpeed;		//20.0f for origin
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
					if (CONFIG.isEnableFog()) {
						BackgroundRenderer.setFogBlack();
						if (!CONFIG.isFogAutoDistance()) {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getFogMinDistance());
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getFogMaxDistance());
						} else {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * (float)Math.pow(cloudRenderDistance / 48f, 2) / 2);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * (float)Math.pow(cloudRenderDistance / 48f, 2));
						}
					} else {
						BackgroundRenderer.clearFog();
					}

					RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1);

					matrices.push();
					matrices.translate(-cameraX, -cameraY, -cameraZ);
					matrices.translate(xScroll - cloudRenderDistanceOffset, f - 15, zScroll + RUNTIME.partialOffset - cloudRenderDistanceOffset);
					cb.bind();

					for (int s = 0; s < 2; ++s) {
						if (s == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						Shader shaderProgram = RenderSystem.getShader();
						cb.draw(matrices.peek().getPositionMatrix(), projectionMatrix, shaderProgram);
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

	private void collectCloudData(double scrollX, double scrollZ) {

		try {
			double startX = scrollX / 16 - (cloudRenderDistance - 96) / 2f;
			double startZ = scrollZ / 16 - (cloudRenderDistance - 96) / 2f;

			double timeOffset = Math.floor(RUNTIME.time / 6) * 6;

			RUNTIME.checkFullOffset();
			
			float baseFreq = 0.05f;
			float baseTimeFactor = 0.01f;

			float l1Freq = 0.09f;
			float l1TimeFactor = 0.02f;

			float l2Freq = 0.001f;
			float l2TimeFactor = 0.1f;
			
			var f = 1.3 - cloudDensityByWeather * (1 - (1 - cloudDensityByBiome) * CONFIG.getBiomeDensityMultipler() / 100f * 2);
			if (CONFIG.isEnableDebug())
				SFCReMain.LOGGER.info("[SFCRe] density W: " + cloudDensityByWeather + ", B: " + cloudDensityByBiome + ", f: " + f);

			for (int cx = 0; cx < cloudRenderDistance; cx++) {
				for (int cy = 0; cy < cloudLayerThickness; cy++) {
					for (int cz = 0; cz < cloudRenderDistance; cz++) {
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

				RUNTIME.checkPartialOffset();
				cloudRenderDistanceOffset = (cloudRenderDistance - 96) / 2f * 16;
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

	private BufferBuilder.BuiltBuffer rebuildCloudMesh() {

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

		builder.clear();
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

				builder.vertex(x, y, z).texture(0.5f, 0.5f).color(ColorHelper.Argb.mixColor(colors[normIndex], cloudColorModifier)).normal(nx, ny, nz).next();
			}
		} catch (Exception e) {
			// -- Ignore...
			SFCReMain.LOGGER.error(e.toString());
		}

		return builder.end();
	}
	
	/* 
	 * @param tg - target to approach
	 * @param cr - current value
	 * @param spd - value of change speed
	 * 
	 * @comment needs to be improve.
	 */
	private static float nextDensityStep(float tg, float cr, float spd) {
		return cr + (tg - cr) / spd;
	}
	
	//Update Setting.
	public void updateRenderData(SFCReConfig config) {
		cloudRenderDistance = config.getCloudRenderDistance();
		cloudLayerThickness = config.getCloudLayerThickness();
		normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
		weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
		densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed());
		
		_cloudData = new boolean[cloudRenderDistance][cloudLayerThickness][cloudRenderDistance];
	}	
}
