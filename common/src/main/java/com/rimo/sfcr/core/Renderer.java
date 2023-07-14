package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.util.CloudDataType;
import com.rimo.sfcr.util.WeatherType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class Renderer {

	private final Runtime RUNTIME = SFCReMod.RUNTIME;
	private final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

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

	public ObjectArrayList<CloudData> cloudDataGroup = new ObjectArrayList<>();

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double time;
	private double timeRebuild;

	public double xScroll;
	public double zScroll;

	public int cullStateSkipped = 0;
	public int cullStateShown = 0;

	public void init() {
		CloudData.initSampler(SFCReMod.RUNTIME.seed);
		isProcessingData = false;
	}

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
					targetDownFall = CONFIG.getDownfall(world.getBiome(player.getBlockPos()).value().getPrecipitation(player.getBlockPos()));
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
						case THUNDER -> cloudDensityByWeather = nextDensityStep(CONFIG.getThunderDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
						case RAIN -> cloudDensityByWeather = nextDensityStep(CONFIG.getRainDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
						case CLEAR -> cloudDensityByWeather = nextDensityStep(CONFIG.getCloudDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
					}
				} else {
					switch (RUNTIME.nextWeather) {
						case THUNDER -> cloudDensityByWeather = CONFIG.getThunderDensityPercent() / 100f;
						case RAIN -> cloudDensityByWeather = CONFIG.getRainDensityPercent() / 100f;
						case CLEAR -> cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 100f;
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
				SFCReMod.LOGGER.info("wc: " + isWeatherChange + ", bc: " + isBiomeChange + ", wd: " + cloudDensityByWeather + ", bd: " + cloudDensityByBiome);
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
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.depthMask(true);

			Vec3d cloudColor = world.getCloudsColor(tickDelta);
			Vec3d cloudColor2 = new Vec3d(
					cloudColor.x + (1 - cloudColor.x) * CONFIG.getCloudBrightMultiplier(),
					cloudColor.y + (1 - cloudColor.y) * CONFIG.getCloudBrightMultiplier(),
					cloudColor.z + (1 - cloudColor.z) * CONFIG.getCloudBrightMultiplier()
			);

			synchronized (this) {

				if (!MinecraftClient.getInstance().isInSingleplayer() || !MinecraftClient.getInstance().isPaused())
					SFCReMod.RUNTIME.partialOffset += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f * CONFIG.getCloudBlockSize() / 16f;

				if ((isWeatherChange && cloudDensityByBiome != 0) || (isBiomeChange && cloudDensityByWeather != 0)) {
					time += MinecraftClient.getInstance().getLastFrameDuration() / weatheringRefreshSpeed;
				} else {
					time += MinecraftClient.getInstance().getLastFrameDuration() / normalRefreshSpeed;		//20.0f for origin
				}

				if (++timeRebuild > CONFIG.getRebuildInterval()) {
					timeRebuild = 0;
					BufferBuilder.BuiltBuffer cb = rebuildCloudMesh(Tessellator.getInstance().getBuffer());

					if (cb != null) {
						if (cloudBuffer != null)
							cloudBuffer.close();
						cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

						cloudBuffer.bind();
						cloudBuffer.upload(cb);
						VertexBuffer.unbind();
					}
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
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getAutoFogMaxDistance() / 2);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getAutoFogMaxDistance());
						}
					} else {
						BackgroundRenderer.clearFog();
					}

					matrices.push();
					matrices.translate(-cameraX, -cameraY, -cameraZ);
					matrices.translate(xScroll + 0.01f, cloudHeight - CONFIG.getCloudBlockSize() + 0.01f, zScroll + SFCReMod.RUNTIME.partialOffset);

					RenderSystem.setShaderColor((float) cloudColor2.x, (float) cloudColor2.y, (float) cloudColor2.z, 1);
					cloudBuffer.bind();

					for (int s = 0; s < 2; ++s) {
						if (s == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						cloudBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, RenderSystem.getShader());
					}

					VertexBuffer.unbind();
					matrices.pop();

					//Restore render system
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				}
			}
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
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

	private final float[][] normals = {
			{1, 0, 0},		//r
			{-1, 0, 0},		//l
			{0, 1, 0},		//u
			{0, -1, 0},		//d
			{0, 0, 1},		//f
			{0, 0, -1},		//b
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
	@Nullable
	private BufferBuilder.BuiltBuffer rebuildCloudMesh(BufferBuilder builder) {

		var client = MinecraftClient.getInstance();
		Vec3d camVec = new Vec3d(
				-Math.sin(Math.toRadians(client.gameRenderer.getCamera().getYaw()	)),
				-Math.tan(Math.toRadians(client.gameRenderer.getCamera().getPitch()	)),
				 Math.cos(Math.toRadians(client.gameRenderer.getCamera().getYaw()	))
		).normalize();
		double fovCos = Math.cos(client.options.getFov().getValue() * Math.toRadians(client.player.getFovMultiplier()) * CONFIG.getCullRadianMultiplier());

		builder.clear();
		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		cullStateShown = 0;
		cullStateSkipped = 0;

		for (CloudData data : cloudDataGroup) {
			try {
				var colorModifier = getCloudColor(client.world.getTimeOfDay(), data);
				int normCount = data.normalList.size();

				for (int i = 0; i < normCount; i++) {
					int normIndex = data.normalList.getByte(i);		// exacting data...
					var nx = normals[normIndex][0];
					var ny = normals[normIndex][1];
					var nz = normals[normIndex][2];
					float[][] verCache = new float[4][3];
					for (int j = 0; j < 4; j++) {
						verCache[j][0] = data.vertexList.getFloat(i * 12 + j * 3) * CONFIG.getCloudBlockSize();
						verCache[j][1] = data.vertexList.getFloat(i * 12 + j * 3 + 1) * CONFIG.getCloudBlockSize() / 2;
						verCache[j][2] = data.vertexList.getFloat(i * 12 + j * 3 + 2) * CONFIG.getCloudBlockSize();
					}

					// Normal Culling
					if (!CONFIG.isEnableNormalCull() || new Vec3d(nx, ny, nz).dotProduct(new Vec3d(
							verCache[0][0] + 0.01f,
							verCache[0][1] + cloudHeight + 0.01f - CONFIG.getCloudBlockSize() - client.gameRenderer.getCamera().getPos().y,
							verCache[0][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
									).normalize()) < 0.003805f) {		// clouds is moving, z-pos isn't precise, so leave some margin
						// Position Culling
						int j = -1;
						while (++j < 4) {
							if (CONFIG.getCullRadianMultiplier() > 1.5f || camVec.dotProduct(new Vec3d(
									verCache[j][0] + 0.01f,
									verCache[j][1] + cloudHeight + 0.01f - CONFIG.getCloudBlockSize() - client.gameRenderer.getCamera().getPos().y,
									verCache[j][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
											).normalize()) > fovCos) {
								for (int k = 0; k < 4; k++)
									builder.vertex(verCache[k][0], verCache[k][1], verCache[k][2]).texture(0.5f, 0.5f).color(ColorHelper.Argb.mixColor(colors[normIndex], colorModifier)).normal(nx, ny, nz).next();
								break;
							}
						}

						if (j < 4) {
							cullStateShown++;
						} else {
							cullStateSkipped++;
						}
					} else {
						cullStateSkipped++;
					}
				}
			} catch (Exception e) {
				SFCReMod.exceptionCatcher(e);
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
			SFCReMod.exceptionCatcher(e);
			return null;
		}
	}

	private void collectCloudData(double scrollX, double scrollZ) {

		CloudData tmp;
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
			timeRebuild = CONFIG.getRebuildInterval();		//Instant refresh once to prevent flicker.
		} catch (Exception e) {		// Debug
			SFCReMod.exceptionCatcher(e);
		}

		isProcessingData = false;
	}

	private int getCloudColor(long worldTime, CloudData data) {
		var a = 255;
		var r = (CONFIG.getCloudColor() & 0xFF0000) >> 16;
		var g = (CONFIG.getCloudColor() & 0x00FF00) >> 8;
		var b = (CONFIG.getCloudColor() & 0x0000FF);
		int t = (int) (worldTime % 24000);

		// Alpha changed by cloud type and lifetime
		switch (data.getDataType()) {
			case NORMAL, TRANS_MID_BODY: break;
			case TRANS_IN: a = (int) (a - a * data.getLifeTime() / CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed()) * 5f); break;
			case TRANS_OUT: a = (int) (a * data.getLifeTime() / CONFIG.getNumFromSpeedEnum(CONFIG.getNormalRefreshSpeed()) * 5f); break;
		}

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, scale value in [0, 2000]
			t = t > 22500 ? t - 22000 : t + 1500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		}

		return ColorHelper.Argb.getArgb(a, r, g, b);
	}

	private float nextDensityStep(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ? (target > current ? current + 1f / speed : current - 1f / speed) : target;
	}

	//Update Setting.
	public void updateConfig(CommonConfig config) {
		normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
		weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
		densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed());
	}
}
