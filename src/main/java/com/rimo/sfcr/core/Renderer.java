package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.rimo.sfcr.Common;
import com.rimo.sfcr.VersionUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
//? if < 1.20 {
/*import com.mojang.math.Matrix4f;
*///? } else {
import org.joml.Matrix4f;
//? }
//? if < 1.21.1 {
/*import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
*///? } else {
import net.minecraft.client.renderer.*;
import static net.minecraft.client.renderer.RenderStateShard.*;
//? }

import java.util.ArrayList;

import static com.rimo.sfcr.Common.*;

public class Renderer {
	private static final ResourceLocation whiteTexture = VersionUtil.getId("white.png");
	//? if = 1.21.1 {
	private static final RenderType SFCR = createCustomCloudRenderType(false);
	private static final RenderType SFCR_DEPTH_ONLY = createCustomCloudRenderType(true);
	//? }
	private final ArrayList<CloudData> cloudDataGroup = new ArrayList<>();
	private VertexBuffer cloudsBuffer;
	protected boolean isResampling = false;
	protected Thread resamplingThread;
	protected float cloudHeight;
	protected int oldGridX, oldGridZ;
	private Vec3 oldColor = Vec3.ZERO;
	protected double xOffset, zOffset;
	protected double resamplingTimer = 0.0;  //manual update counter
	private int rebuildTimer = 0;  //measure in ticks
	protected int cullStateSkipped, cullStateShown;  //debug counter
	protected double debugRebuildTime, debugUploadTime;

	public Renderer() {}
	public Renderer(Renderer renderer) {
		renderer.stop();
	}

