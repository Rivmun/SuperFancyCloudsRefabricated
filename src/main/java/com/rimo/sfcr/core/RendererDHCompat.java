package com.rimo.sfcr.core;

import com.rimo.sfcr.Common;
import com.rimo.sfcr.VersionUtil;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.rimo.sfcr.Common.CONFIG;

/* Some note:
	DH official cloud renderer parse a picture to a renderableBoxGroup, then copy it 11*11 times as a cloud matrix,
	Its culling method cut matrix group by group, not by a single box
	We convert our cloudGrid to renderableBoxGroup and add it to DH's renderPass
	Also, thread-ify it.
 */
public class RendererDHCompat extends Renderer {
	private final DhApiRenderableBoxGroupShading cloudShading = createCloudShading();
	private IDhApiRenderableBoxGroup group;
	private float cloudHeight = 192.33F;

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
		int h = grid[0][0].length;
		boolean[][][] covered = new boolean[w][w][h];
		List<DhApiRenderableBox> result = new ArrayList<>();
		debugBuiltCounter = 0;
		debugCullCounter = 0;

		for (int z = 0; z < h; z++) {  //note that z is height
			for (int y = 0; y < w; y++) {
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
						while (yMax + 1 < w) {  // Y to down
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
						while (zMax + 1 < h) {  // Z to forward
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
										zMin * cloudBlockHeight,
										(yMin - w / 2) * cloudBlockWidth
								),
								new DhApiVec3d(
										(++xMax - w / 2) * cloudBlockWidth,  //++at least one block size
										++zMax * cloudBlockHeight,
										(++yMax - w / 2) * cloudBlockWidth
								),
								new Color(255,255,255,255),  //color change by time, here just placeholder
								EDhApiBlockMaterial.UNKNOWN
						));
						debugBuiltCounter ++;
					}
				}
			}
		}
		return result;
	}

	//add RenderableBoxGroup build and replace.
	@Override
	protected void updateCloudGrid(int renderRange) {
		CloudGrid newGrid = getCloudGrid(gridX, gridZ, renderRange);
		if (newGrid == null)
			return;
		if (cloudGrid != null) {
			int oldOffset = Math.abs(gridX - cloudGrid.centerX()) + Math.abs(gridZ - cloudGrid.centerZ());
			int newOffset = Math.abs(gridX - newGrid.centerX()) + Math.abs(gridZ - newGrid.centerZ());
			if (newOffset > oldOffset)
				return;  //pick grids closer to player
		}
		IDhApiRenderableBoxGroup newGroup = DhApi.Delayed.customRenderObjectFactory.createRelativePositionedGroup(
				Common.MOD_ID + ":clouds",
				new DhApiVec3d(0, 0, 0),
				convertGridForm(newGrid.grids())
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
			cloudGrid = newGrid;
			group = newGroup;
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (group != null) {
			try {
				DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister().remove(group.getId());
			} catch (IllegalStateException e) {
				//
			}
			group = null;
		}
	}

	// Below we rewrite method from
	// com.seibel.distanthorizons.core.render.renderer.generic.CloudRenderHandler

	//rewrite of original constructor (more likes entire delete?)
	//to dynamic build cloud RenderableBoxGroup, we replaced picture parse to grid convertor,
	//and move it to thread-ify method updateCloudGrid
	@Override
	public void render(int cloudColor, float cloudHeight, Vec3 camPos, float partialTick, MappableRingBuffer infoBuffer, MappableRingBuffer faceBuffer, int renderRange, Level level) {
		if (!DhApi.Delayed.configs.graphics().renderingEnabled().getValue())
			return;  //save battery if DH render was disabled.

//		float offsetY = (float)((double)cloudHeight - camPos.y);
//		int gridY = Mth.floor(offsetY / cloudHeight);

		float timeOffset = level.getGameTime() + partialTick;
		double cloudX = camPos.x + timeOffset * 0.030000001F;
		double cloudZ = camPos.z + 3.96F;
		cloudBlockWidth = CONFIG.getCloudBlockSize();
		cloudBlockHeight = cloudBlockWidth / 2;
		int gridX = Mth.floor(cloudX / cloudBlockWidth);
		int gridZ = Mth.floor(cloudZ / cloudBlockWidth);
		float offsetX = (float)(cloudX - (double)((float)gridX * cloudBlockWidth));
		float offsetZ = (float)(cloudZ - (double)((float)gridZ * cloudBlockWidth));

		renderRange *= CONFIG.getDhRenderRangeMultiplier();
		this.cloudHeight = cloudHeight;
		this.xOffset = offsetX;
		this.zOffset = offsetZ;

		resamplingTimer += VersionUtil.getLastFrameDuration() * 0.25 * 0.25;
		if (! Minecraft.getInstance().isPaused() && (gridX != this.gridX || gridZ != this.gridZ || isTimeToResampling())) {
			resamplingTimer = 0;
			this.gridX = gridX;
			this.gridZ = gridZ;
			if (cloudGrid == null) {
				updateCloudGrid(renderRange);  //resampling directly at first time
			} else {
				tryStartGridUpdateThread(renderRange);
			}
		}
	}

	//to calc RenderableBoxGroup pos and culling, etc...
	private void preRender(IDhApiRenderableBoxGroup group) {
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;
		GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
		Vec3 cameraPos = gameRenderer.getMainCamera().position();
		float offsetX = xOffset + (gridX - cloudGrid.centerX()) * cloudBlockWidth;
		float offsetZ = zOffset + (gridZ - cloudGrid.centerZ()) * cloudBlockWidth;

		/* TODO: culling?
		    but we have only one group. considering is unnecessary..
		    if we want, try slicing cloudGrid into convertor, to get RenderableBoxGroup[] of sliced cloudGrid.
		 */

		//color
		if (!group.isEmpty()) {
			int iColor = gameRenderer.getMainCamera().attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, VersionUtil.getLastFrameDuration());
			Color color = new Color(iColor);
			if (!group.getFirst().color.equals(color)) {
				for (DhApiRenderableBox box : group)
					box.color = color;
				group.triggerBoxChange();
			}
		}

		//TODO: why cloudHeight is follow by player?
		//simply 'cameraPos - offset'...
		group.setOriginBlockPos(new DhApiVec3d(cameraPos.x() - offsetX, cloudHeight, cameraPos.z() - offsetZ));
	}
}
