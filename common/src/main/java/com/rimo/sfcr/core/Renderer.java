package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rimo.sfcr.config.CullMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.rimo.sfcr.Common.*;

public class Renderer {
	private final Identifier whiteTexture = new Identifier(MOD_ID, "white.png");
	private final ArrayList<CloudData> cloudDataGroup = new ArrayList<>();
	private VertexBuffer cloudsBuffer;
	protected boolean isResampling = false;
	protected Thread resamplingThread;
	protected float cloudHeight;
	protected int oldGridX, oldGridZ;
	private Vec3d oldColor = Vec3d.ZERO;
	protected double xOffset, zOffset;
	protected double resamplingTimer = 0.0;  //manual update counter
	private int rebuildTimer = 0;  //measure in ticks
	public int cullStateSkipped, cullStateShown;  //debug counter
	public double debugRebuildTime, debugUploadTime;

	public Renderer() {}
	public Renderer(Renderer renderer) {
		renderer.stop();
	}

	//Rewrite of vanilla renderClouds invoke by mixin
	public void render(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ,
	                   ClientWorld world, int ticks) {
		float cloudHeight = CONFIG.getCloudHeight() < 0 ? world.getSkyProperties().getCloudsHeight() : CONFIG.getCloudHeight();
		if (Float.isNaN(cloudHeight))
			return;
		this.cloudHeight = cloudHeight;
		boolean isPause = MinecraftClient.getInstance().isPaused();

		//vanilla cloud pos calculation
		final float CLOUD_BLOCK_WIDTH = CONFIG.getCloudBlockSize();  //cloud size
		final float CLOUD_BLOCK_HEIGHT = CLOUD_BLOCK_WIDTH / 2F;
		double timeOffset = (ticks + tickDelta) * 0.03F;
		double cloudX = (cameraX + timeOffset) / CLOUD_BLOCK_WIDTH;  //grid pos where to draw cloud layer
		double cloudY = cloudHeight - (float) cameraY + 0.33F;
		double cloudZ = cameraZ / CLOUD_BLOCK_WIDTH + 0.33F;
		int GridX = (int) Math.floor(cloudX);  //cloud grid pos !!NOTICE that timeOffset is already contained.
		//int GridY = (int) Math.floor(cloudY / CLOUD_BLOCK_HEIGHT);
		int GridZ = (int) Math.floor(cloudZ);
		float xOffsetInGrid = (float) (cloudX - Math.floor(cloudX));  //cloud offset in current grid
		//float yOffsetInGrid = (float) ((cloudY / CLOUD_BLOCK_HEIGHT - Math.floor(cloudY / CLOUD_BLOCK_HEIGHT)) * CLOUD_BLOCK_HEIGHT);
		float zOffsetInGrid = (float) (cloudZ - Math.floor(cloudZ));
		Vec3d cloudColor = world.getCloudsColor(tickDelta);

		cloudColor = getBrightMultiplier(cloudColor);
		synchronized (this) {
			xOffsetInGrid += GridX - oldGridX;
			zOffsetInGrid += GridZ - oldGridZ;
		}
		this.xOffset = (xOffsetInGrid - 0.33F) * CLOUD_BLOCK_WIDTH;
		this.zOffset = (zOffsetInGrid - 0.33F) * CLOUD_BLOCK_WIDTH;
		int cameraGridY = (int) (cameraY / CLOUD_BLOCK_HEIGHT);

		//refresh check
		resamplingTimer += MinecraftClient.getInstance().getLastFrameDuration() * 0.25 * 0.25;
		if (! isPause && ! isResampling) {
			if (resamplingTimer > DATA.getResamplingInterval() || oldGridX != GridX || oldGridZ != GridZ || oldColor.squaredDistanceTo(cloudColor) > 2.0E-4) {
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
		RenderSystem.enableAlphaTest();
		RenderSystem.enableDepthTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

		//cloud mesh rebuilt
		/* NOTE:
		 * When cloudData is too HUGE, .upload() will get big lag, so we made a soft culling in rebuild to decrease upload size.
		 * But precisely because of culling, mesh rebuild must run in every tick instead of concurrent, to prevent bad visual.
		 * We made a lot of small lags of culling equation to replace a single big lag of upload, it's hard to say which is better...
		 */
		boolean isCullingDisabled = CONFIG.getCullMode().equals(CullMode.NONE);
		//if culling is disabled, no need to rebuild in every tick.
		if (! isPause && (isCullingDisabled && rebuildTimer == 99 || ! isCullingDisabled && ++ rebuildTimer > CONFIG.getRebuildInterval())) {
			rebuildTimer = 0;
			debugRebuildTime = System.nanoTime();
			BufferBuilder cb = rebuildCloudMesh(Tessellator.getInstance().getBuffer(), cloudColor, xOffsetInGrid, cloudHeight);
			debugRebuildTime = (System.nanoTime() - debugRebuildTime) / 1000000;
			if (cb != null) {
				if (cloudsBuffer != null)
					cloudsBuffer.close();
				cloudsBuffer = new VertexBuffer(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
				debugUploadTime = System.nanoTime();
				cloudsBuffer.upload(cb);
				debugUploadTime = (System.nanoTime() - debugUploadTime) / 1000000;
			}
		}

		//Setup shader
		MinecraftClient.getInstance().getTextureManager().bindTexture(whiteTexture);
		float viewDistance = MinecraftClient.getInstance().gameRenderer.getViewDistance();
		if (CONFIG.isEnableFog()) {
			RenderSystem.enableFog();
			if (!CONFIG.isFogAutoDistance()) {
				RenderSystem.fogStart(viewDistance * CONFIG.getFogMinDistance() * CONFIG.getCloudBlockSize() / 16);
				RenderSystem.fogEnd(viewDistance * CONFIG.getFogMaxDistance() * CONFIG.getCloudBlockSize() / 16);
			} else {
				RenderSystem.fogStart(viewDistance * CONFIG.getAutoFogMaxDistance() / 2);
				RenderSystem.fogEnd(viewDistance * CONFIG.getAutoFogMaxDistance());
			}
		} else {
			RenderSystem.disableFog();
		}
		RenderSystem.depthMask(true);

		matrices.push();
		matrices.scale(CLOUD_BLOCK_WIDTH, CLOUD_BLOCK_HEIGHT, CLOUD_BLOCK_WIDTH);
		matrices.translate(-xOffsetInGrid, cloudY / CLOUD_BLOCK_HEIGHT, -zOffsetInGrid);  //strange that if I use yOffsetInGrid here, cloudLayer height is unstable...
		RenderSystem.color4f((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1);

		if (cloudsBuffer != null) {
			cloudsBuffer.bind();
			VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.startDrawing(0L);
			for (int s = 0; s < 2; ++s) {
				if (s == 0) {
					RenderSystem.colorMask(false, false, false, false);
				} else {
					RenderSystem.colorMask(true, true, true, true);
				}
				this.cloudsBuffer.draw(matrices.peek().getModel(), 7);
			}
			VertexBuffer.unbind();
			VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.endDrawing();
		}

		//Restore render system
		matrices.pop();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableAlphaTest();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.disableFog();
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

	enum FACING {
		EAST(1, 0, 0),
		WEST(-1, 0, 0),
		TOP(0, 1, 0),
		BOTTOM(0, -1, 0),
		SOUTH(0, 0, 1),
		NORTH(0, 0, -1);

		final Vec3i normal;

		FACING(int x, int y, int z) {
			this.normal = new Vec3i(x, y, z);
		}

		static FACING get(int i) {
			return FACING.values()[i];
		}
	}

	private final Vec3d[] colors = {
			new Vec3d(0.95f, 0.9f,  0.9f),
			new Vec3d(0.75f, 0.75f, 0.75f),
			new Vec3d(1f,    1f,    1f),
			new Vec3d(0.6f,  0.6f,  0.6f),
			new Vec3d(0.92f, 0.85f, 0.85f),
			new Vec3d(0.8f,  0.8f,  0.8f),
	};

	// Building mesh
	private @Nullable BufferBuilder rebuildCloudMesh(BufferBuilder builder, Vec3d cloudColor, double offset, float cloudHeight) {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d camVec = null;
		Vec3d[] camProjBorder = null;
		double fovCos = 0, extraAngleSin = 0;

		Camera camera = client.gameRenderer.getCamera();
		int fov = (int) client.options.fov;
		float fovMultiplier = 1F;
		if (CONFIG.getCullMode().equals(CullMode.CIRCULAR)) {
			camVec = new Vec3d(
					-Math.sin(Math.toRadians(camera.getYaw())),
					-Math.tan(Math.toRadians(camera.getPitch())),
					Math.cos(Math.toRadians(camera.getYaw()))
			).normalize();
			fovCos = Math.cos(Math.toRadians(fov * fovMultiplier * CONFIG.getCullRadianMultiplier()));		//multiplier 2 for better visual.
		} else if (CONFIG.getCullMode().equals(CullMode.RECTANGULAR)) {
			Camera.Projection camProj = client.gameRenderer.getBasicProjectionMatrix();
			camProjBorder = new Vec3d[]{
					camProj.getTopRight().crossProduct(camProj.getTopLeft()).normalize(),			//up
					camProj.getBottomLeft().crossProduct(camProj.getBottomRight()).normalize(),		//down
					camProj.getTopLeft().crossProduct(camProj.getBottomLeft()).normalize(),			//left
					camProj.getBottomRight().crossProduct(camProj.getTopRight()).normalize()		//right
			};
			extraAngleSin = Math.sin(Math.toRadians(fov * (1.9f - fovMultiplier - CONFIG.getCullRadianMultiplier())));		//increase 0.1 for better visual.
		}

		int customColor = CONFIG.getCloudColor();  //apply custom color
		cloudColor = cloudColor.multiply(((customColor & 0xFF0000) >> 16) / 255F, ((customColor & 0xFF00) >> 8) / 255F, (customColor & 0xFF) / 255F);
		float cloudAlpha = ((customColor & 0xFF000000) >>> 24) / 255F;
		if (CONFIG.isEnableDuskBlush())  //apply dawn/dusk blush
			cloudColor = cloudColor.multiply(getBlushColorByTime(client.world.getTimeOfDay()));

		builder.clear();
		builder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

		for (int i = 0; i < 4; i ++)  // empty builder will lead game crash... we draw a holder face to prevent that.
			builder.vertex(i, -99, i).texture(0.5f, 0.5f).color(0, 0, 0, 0).normal(0, -1, 0).next();

		cullStateShown = 0;
		cullStateSkipped = 0;

		try {
			for (CloudData data : cloudDataGroup) {
				switch (data.getDataType()) {  // Smooth Change: Alpha changed by cloud type and lifetime
					case TRANS_IN: cloudAlpha *= 1F - data.getLifeTime() / CONFIG.getNormalRefreshSpeed().getValue() * 5f; break;
					case TRANS_OUT: cloudAlpha *= data.getLifeTime() / CONFIG.getNormalRefreshSpeed().getValue() * 5f; break;
					default: break;
				};

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

					for (int j = 0; j <= 3; j ++) {
						Vec3d cloudVec = new Vec3d(  // turns to exactly pos & size to calc position culling
								(verCache[j][0] + offset - 1) * CONFIG.getCloudBlockSize(),
								verCache[j][1] * CONFIG.getCloudBlockSize() / 2f + cloudHeight + 0.33f - camera.getPos().y,
								(verCache[j][2] - 1) * CONFIG.getCloudBlockSize() + 0.33f
						).normalize();
						boolean isInRange = true;
						if (camVec != null && camVec.dotProduct(cloudVec) < fovCos) {
							continue;
						} else if (camProjBorder != null) {
							//TODO: 所有顶点都在视野外，但斜边仍可能与视野存在交集。
							// 或许应该反向计算，构建顶点四边与相机的 projBorder 拿去和相机的投影顶点算点乘（你看 camProj 对象本身就是由相机投影四个顶点构成的）
							// 不过因为需要给每个面都构建四个投影平面，而非每 tick 仅为相机构建四次，计算量估计要翻好几番…
							for (Vec3d plane : camProjBorder) {
								if (plane.dotProduct(cloudVec) < extraAngleSin) {
									isInRange = false;
									break;  //skipped if this vertex out of any projection plane
								}
							}
						}
						if (isInRange) {
							FACING facing = FACING.get(CloudData.depressFromHead(vertexList.get(i * 4)));
							Vec3d faceColor = cloudColor.multiply(colors[facing.ordinal()]);
							if (CONFIG.isEnableBottomDim()) {
								faceColor = faceColor.multiply(MathHelper.clamp((255 - CloudData.depressFromHead(vertexList.get(i * 4 + 1)) * 8) / 255f, 0f, 1f));
							}
							int nx = facing.normal.getX();
							int ny = facing.normal.getY();
							int nz = facing.normal.getZ();
							for (int k = 0; k < 4; k++) {
								builder.vertex(verCache[k][0], verCache[k][1], verCache[k][2])
										.texture(0.5f, 0.5f)
										.color((float) faceColor.x, (float) faceColor.y, (float) faceColor.z, 0.8F * cloudAlpha)
										.normal(nx, ny, nz)
										.next();
							}
							isDrawn = true;
							break;
						}
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

			builder.end();
			return builder;
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
			fadeIn = new CloudFadeData(cloudDataGroup.get(0), tmp, CloudData.Type.TRANS_IN).buildMesh();
			fadeOut = new CloudFadeData(tmp, cloudDataGroup.get(0), CloudData.Type.TRANS_OUT).buildMesh();
			midBody = new CloudMidData(cloudDataGroup.get(0), tmp, CloudData.Type.TRANS_MID_BODY).buildMesh();
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

	protected Vec3d getBlushColorByTime(long worldTime) {
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
		return new Vec3d(r / 255F, g / 255F, b / 255F);
	}

	protected Vec3d getBrightMultiplier(Vec3d cloudColor) {
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
}