	/*
	 * since 1.21.1: vanilla RenderType is blinding with vanilla texture that let our uv always pointing to an empty pixel.
	 * it's causing our clouds cannot render. we must modify it to our whiteTexture.
	 */
	//? if = 1.21.1 {
	private static RenderType createCustomCloudRenderType(boolean bl) {
		return RenderType.create(
				"clouds",
				DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
				VertexFormat.Mode.QUADS,
				786432,
				false,
				false,
				RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_CLOUDS_SHADER)
						.setTextureState(new RenderStateShard.TextureStateShard(whiteTexture, false, false))  //modify it
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(bl ? DEPTH_WRITE : COLOR_DEPTH_WRITE)
						.setOutputState(CLOUDS_TARGET)
						.createCompositeState(true)
		);
	}
	//? }

	//Rewrite of vanilla renderClouds invoke by mixin
	//? if = 1.16.5 {
	/*public void render(PoseStack poseStack, float tickDelta, double cameraX, double cameraY, double cameraZ,
	*///? } else if < 1.21.1 {
	/*public void render(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ,
	*///? } else {
	public void render(PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ,
	//? }
	                   ClientLevel level) {
		float cloudHeight = level.effects().getCloudHeight();
		if (Float.isNaN(cloudHeight))
			return;
		int configHeight = CONFIG.getCloudHeight();
		if (configHeight >= 0)
			cloudHeight = configHeight;
		this.cloudHeight = cloudHeight;
		boolean isPause = Minecraft.getInstance().isPaused();

		//vanilla cloud pos calculation
		final float CLOUD_BLOCK_WIDTH = CONFIG.getCloudBlockSize();  //cloud size
		final float CLOUD_BLOCK_HEIGHT = CLOUD_BLOCK_WIDTH / 2F;
		double timeOffset = (level.getGameTime() + tickDelta) * 0.03F;
		double cloudX = (cameraX + timeOffset) / CLOUD_BLOCK_WIDTH;  //grid pos where to draw cloud layer
		double cloudY = cloudHeight - (float) cameraY + 0.33F;
		double cloudZ = cameraZ / CLOUD_BLOCK_WIDTH + 0.33F;
		int GridX = (int) Math.floor(cloudX);  //cloud grid pos !!NOTICE that timeOffset is already contained.
		//int GridY = (int) Math.floor(cloudY / CLOUD_BLOCK_HEIGHT);
		int GridZ = (int) Math.floor(cloudZ);
		float xOffsetInGrid = (float) (cloudX - Math.floor(cloudX));  //cloud offset in current grid
		//float yOffsetInGrid = (float) ((cloudY / CLOUD_BLOCK_HEIGHT - Math.floor(cloudY / CLOUD_BLOCK_HEIGHT)) * CLOUD_BLOCK_HEIGHT);
		float zOffsetInGrid = (float) (cloudZ - Math.floor(cloudZ));
		Vec3 cloudColor = level.getCloudColor(tickDelta);

		cloudColor = getBrightMultiplier(cloudColor);
		synchronized (this) {
			xOffsetInGrid += GridX - oldGridX;
			zOffsetInGrid += GridZ - oldGridZ;
		}
		this.xOffset = (xOffsetInGrid - 0.33F) * CLOUD_BLOCK_WIDTH;
		this.zOffset = (zOffsetInGrid - 0.33F) * CLOUD_BLOCK_WIDTH;
		int cameraGridY = (int) (cameraY / CLOUD_BLOCK_HEIGHT);

		//refresh check
		resamplingTimer += VersionUtil.getLastFrameDuration() * 0.25 * 0.25;
		if (! isPause && ! isResampling) {
			if (resamplingTimer > DATA.getResamplingInterval() || oldGridX != GridX || oldGridZ != GridZ || oldColor.distanceToSqr(cloudColor) > 2.0E-4) {
				isResampling = true;
				resamplingTimer = 0.0;
				oldColor = cloudColor;
				resamplingThread = new Thread(() -> {  //start data refresh thread
					try {
						collectCloudData(GridX, cameraGridY, GridZ);
					} catch (Exception e) {
						exceptionCatcher(e);
					} finally {
						synchronized (this) {
							oldGridX = GridX;  //delayed update to prevent flicker
							oldGridZ = GridZ;
						}
						markForRebuild();  //let cloudCell rebuilt instantly
						isResampling = false;
					}
				});
				resamplingThread.start();
			}
			/*
			 * if only Y changed (condition in cloudData) and not in resampling, and in cloudLayer, just refresh mesh.
			 * Normal culling already does in 1.21.6+ vanilla mesh building, we must remesh it to prevent top/bottom face disappear when Y changed.
			 */
			int cloudGridHeight = (int) (cloudHeight / CLOUD_BLOCK_HEIGHT);
			if (! isResampling && cameraGridY > cloudGridHeight && cameraGridY < cloudGridHeight + CONFIG.getCloudLayerThickness() + 1) {
 				cloudDataGroup.forEach(cloudData -> cloudData.tryRebuildMesh(cameraGridY));
			}
		}

		//Setup render system
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		//? if = 1.16.5
		//RenderSystem.enableAlphaTest();
		RenderSystem.enableDepthTest();
		//? if = 1.16.5
		//RenderSystem.defaultAlphaFunc();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		//? if ! 1.16.5
		RenderSystem.depthMask(true);

		//cloud mesh rebuilt
		/* NOTE:
		 * When cloudData is too HUGE, .upload() will get big lag, so we made a soft culling in rebuild to decrease upload size.
		 * But precisely because of culling, mesh rebuild must run in every tick instead of concurrent, to prevent bad visual.
		 * We made a lot of small lags of culling equation to replace a single big lag of upload, it's hard to say which is better...
		 */
		boolean enableCulling = CONFIG.getEnableViewCulling();
		//if culling is disabled, no need to rebuild in every tick.
		if (! isPause && (! enableCulling && rebuildTimer == 99 || enableCulling && ++ rebuildTimer > CONFIG.getRebuildInterval())) {
			rebuildTimer = 0;
			debugRebuildTime = System.nanoTime();
			//? if > 1.21 {
			MeshData cb = rebuildCloudMesh(Tesselator.getInstance(), cloudColor, xOffsetInGrid, cloudHeight);
			//? } else if > 1.19 {
			/*BufferBuilder.RenderedBuffer cb = rebuildCloudMesh(Tesselator.getInstance().getBuilder(), cloudColor, xOffsetInGrid, cloudHeight);
			*///? } else {
			/*BufferBuilder cb = rebuildCloudMesh(Tesselator.getInstance().getBuilder(), cloudColor, xOffsetInGrid, cloudHeight);
			*///? }
			debugRebuildTime = (System.nanoTime() - debugRebuildTime) / 1000000;
			if (cb != null) {
				if (cloudsBuffer != null)
					cloudsBuffer.close();
				//? if > 1.20 {
				cloudsBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
				//? } else if > 1.18 {
				/*cloudsBuffer = new VertexBuffer();
				*///? } else {
				/*cloudsBuffer = new VertexBuffer(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
				*///? }
				cloudsBuffer.bind();
				debugUploadTime = System.nanoTime();
				cloudsBuffer.upload(cb);
				debugUploadTime = (System.nanoTime() - debugUploadTime) / 1000000;
				VertexBuffer.unbind();
			}
		}

		//Setup shader
		//? if = 1.16.5 {
		/*Minecraft.getInstance().getTextureManager().bind(whiteTexture);
		*///? } else if < 1.21.1 {
		/*RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		RenderSystem.setShaderTexture(0, whiteTexture);
		*///? }
		if (CONFIG.isEnableFog()) {
			FogRenderer.levelFogColor();
			//? if ! 1.16.5 {
			if (!CONFIG.isFogAutoDistance()) {
				RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getFogMinDistance() * CONFIG.getCloudBlockSize() / 16);
				RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getFogMaxDistance() * CONFIG.getCloudBlockSize() / 16);
			} else {
				RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getAutoFogMaxDistance() / 4);
				RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getAutoFogMaxDistance());
			}
			//? } else {
			/*float viewDistance = Minecraft.getInstance().gameRenderer.getRenderDistance();
			if (!CONFIG.isFogAutoDistance()) {
				RenderSystem.fogStart(viewDistance * CONFIG.getFogMinDistance() * CONFIG.getCloudBlockSize() / 16);
				RenderSystem.fogEnd(viewDistance * CONFIG.getFogMaxDistance() * CONFIG.getCloudBlockSize() / 16);
			} else {
				RenderSystem.fogStart(viewDistance * CONFIG.getAutoFogMaxDistance() / 2);
				RenderSystem.fogEnd(viewDistance * CONFIG.getAutoFogMaxDistance());
			}
			*///? }
		} else {
			FogRenderer.setupNoFog();
		}
		//? if = 1.16.5
		//RenderSystem.depthMask(true);
		poseStack.pushPose();
		//? if = 1.21.1
		poseStack.mulPose(projectionMatrix);
		poseStack.scale(CLOUD_BLOCK_WIDTH, CLOUD_BLOCK_HEIGHT, CLOUD_BLOCK_WIDTH);
		poseStack.translate(-xOffsetInGrid, cloudY / CLOUD_BLOCK_HEIGHT, -zOffsetInGrid);  //strange that if I use yOffsetInGrid here, cloudLayer height is unstable...
		//~ if = 1.16.5 '.setShaderColor' -> '.color4f'
		RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1);

		if (cloudsBuffer != null) {
			cloudsBuffer.bind();
			//? if = 1.16.5
			//DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.setupBufferState(0L);
			for (int s = 0; s < 2; ++s) {
				//? if < 1.21.1 {
				/*if (s == 0) {
					RenderSystem.colorMask(false, false, false, false);
				} else {
					RenderSystem.colorMask(true, true, true, true);
				}
				//? if ! 1.16.5 {
				cloudsBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
				//? } else {
				/^cloudsBuffer.draw(poseStack.last().pose(), 7);
				^///? }
				*///? } else {
				RenderType renderType = s == 0 ? SFCR_DEPTH_ONLY : SFCR;
				renderType.setupRenderState();
				ShaderInstance shaderInstance = RenderSystem.getShader();
				cloudsBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderInstance);
				renderType.clearRenderState();
				//? }
			}
			VertexBuffer.unbind();
			//? if = 1.16.5
			//DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.clearBufferState();
		}

		//Restore render system
		//~ if = 1.16.5 '.setShaderColor' -> '.color4f'
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
		//? if = 1.16.5
		//RenderSystem.disableAlphaTest();
		//? if < 1.21.1 {
		/*RenderSystem.enableCull();
		RenderSystem.disableBlend();
		//? if ! 1.16.5
		RenderSystem.defaultBlendFunc();
		*///? }
		//? if = 1.16.5
		//RenderSystem.disableFog();
	}

	public void markForRebuild() {
		this.rebuildTimer = 99;
	}

	public void stop() {
		cloudDataGroup.forEach(CloudData::stop);
		try {
			if (resamplingThread != null)
				resamplingThread.join();
		} catch (Exception e) {
			//Ignore...
		}
	}

	// Building mesh
	//? if > 1.21 {
	private @Nullable MeshData rebuildCloudMesh(Tesselator tesselator, Vec3 cloudColor, double offset, float cloudHeight) {
	//? } else if > 1.19 {
	/*private @Nullable BufferBuilder.RenderedBuffer rebuildCloudMesh(BufferBuilder builder, Vec3 cloudColor, double offset, float cloudHeight) {
	*///? } else {
	/*private @Nullable BufferBuilder rebuildCloudMesh(BufferBuilder builder, Vec3 cloudColor, double offset, float cloudHeight) {
	*///? }
		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.getMainCamera();

		Vec3 look = null, up = null, right = null;
		double tanHalfFov = 0, tanHalfFovHorizontal = 0;
		boolean enableCulling = CONFIG.getEnableViewCulling();
		if (enableCulling) {
			look = new Vec3(camera.getLookVector());
			up = new Vec3(camera.getUpVector());
			//~ if ! 1.16.5 'up.cross(look).normalize()' -> 'new Vec3(camera.getLeftVector())'
			right = new Vec3(camera.getLeftVector());
			//~ if < 1.19 '.fov().get()' -> '.fov'
			tanHalfFov = Math.tan(Math.toRadians(CONFIG.getCullRadianMultiplier() * client.options.fov().get() * client.player.getFieldOfViewModifier()) / 2F);
			tanHalfFovHorizontal = tanHalfFov * client.getWindow().getWidth() / client.getWindow().getHeight();
		}

		int customColor = CONFIG.getCloudColor();  //apply custom color
		cloudColor = cloudColor.multiply(((customColor & 0xFF0000) >> 16) / 255F, ((customColor & 0xFF00) >> 8) / 255F, (customColor & 0xFF) / 255F);
		float cloudAlpha = ((customColor & 0xFF000000) >>> 24) / 255F;
		if (CONFIG.isEnableDuskBlush())  //apply dawn/dusk blush
			cloudColor = cloudColor.multiply(getBlushColorByTime(client.level.getDayTime()));

		//? if < 1.21 {
		/*builder.clear();
		//~ if ! 1.16.5 '7' -> 'VertexFormat.Mode.QUADS'
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		for (int i = 0; i < 4; i ++)  // empty builder will lead game crash... we draw a holder face to prevent that.
			builder.vertex(i, -99, i).uv(0.5f, 0.5f).color(0, 0, 0, 0).normal(0, -1, 0).endVertex();
		*///? } else {
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		for (int i = 0; i < 4; i ++)
			builder.addVertex(i, -99, i).setUv(0.5f, 0.5f).setColor(0, 0, 0, 0).setNormal(0, -1, 0);
		//? }

		cullStateShown = 0;
		cullStateSkipped = 0;

		final int refreshSpeed = CONFIG.getNormalRefreshSpeed().getValue();
		int cloudBlockSize = CONFIG.getCloudBlockSize();
		boolean enableBottomDim = CONFIG.isEnableBottomDim();
		boolean isDebug = CONFIG.isEnableDebug();
		try {
			for (CloudData data : cloudDataGroup) {
				switch (data.getDataType()) {  // Smooth Change: Alpha changed by cloud type and lifetime
					case TRANS_IN: cloudAlpha *= 1F - data.getLifeTime() / refreshSpeed * 5F; break;
					case TRANS_OUT: cloudAlpha *= data.getLifeTime() / refreshSpeed * 5F; break;
					default: break;
				}

				ArrayList<Integer> vertexList = data.meshData;  //make a snapshot to prevent concurrent violate
				int normCount = vertexList.size() / 4;

				for (int i = 0; i < normCount; i++) {
					int[][] verCache = new int[][]{		// exacting data...
							CloudData.depressVertex(vertexList.get(i * 4)),
							CloudData.depressVertex(vertexList.get(i * 4 + 1)),
							CloudData.depressVertex(vertexList.get(i * 4 + 2)),
							CloudData.depressVertex(vertexList.get(i * 4 + 3))
					};
					boolean isDrawn = false;

					if (isDebug && ! Common.isNoCloudCovered(Minecraft.getInstance().level,
							(verCache[0][0] + offset - 1) * cloudBlockSize + camera.getPosition().x(),
							63,
							(verCache[0][2] - 1) * cloudBlockSize + camera.getPosition().z()
					)) {
						cloudColor = cloudColor.multiply(0, 1, 0);
					}

					for (int j = 0; j <= 3; j ++) {
						if (enableCulling) {
							Vec3 cloudVec = new Vec3(  // turns to exactly pos & size to calc position culling (camera relative)
									(verCache[j][0] + offset - 1) * cloudBlockSize,
									verCache[j][1] * cloudBlockSize / 2f + cloudHeight + 0.33f - camera.getPosition().y(),
									(verCache[j][2] - 1) * cloudBlockSize + 0.33f
							);
							double depth = look.dot(cloudVec);
							if (depth < 0.05F ||
									Math.abs(up.dot(cloudVec)) / depth > tanHalfFov ||
									Math.abs(right.dot(cloudVec)) / depth > tanHalfFovHorizontal)
								continue;
						}
						CloudData.Facing facing = CloudData.Facing.get(CloudData.depressFromHead(vertexList.get(i * 4)));
						Vec3 faceColor = cloudColor.multiply(facing.color);
						if (enableBottomDim) {
							faceColor = faceColor.scale(Mth.clamp((255 - CloudData.depressFromHead(vertexList.get(i * 4 + 1)) * 8) / 255f, 0f, 1f));
						}
						int nx = facing.normal.getX();
						int ny = facing.normal.getY();
						int nz = facing.normal.getZ();
						for (int k = 0; k < 4; k++) {
							//? if < 1.21 {
							/*builder.vertex(verCache[k][0], verCache[k][1], verCache[k][2])
									.uv(0.5f, 0.5f)
									.color((float) faceColor.x, (float) faceColor.y, (float) faceColor.z, 0.8F * cloudAlpha)
									.normal(nx, ny, nz)
									.endVertex();
							*///? } else {
							builder.addVertex(verCache[k][0], verCache[k][1], verCache[k][2])
									.setUv(0.5f, 0.5f)
									.setColor((float) faceColor.x, (float) faceColor.y, (float) faceColor.z, 0.8F * cloudAlpha)
									.setNormal(nx, ny, nz);
							//? }
						}
						isDrawn = true;
						break;
					}

					if (isDrawn) {
						cullStateShown++;
					} else {
						cullStateSkipped++;
					}
				}

				if (data.getDataType().equals(CloudData.Type.NORMAL)) {
					break;
				} else if (data.getDataType().equals(CloudData.Type.TRANS_MID_BODY)) {
					data.tick();
					if (data.getLifeTime() <= 0) {		// Clear if lifetime reach end
						while (!cloudDataGroup.get(0).getDataType().equals(CloudData.Type.NORMAL)) {
							cloudDataGroup.remove(0);
						}
					}
					break;		// Only render IN, BODY, OUT till its life end and remove.
				} else {
					data.tick();
				}
			}

			//? if >= 1.21.1 {
			return builder.buildOrThrow();
			//? } else if > 1.19 {
			/*return builder.end();
			*///? } else {
			/*builder.end();
			return builder;
			*///? }
		} catch (Exception e) {
			exceptionCatcher(e);
			return null;
		}
	}

	protected void collectCloudData(int x, int y, int z) {
		CloudData tmp;
		CloudData fadeIn = null, fadeOut = null, midBody = null;

		tmp = new CloudData(x, y, z, DATA.densityByWeather, DATA.densityByBiome).buildMesh();
		if (!cloudDataGroup.isEmpty() && CONFIG.isEnableSmoothChange()) {
			fadeIn = new CloudData.CloudFadeData(cloudDataGroup.get(0), tmp, CloudData.Type.TRANS_IN).buildMesh();
			fadeOut = new CloudData.CloudFadeData(tmp, cloudDataGroup.get(0), CloudData.Type.TRANS_OUT).buildMesh();
			midBody = new CloudData.CloudMidData(cloudDataGroup.get(0), tmp, CloudData.Type.TRANS_MID_BODY).buildMesh();
		}
		cloudDataGroup.forEach(CloudData::stop);
		synchronized (this) {
			cloudDataGroup.clear();
			if (midBody != null) {
				cloudDataGroup.add(fadeIn);
				cloudDataGroup.add(fadeOut);
				cloudDataGroup.add(midBody);
			}
			cloudDataGroup.add(tmp);
		}
	}

	protected Vec3 getBlushColorByTime(long worldTime) {
		int r = 255, g = 255, b = 255;
		int t = (int) (worldTime % 24000);

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, scale value in [0, 2000]
			t = t > 22500 ? t - 22500 : t + 1500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			double v = Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3;
			g = (int) (g * (1 - v / 2.1));
			b = (int) (b * (1 - v / 1.6));
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			double v = Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3;
			g = (int) (g * (1 - v / 2.1));
			b = (int) (b * (1 - v / 1.6));
		}
		return new Vec3(r / 255F, g / 255F, b / 255F);
	}

	protected Vec3 getBrightMultiplier(Vec3 cloudColor) {
		return cloudColor.add(
				(1 - cloudColor.x) * CONFIG.getCloudBrightMultiplier(),
				(1 - cloudColor.y) * CONFIG.getCloudBrightMultiplier(),
				(1 - cloudColor.z) * CONFIG.getCloudBrightMultiplier()
		);
	}

	public float getCloudHeight() {
		return cloudHeight;
	}

	public boolean isCloudCovered(double x, double y, double z) {
		for(CloudData data : cloudDataGroup) {
			if (data != null && data.isCloudCovered(x + xOffset, y, z + zOffset))
				return true;
		}
		return false;
	}

	public String getDebugString() {
		return "[SFCR] build " + cullStateShown + "/" +
				(cullStateSkipped + cullStateShown) + " faces, cost " +
				debugRebuildTime + "ms, upload in " +
				debugUploadTime + "ms";
	}
}
