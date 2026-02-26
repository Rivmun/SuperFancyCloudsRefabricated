package com.rimo.sfcr.core;

import com.rimo.sfcr.Common;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.rimo.sfcr.Common.*;

/* Some note:
	DH official cloud renderer parse a picture to a renderableBoxGroup, then copy it 11*11 times as a cloud matrix,
	Its culling method cut matrix group by group, not by a single box
	We convert our cloudGrid to renderableBoxGroup and add it to DH's renderPass
	Also, thread-ify it.
 */
public class RendererDHCompat extends Renderer {
	private final DhApiRenderableBoxGroupShading cloudShading = createCloudShading();
	private IDhApiRenderableBoxGroup group;

	private float cloudBlockWidth, cloudBlockHeight;
	private double timeOffset;
	private int cloudColor;

	public RendererDHCompat() {}
	public RendererDHCompat(Renderer renderer) {super(renderer);}

	private DhApiRenderableBoxGroupShading createCloudShading() {
		DhApiRenderableBoxGroupShading cloudShading = new DhApiRenderableBoxGroupShading();
		cloudShading.north = cloudShading.south = 0.9F;
		cloudShading.east = cloudShading.west = 0.8F;
		cloudShading.top = 1.0F;
		cloudShading.bottom = 0.7F;
		return cloudShading;
	}

