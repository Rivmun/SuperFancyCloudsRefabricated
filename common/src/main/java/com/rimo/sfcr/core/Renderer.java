package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.util.CloudDataType;
import com.rimo.sfcr.util.WeatherType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Renderer {

	private final Runtime RUNTIME = SFCReMod.RUNTIME;
	private CommonConfig config = SFCReMod.COMMON_CONFIG;

	private float cloudDensityByWeather = 0f;
	private float cloudDensityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	public float cloudHeight;

	private int normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
	private int weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
	private int densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed());

	private final Identifier whiteTexture = new Identifier("sfcr", "white.png");

	public VertexBuffer cloudBuffer;

	public ObjectArrayList<CloudData> cloudDataGroup = new ObjectArrayList<>();

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double time;

	public double xScroll;
	public double zScroll;

	public int cullStateSkipped = 0;
	public int cullStateShown = 0;

	public void init() {
		CloudData.initSampler(SFCReMod.RUNTIME.seed);
		isProcessingData = false;
	}

	@SuppressWarnings("resource")
	public void tick() {

		if (MinecraftClient.getInstance().player == null)
			return;

		if (!config.isEnableMod())
			return;

		if (MinecraftClient.getInstance().isInSingleplayer() && MinecraftClient.getInstance().isPaused())
			return;

		if (!MinecraftClient.getInstance().world.getDimension().hasSkyLight())
			return;

		//If already processing, don't start up again.
		if (isProcessingData)
			return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		ClientWorld world = MinecraftClient.getInstance().world;
		int xScroll = (int) (player.getX() / config.getCloudBlockSize()) * config.getCloudBlockSize();
		int zScroll = (int) (player.getZ() / config.getCloudBlockSize()) * config.getCloudBlockSize();

		int timeOffset = (int) (Math.floor(time / 6) * 6);

		RUNTIME.clientTick(world);

		//Detect Weather Change
		if (config.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.THUNDER && config.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather < config.getThunderDensityPercent() / 100f;
			} else if (world.isRaining()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.RAIN && config.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather != config.getRainDensityPercent() / 100f;
			} else {		//Clear...
				isWeatherChange = RUNTIME.nextWeather != WeatherType.CLEAR && config.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather > config.getCloudDensityPercent() / 100f;
			}

			//Detect Biome Change
			if (!config.isBiomeDensityByChunk()) {		//Hasn't effect if use chunk data.
				if (!config.isFilterListHasBiome(world.getBiome(player.getBlockPos()).getCategory()))
					targetDownFall = world.getBiome(player.getBlockPos()).getDownfall();
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
			if (config.isEnableWeatherDensity()) {
				if (isWeatherChange) {
					switch (RUNTIME.nextWeather) {
						case THUNDER: cloudDensityByWeather = nextDensityStep(config.getThunderDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
						case RAIN: cloudDensityByWeather = nextDensityStep(config.getRainDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
						case CLEAR: cloudDensityByWeather = nextDensityStep(config.getCloudDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed); break;
					}
				} else {
					switch (RUNTIME.nextWeather) {
						case THUNDER: cloudDensityByWeather = config.getThunderDensityPercent() / 100f; break;
						case RAIN: cloudDensityByWeather = config.getRainDensityPercent() / 100f; break;
						case CLEAR: cloudDensityByWeather = config.getCloudDensityPercent() / 100f; break;
					}
				}
				//Density Change by Biome
				if (!config.isBiomeDensityByChunk()) {
					cloudDensityByBiome = isBiomeChange ? nextDensityStep(targetDownFall, cloudDensityByBiome, densityChangingSpeed) : targetDownFall;
				} else {
					cloudDensityByBiome = 0.5f;		//Output common value if use chunk.
				}
			} else {		//Initialize if disabled detect in rain/thunder.
				cloudDensityByWeather = config.getCloudDensityPercent() / 100f;
				cloudDensityByBiome = 0f;
			}

			dataProcessThread = new Thread(() -> collectCloudData(xScroll, zScroll));
			dataProcessThread.start();

			if (config.isEnableDebug()) {
				SFCReMod.LOGGER.info("wc: " + isWeatherChange + ", bc: " + isBiomeChange + ", wd: " + cloudDensityByWeather + ", bd: " + cloudDensityByBiome);
			}
		}
	}

	public void render(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ) {

		cloudHeight = config.getCloudHeight() < 0 ? MinecraftClient.getInstance().world.getSkyProperties().getCloudsHeight() : config.getCloudHeight();

		if (!Float.isNaN(cloudHeight)) {
			//Setup render system
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableDepthTest();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.depthMask(true);

			Vec3d cloudColor = MinecraftClient.getInstance().world.getCloudsColor(tickDelta);

			synchronized (this) {

				if (!MinecraftClient.getInstance().isInSingleplayer() || !MinecraftClient.getInstance().isPaused())
					SFCReMod.RUNTIME.partialOffset += MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f * config.getCloudBlockSize() / 16f;

				if ((isWeatherChange && cloudDensityByBiome != 0) || (isBiomeChange && config.getBiomeDensityMultiplier() != 0) || (isBiomeChange && cloudDensityByWeather != 0)) {
					time += MinecraftClient.getInstance().getLastFrameDuration() / weatheringRefreshSpeed;
				} else {
					time += MinecraftClient.getInstance().getLastFrameDuration() / normalRefreshSpeed;		//20.0f for origin
				}

				BufferBuilder cb = rebuildCloudMesh(cloudColor);

				if (cb != null) {
					if (cloudBuffer != null)
						cloudBuffer.close();
					cloudBuffer = new VertexBuffer(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
					cloudBuffer.upload(cb);
				}

				//Setup shader
				if (cloudBuffer != null) {
					MinecraftClient.getInstance().getTextureManager().bindTexture(whiteTexture);
					if (config.isEnableFog()) {
						RenderSystem.enableFog();
						if (!config.isFogAutoDistance()) {
							RenderSystem.fogStart(MinecraftClient.getInstance().gameRenderer.getViewDistance() * config.getFogMinDistance() * config.getCloudBlockSize() / 16);
							RenderSystem.fogEnd(MinecraftClient.getInstance().gameRenderer.getViewDistance() * config.getFogMaxDistance() * config.getCloudBlockSize() / 16);
						} else {
							RenderSystem.fogStart(MinecraftClient.getInstance().gameRenderer.getViewDistance() * config.getAutoFogMaxDistance() / 2);
							RenderSystem.fogEnd(MinecraftClient.getInstance().gameRenderer.getViewDistance() * config.getAutoFogMaxDistance());
						}
					} else {
						RenderSystem.disableFog();
					}

					matrices.push();
					matrices.translate(-cameraX, -cameraY, -cameraZ);
					matrices.translate(xScroll + 0.01f, cloudHeight - config.getCloudBlockSize() + 0.01f, zScroll + SFCReMod.RUNTIME.partialOffset);

					cloudBuffer.bind();
					VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.startDrawing(0);

					for (int s = 0; s < 2; ++s) {
						if (s == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						cloudBuffer.draw(matrices.peek().getModel(),7);
					}

					VertexBuffer.unbind();
					VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.endDrawing();
					matrices.pop();

					//Restore render system
					RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.disableAlphaTest();
					RenderSystem.enableCull();
					RenderSystem.disableBlend();
					RenderSystem.disableFog();
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
			{1, 0, 0},		//r
			{-1, 0, 0},		//l
			{0, 1, 0},		//u
			{0, -1, 0},		//d
			{0, 0, 1},		//f
			{0, 0, -1},		//b
	};

	private final float[][] colors = {
			{0.8f, 0.95f, 0.9f, 0.9f},
			{0.8f, 0.75f, 0.75f, 0.75f},
			{0.8f, 1, 1, 1},
			{0.8f, 0.6f, 0.6f, 0.6f},
			{0.8f, 0.92f, 0.85f, 0.85f},
			{0.8f, 0.8f, 0.8f, 0.8f},
	};

	// Building mesh
	private BufferBuilder rebuildCloudMesh(Vec3d cloudColor) {

		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d camVec = new Vec3d(
				-Math.sin(client.gameRenderer.getCamera().getYaw()   / 180f * Math.PI),
				-Math.tan(client.gameRenderer.getCamera().getPitch() / 180f * Math.PI),
				 Math.cos(client.gameRenderer.getCamera().getYaw()   / 180f * Math.PI)
		).normalize();
		double fovCos = Math.cos(client.options.fov / 180f * Math.PI * config.getCullRadianMultiplier());

		builder.clear();
		builder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

		for (CloudData data : cloudDataGroup) {
			try {
				float[] colorModifier = getCloudColor(client.world.getTimeOfDay(), data);
				int normCount = data.normalList.size();

				for (int i = 0; i < normCount; i++) {
					int normIndex = data.normalList.getByte(i);		// exacting data...
					float nx = normals[normIndex][0];
					float ny = normals[normIndex][1];
					float nz = normals[normIndex][2];
					float[][] verCache = new float[4][3];
					for (int j = 0; j < 4; j++) {
						verCache[j][0] = data.vertexList.getFloat(i * 12 + j * 3) * config.getCloudBlockSize();
						verCache[j][1] = data.vertexList.getFloat(i * 12 + j * 3 + 1) * config.getCloudBlockSize() / 2;
						verCache[j][2] = data.vertexList.getFloat(i * 12 + j * 3 + 2) * config.getCloudBlockSize();
					}

					// Normal Culling
					if (!config.isEnableNormalCull() || new Vec3d(nx, ny, nz).dotProduct(new Vec3d(
							verCache[0][0] + 0.01f,
							verCache[0][1] + cloudHeight + 0.01f - config.getCloudBlockSize() - client.gameRenderer.getCamera().getPos().y,
							verCache[0][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
									).normalize()) < 0.034074f) {		// clouds is moving, z-pos isn't precise, so leave some margin
						// Position Culling
						int j = -1;
						while (++j < 4) {
							if (camVec.dotProduct(new Vec3d(
									verCache[j][0] + 0.01f,
									verCache[j][1] + cloudHeight + 0.01f - config.getCloudBlockSize() - client.gameRenderer.getCamera().getPos().y,
									verCache[j][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
											).normalize()) > fovCos) {
								for (int k = 0; k < 4; k++)
									builder.vertex(verCache[k][0], verCache[k][1], verCache[k][2]).texture(0.5f, 0.5f).color(
											(float) cloudColor.x * colors[normIndex][1] * colorModifier[1],
											(float) cloudColor.y * colors[normIndex][2] * colorModifier[2],
											(float) cloudColor.z * colors[normIndex][3] * colorModifier[3],
											colors[normIndex][0] * colorModifier[0]
									).normal(nx, ny, nz).next();
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
			builder.end();
			return builder;
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
			if (!cloudDataGroup.isEmpty() && config.isEnableSmoothChange()) {
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
			SFCReMod.exceptionCatcher(e);
		}

		isProcessingData = false;
	}
	
	private float[] getCloudColor(long worldTime, CloudData data) {
		float[] m = {1, 1, 1, 1};
		int t = (int) (worldTime % 24000);

		// Alpha changed by cloud type and lifetime
		switch (data.getDataType()) {
			case TRANS_IN: m[0] = 1 - data.getLifeTime() / config.getNumFromSpeedEnum(config.getNormalRefreshSpeed()) * 5f; break;
			case TRANS_OUT: m[0] = (int) (255 * data.getLifeTime() / config.getNumFromSpeedEnum(config.getNormalRefreshSpeed()) * 5f); break;
			default: break;
		}

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, scale value in [0, 2000]
			t = t > 22500 ? t - 22000 : t + 1500; 
			m[1] = (float) (1 - Math.sin(t / 2000d * Math.PI) / 8);
			m[2] = (float) (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1); 
			m[3] = (float) (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6);
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			m[1] = (float) (1 - Math.sin(t / 2000d * Math.PI) / 8);
			m[2] = (float) (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1); 
			m[3] = (float) (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6);
		}

		return m;
	}

	private float nextDensityStep(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ? (target > current ? current + 1f / speed : current - 1f / speed) : target;
	}

	//Update Setting.
	public void updateConfig(CommonConfig config) {
		this.config = config;
		normalRefreshSpeed = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed());
		weatheringRefreshSpeed = config.getNumFromSpeedEnum(config.getWeatherRefreshSpeed()) / 2;
		densityChangingSpeed = config.getNumFromSpeedEnum(config.getDensityChangingSpeed());
	}	
}
