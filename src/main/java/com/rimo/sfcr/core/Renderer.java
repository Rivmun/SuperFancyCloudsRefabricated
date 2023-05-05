package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.util.CloudDataType;
import com.rimo.sfcr.util.WeatherType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class Renderer {

	private static final RuntimeData RUNTIME = SFCReMain.RUNTIME;
	private static final SFCReConfig CONFIG = SFCReMain.CONFIGHOLDER.getConfig();

	private float cloudDensityByWeather = 0f;
	private float cloudDensityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	public float cloudHeight;

	private int normalRefreshSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed());
	private int weatheringRefreshSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getWeatherRefreshSpeed()) / 2;
	private int densityChangingSpeed = CONFIG.getNumFromSpeedEnum(CONFIG.getDensityChangingSpeed());

	private final Identifier whiteTexture = new Identifier("sfcr", "white.png");

	public VertexBuffer cloudBuffer;

	public ObjectArrayList<CloudData> cloudDataGroup = new ObjectArrayList<CloudData>();

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double partialOffsetSecondary = 0;

	public double time;

	public double xScroll;
	public double zScroll;

	public BufferBuilder.BuiltBuffer cb;

	public void init() {
		CloudData.initSampler(SFCReMain.RUNTIME.seed);
		isProcessingData = false;
	}

	@SuppressWarnings("resource")
	public void tick() {

		if (MinecraftClient.getInstance().player == null)
			return;

		if (!CONFIG.isEnableMod())
			return;

		if (MinecraftClient.getInstance().isInSingleplayer() && MinecraftClient.getInstance().isPaused())
			return;

		if (!MinecraftClient.getInstance().world.getDimension().hasSkyLight())
			return;

		//If already processing, don't start up again.
		if (isProcessingData)
			return;

		var player = MinecraftClient.getInstance().player;
		var world = MinecraftClient.getInstance().world;
		var xScroll = (int) (player.getX() / CONFIG.getCloudBlockSize()) * CONFIG.getCloudBlockSize();
		var zScroll = (int) (player.getZ() / CONFIG.getCloudBlockSize()) * CONFIG.getCloudBlockSize();

		int timeOffset = (int) (Math.floor(time / 6) * 6);

		RUNTIME.clientTick(world);

		//Detect Weather Change
		if (CONFIG.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.THUNDER && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather < CONFIG.getThunderDensityPercent() / 100f;
			} else if (world.isRaining()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.RAIN && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather != CONFIG.getRainDensityPercent() / 100f;
			} else {		//Clear...
				isWeatherChange = RUNTIME.nextWeather != WeatherType.CLEAR && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather > CONFIG.getCloudDensityPercent() / 100f;
			}

			//Detect Biome Change
			if (!CONFIG.isBiomeDensityByChunk()) {		//Hasn't effect if use chunk data.
				if (!CONFIG.isFilterListHasBiome(world.getBiome(player.getBlockPos())))
					targetDownFall = world.getBiome(player.getBlockPos()).value().getDownfall();
				isBiomeChange = cloudDensityByBiome != targetDownFall;
			}
		} else {
			isWeatherChange = false;
			isBiomeChange = false;
		}


		//Refresh Processing...
		if (timeOffset != moveTimer || xScroll != this.xScroll || zScroll != this.zScroll) {
			moveTimer = timeOffset;
			isProcessingData = true;

			//Density Change by Weather
			if (CONFIG.isEnableWeatherDensity()) {
				if (isWeatherChange) {
					switch (RUNTIME.nextWeather) {
						case THUNDER: cloudDensityByWeather = nextDensityStep(CONFIG.getThunderDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
						case RAIN: cloudDensityByWeather = nextDensityStep(CONFIG.getRainDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
						case CLEAR: cloudDensityByWeather = nextDensityStep(CONFIG.getCloudDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
					}
				} else {
					switch (RUNTIME.nextWeather) {
						case THUNDER: cloudDensityByWeather = CONFIG.getThunderDensityPercent() / 100f; break;
						case RAIN: cloudDensityByWeather = CONFIG.getRainDensityPercent() / 100f; break;
						case CLEAR: cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 100f; break;
					}
				}
				//Density Change by Biome
				if (!CONFIG.isBiomeDensityByChunk()) {
					cloudDensityByBiome = isBiomeChange ? nextDensityStep(targetDownFall, cloudDensityByBiome, densityChangingSpeed) : targetDownFall;
				} else {
					cloudDensityByBiome = 0.5f;		//Output common value if use chunk.
				}
			} else {		//Initialize if disabled detect in rain/thunder.
				cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 100f;
				cloudDensityByBiome = 0f;
			}

			dataProcessThread = new Thread(() -> collectCloudData(xScroll, zScroll));
			dataProcessThread.start();

			if (CONFIG.isEnableDebug()) {
				SFCReMain.LOGGER.info("wc: " + isWeatherChange + ", bc: " + isBiomeChange + ", wd: " + cloudDensityByWeather + ", bd: " + cloudDensityByBiome);
			}
		}
	}

	public void render(ClientWorld world, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) {

		cloudHeight = CONFIG.getCloudHeight() < 0 ? world.getDimensionEffects().getCloudsHeight() : CONFIG.getCloudHeight();

		if (!Float.isNaN(cloudHeight)) {
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

				if (!MinecraftClient.getInstance().isInSingleplayer() || !MinecraftClient.getInstance().isPaused())
					SFCReMain.RUNTIME.partialOffset += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;

				if ((isWeatherChange && cloudDensityByBiome != 0) || (isBiomeChange && CONFIG.getBiomeDensityMultiplier() != 0) || (isBiomeChange && cloudDensityByWeather != 0)) {
					time += MinecraftClient.getInstance().getLastFrameDuration() / weatheringRefreshSpeed;
				} else {
					time += MinecraftClient.getInstance().getLastFrameDuration() / normalRefreshSpeed;		//20.0f for origin
				}

				var cb = rebuildCloudMesh();

				if (cb != null) {
					if (cloudBuffer != null)
						cloudBuffer.close();
					cloudBuffer = new VertexBuffer();
					cloudBuffer.bind();
					cloudBuffer.upload(cb);
					VertexBuffer.unbind();
				}

				//Setup shader
				if (cloudBuffer != null) {
					RenderSystem.setShader(GameRenderer::getPositionTexColorNormalProgram);
					RenderSystem.setShaderTexture(0, whiteTexture);
					if (CONFIG.isEnableFog()) {
						BackgroundRenderer.setFogBlack();
						if (!CONFIG.isFogAutoDistance()) {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getFogMinDistance() * CONFIG.getCloudBlockSize() / 16);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getFogMaxDistance() * CONFIG.getCloudBlockSize() / 16);
						} else {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * (float) Math.pow(CONFIG.getCloudRenderDistance() / 48, 2) * CONFIG.getCloudBlockSize() / 16 / 2);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * (float) Math.pow(CONFIG.getCloudRenderDistance() / 48, 2) * CONFIG.getCloudBlockSize() / 16);
						}
					} else {
						BackgroundRenderer.clearFog();
					}

					RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1);

					matrices.push();
					matrices.translate(-cameraX, -cameraY, -cameraZ);
					matrices.translate(xScroll + 0.01f, cloudHeight - CONFIG.getCloudBlockSize() + 0.01f, zScroll + SFCReMain.RUNTIME.partialOffset);
					cloudBuffer.bind();

					for (int s = 0; s < 2; ++s) {
						if (s == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						ShaderProgram shaderProgram = RenderSystem.getShader();
						cloudBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, shaderProgram);
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

	public BufferBuilder builder = Tessellator.getInstance().getBuffer();

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

	// Building mesh
	@SuppressWarnings("resource")
	private BufferBuilder.BuiltBuffer rebuildCloudMesh() {
		builder.clear();
		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		
		for (CloudData data : cloudDataGroup) {
			try {
				int vertCount = data.vertexList.size() / 3;
				var colorModifier = getCloudColor(MinecraftClient.getInstance().world.getTimeOfDay(), data);

				for (int i = 0; i < vertCount; i++) {
					int origin = i * 3;
					var x = data.vertexList.getFloat(origin) * CONFIG.getCloudBlockSize();
					var y = data.vertexList.getFloat(origin + 1) * CONFIG.getCloudBlockSize() / 2;
					var z = data.vertexList.getFloat(origin + 2) * CONFIG.getCloudBlockSize();

					int normIndex = data.normalList.getByte(i / 4);
					var norm = normals[normIndex];
					var nx = norm[0];
					var ny = norm[1];
					var nz = norm[2];

					builder.vertex(x, y, z).texture(0.5f, 0.5f).color(ColorHelper.Argb.mixColor(colors[normIndex], colorModifier)).normal(nx, ny, nz).next();
				}
			} catch (Exception e) {
				SFCReMain.exceptionCatcher(e);
			}

			if (data.getDataType().equals(CloudDataType.NORMAL)) {
				break;
			} else if (data.getDataType().equals(CloudDataType.TRANS_MID_BODY)) {
				data.tick();
				if (data.getLifeTime() <= 0) {		// Clear if lifetime reach end
					while (!cloudDataGroup.get(0).getDataType().equals(CloudDataType.NORMAL)) {
						cloudDataGroup.remove(0);
					}
				}
				break;		// Only render IN, BODY, OUT till its life end and remove.
			} else {
				data.tick();
			}
		}

		try {
			return builder.end();
		} catch (Exception e) {
			return null;
		}
	}

	private void collectCloudData(double scrollX, double scrollZ) {

		CloudData tmp = null;
		CloudFadeData fadeIn = null, fadeOut = null;
		CloudMidData midBody = null;

		try {
			RUNTIME.checkFullOffset();

			tmp = new CloudData(scrollX, scrollZ, cloudDensityByWeather, cloudDensityByBiome);
			if (!cloudDataGroup.isEmpty() && CONFIG.isEnableSmoothChange()) {
				fadeIn = new CloudFadeData(cloudDataGroup.get(0), tmp, CloudDataType.TRANS_IN);
				fadeOut = new CloudFadeData(tmp, cloudDataGroup.get(0), CloudDataType.TRANS_OUT);
				midBody = new CloudMidData(cloudDataGroup.get(0), tmp, CloudDataType.TRANS_MID_BODY);
			}

			synchronized (this) {
				cloudDataGroup.clear();

				if (midBody != null) {
					cloudDataGroup.add(fadeIn);
					cloudDataGroup.add(fadeOut);
					cloudDataGroup.add(midBody);
				}
				cloudDataGroup.add(tmp);

				this.xScroll = scrollX;
				this.zScroll = scrollZ;
			}
			RUNTIME.checkPartialOffset();
		} catch (Exception e) {		// Debug
			SFCReMain.exceptionCatcher(e);
		}

		isProcessingData = false;
	}
	
	private int getCloudColor(long worldTime, CloudData data) {
		int a = 255, r = 255, g = 255, b = 255;
		int t = (int) (worldTime % 24000);

		// Alpha changed by cloud type and lifetime
		switch (data.getDataType()) {
			case NORMAL, TRANS_MID_BODY: break;
			case TRANS_IN: a = (int) (255 - 255 * data.getLifeTime() / CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed()) * 5f); break;
			case TRANS_OUT: a = (int) (255 * data.getLifeTime() / CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed()) * 5f); break;
		}

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, scale value in [0, 2000]
			t = t > 22500 ? t - 22000 : t + 1500; 
			r = (int) (255 * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1)); 
			b = (int) (255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			r = (int)(255 * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1)); 
			b = (int) (255 * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		}

		return ColorHelper.Argb.getArgb(a, r, g, b);
	}

	private float nextDensityStep(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ? (target > current ? current + 1f / speed : current - 1f / speed) : target;
	}

	//Update Setting.
	public void updateRenderData(SFCReConfig config) {
		normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
		weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
		densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed());
	}	
}