	//convert cloudGrid.boolean[][][] to List<DhApiRenderableBox>
	//Powered by Deepseek.ai
	private List<DhApiRenderableBox> convertGridForm(boolean[][][] grid) {
		int w = grid.length;
		int h = grid[0].length;
		boolean[][][] covered = new boolean[w][h][w];
		List<DhApiRenderableBox> result = new ArrayList<>();

		for (int z = 0; z < w; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (grid[x][y][z] && !covered[x][y][z]) {
						int xMin = x, xMax = x;
						int yMin = y, yMax = y;
						int zMin = z, zMax = z;

						//expand box
						while (xMax + 1 < w && grid[xMax + 1][y][z])  // X to right
							xMax++;
						while (xMin - 1 >= 0 && grid[xMin - 1][y][z])  // X to left
							xMin--;
						while (yMax + 1 < h) {  // Y to down
							boolean valid = true;
							for (int i = xMin; i <= xMax; i++) {
								if (!grid[i][yMax + 1][z]) {
									valid = false;
									break;
								}
							}
							if (valid) yMax++;
							else break;
						}
						while (yMin - 1 >= 0) {  // Y to up
							boolean valid = true;
							for (int i = xMin; i <= xMax; i++) {
								if (!grid[i][yMin - 1][z]) {
									valid = false;
									break;
								}
							}
							if (valid) yMin--;
							else break;
						}
						while (zMax + 1 < w) {  // Z to forward
							boolean valid = true;
							for (int i = xMin; i <= xMax; i++) {
								for (int j = yMin; j <= yMax; j++) {
									if (!grid[i][j][zMax + 1]) {
										valid = false;
										break;
									}
								}
								if (!valid) break;
							}
							if (valid) zMax++;
							else break;
						}
						while (zMin - 1 >= 0) {  // Z to backward
							boolean valid = true;
							for (int i = xMin; i <= xMax; i++) {
								for (int j = yMin; j <= yMax; j++) {
									if (!grid[i][j][zMin - 1]) {
										valid = false;
										break;
									}
								}
								if (!valid) break;
							}
							if (valid) zMin--;
							else break;
						}

						// mark as covered
						for (int k = zMin; k <= zMax; k++) {
							for (int j = yMin; j <= yMax; j++) {
								for (int i = xMin; i <= xMax; i++) {
									covered[i][j][k] = true;
								}
							}
						}

						// Add AABB box
						result.add(new DhApiRenderableBox(
								new DhApiVec3d(
										(xMin - w / 2) * cloudBlockWidth,  //offset to center
										yMin * cloudBlockHeight,
										(zMin - w / 2) * cloudBlockWidth
								),
								new DhApiVec3d(
										(++xMax - w / 2) * cloudBlockWidth,  //++at least one block size
										++yMax * cloudBlockHeight,
										(++zMax - w / 2) * cloudBlockWidth
								),
								new Color(255,255,255,255),  //color change by time, here just placeholder
								EDhApiBlockMaterial.UNKNOWN
						));
					}
				}
			}
		}
		return result;
	}

	//update cloud invoked by mixin (instead of manual call in 2.0)
	@Override
	public void render(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ,
	                   ClientWorld world, int ticks) {
		float cloudHeight = CONFIG.getCloudHeight() < 0 ? world.getDimensionEffects().getCloudsHeight() : CONFIG.getCloudHeight();
		if (Float.isNaN(cloudHeight))
			return;
		this.cloudHeight = cloudHeight;

		//vanilla cloud pos calculation
		final float CLOUD_BLOCK_WIDTH = CONFIG.getCloudBlockSize();  //cloud size
		final float CLOUD_BLOCK_HEIGHT = CLOUD_BLOCK_WIDTH / 2F;
		double timeOffset = (ticks + tickDelta) * 0.03F;
		double cloudX = (cameraX + timeOffset) / CLOUD_BLOCK_WIDTH;  //grid pos where to draw cloud layer
		double cloudY = cloudHeight - (float) cameraY + 0.33F;
		double cloudZ = cameraZ / CLOUD_BLOCK_WIDTH + 0.33F;
		int GridX = (int) Math.floor(cloudX);  //cloud grid pos !!NOTICE that timeOffset is already contained.
		int GridY = (int) Math.floor(cloudY / CLOUD_BLOCK_HEIGHT);
		int GridZ = (int) Math.floor(cloudZ);
		Vec3d cloudColor = world.getCloudsColor(tickDelta);

		cloudColor = getBrightMultiplier(cloudColor);

		//refresh check
		resamplingTimer += MinecraftClient.getInstance().getLastFrameDuration() * 0.25 * 0.25;
		if (! MinecraftClient.getInstance().isPaused() && ! isResampling &&
				(resamplingTimer > DATA.getResamplingInterval() || oldGridX != GridX || oldGridZ != GridZ)) {
			isResampling = true;
			resamplingTimer = 0.0;
			resamplingThread = new Thread(() -> {  //start data refresh thread
				try {
					collectCloudData(GridX, GridY, GridZ);
				} catch (Exception e) {
					exceptionCatcher(e);
				} finally {
					oldGridX = GridX;
					oldGridZ = GridZ;
					isResampling = false;
				}
			});
			resamplingThread.start();
		}

		this.cloudBlockHeight = CLOUD_BLOCK_HEIGHT;
		this.cloudBlockWidth = CLOUD_BLOCK_WIDTH;
		this.timeOffset = timeOffset;
		this.cloudColor = ColorHelper.Argb.mixColor(
				getCloudColor(world.getTimeOfDay(), null),
				new Color((int) (cloudColor.x * 255), (int) (cloudColor.y * 255), (int) (cloudColor.z * 255)).getRGB()
		);
	}

	//add RenderableBoxGroup build and replace.
	@Override
	protected void collectCloudData(int x, int y, int z) {
		if (!DhApi.Delayed.configs.graphics().renderingEnabled().getValue())
			return;  //save battery if DH render was disabled.
		IDhApiRenderableBoxGroup newGroup = DhApi.Delayed.customRenderObjectFactory.createRelativePositionedGroup(
				Common.MOD_ID + ":clouds",
				new DhApiVec3d(0, 0, 0),
				convertGridForm(new CloudData(x, y, z, DATA.densityByWeather, DATA.densityByBiome)._cloudData)
		);
		newGroup.setBlockLight(15);
		newGroup.setSkyLight(15);
		newGroup.setSsaoEnabled(false);
		newGroup.setShading(cloudShading);
		newGroup.setPreRenderFunc(renderParam -> preRender(newGroup));
		synchronized (this) {
			IDhApiCustomRenderRegister renderRegister = DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister();
			if (group != null)
				renderRegister.remove(group.getId());  //clear old group
			renderRegister.add(newGroup);
			group = newGroup;
			cullStateShown = group.size();
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (group != null && DhApi.Delayed.worldProxy.worldLoaded()) {
			DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister().remove(group.getId());
			group = null;
		}
	}

	// Below we rewrite 2 method from
	// com.seibel.distanthorizons.core.render.renderer.generic.CloudRenderHandler

	//rewrite of original constructor (more likes entire delete?)
	//update check by mixin vanilla call, so yep, we delete it completely XD

	//to calc RenderableBoxGroup pos and culling, etc..
	private void preRender(IDhApiRenderableBoxGroup group) {

		/* TODO: culling?
		    but we have only one group. considering is unnecessary..
		    if we want, try slicing cloudGrid into convertor, to get RenderableBoxGroup[] of sliced cloudGrid.
		 */

		//color
		if (!group.isEmpty()) {
			Color color = new Color(cloudColor);
			if (!group.get(0).color.equals(color)) {
				for (DhApiRenderableBox box : group)
					box.color = color;
				group.triggerBoxChange();
			}
		}

		//pos
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		double xOffsetInGrid = (cameraPos.getX() + timeOffset) / cloudBlockWidth - oldGridX;
		double zOffsetInGrid = cameraPos.getZ() / cloudBlockWidth + 0.33F - oldGridZ;
		xOffsetInGrid *= cloudBlockWidth;  //turns to blocks
		zOffsetInGrid *= cloudBlockWidth;
		/* I'm fk realized there should be simply "cameraPos - offset" ...
		 * W T F to my brain (╯‵□′)╯︵┻━┻ */
		double cloudX = cameraPos.getX() - xOffsetInGrid;
		double cloudZ = cameraPos.getZ() - zOffsetInGrid;
		double cloudY = getCloudHeight() + 0.33F;
		group.setOriginBlockPos(new DhApiVec3d(cloudX, cloudY, cloudZ));
	}
}
