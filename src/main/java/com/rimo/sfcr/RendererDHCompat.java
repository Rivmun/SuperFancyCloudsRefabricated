package com.rimo.sfcr;

import com.rimo.sfcr.mixin.WorldRendererAccessor;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/* Some note:
	DH official cloud renderer parse a picture to a renderableBoxGroup, then copy it 11*11 times as a cloud matrix,
	Its culling method cut matrix group by group, not by a single box
	We convert our cloudGrid to renderableBoxGroup and add it to DH's renderPass
	Also, thread-ify it.
 */
public class RendererDHCompat extends Renderer{
	private final DhApiRenderableBoxGroupShading cloudShading = DhApiRenderableBoxGroupShading.getUnshaded();
	private IDhApiRenderableBoxGroup group;

	public RendererDHCompat() {
		this.setCloudShading();
	}

	public RendererDHCompat(Renderer renderer) {
		super(renderer);
		this.setCloudShading();
	}

	private void setCloudShading() {
		cloudShading.north = cloudShading.south = 0.9F;
		cloudShading.east = cloudShading.west = 0.8F;
		cloudShading.top = 1.0F;
		cloudShading.bottom = 0.7F;
	}

	//convert cloudGrid.boolean to List<DhApiRenderableBox>
	//Powered by Deepseek.ai
	private List<DhApiRenderableBox> convertGridForm(boolean[][][] grid) {
		int w = grid.length;
		int h = grid[0][0].length;
		boolean[][][] covered = new boolean[w][w][h];
		List<DhApiRenderableBox> result = new ArrayList<>();

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
										(xMin - w / 2) * CLOUD_BLOCK_WIDTH,  //trans to offset pos from camera
										zMin * CLOUD_BLOCK_HEIGHT,
										(yMin - w / 2) * CLOUD_BLOCK_WIDTH
								),
								new DhApiVec3d(
										(++xMax - w / 2) * CLOUD_BLOCK_WIDTH,  //++at least one block
										++zMax * CLOUD_BLOCK_HEIGHT,
										(++yMax - w / 2) * CLOUD_BLOCK_WIDTH
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

	@Override
	protected int getRenderDistance() {
		return super.getRenderDistance() * Common.CONFIG.getDhDistanceMultipler();
	}

	//add RenderableBoxGroup build and replace.
	@Override
	protected void updateCloudGrid() {
		CloudGrid newGrid = getCloudGrid(gridX, gridZ);
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
			if (cloudGrid != null) {
				int oldOffset = Math.abs(gridX - cloudGrid.centerX()) + Math.abs(gridZ - cloudGrid.centerZ());
				int newOffset = Math.abs(gridX - newGrid.centerX()) + Math.abs(gridZ - newGrid.centerZ());
				if (newOffset > oldOffset)
					return;  //pick grids closer to player
			}
			IDhApiCustomRenderRegister renderRegister = DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister();
			if (group != null)
				renderRegister.remove(group.getId());  //clear old group
			renderRegister.add(newGroup);
			cloudGrid = newGrid;
			group = newGroup;
			isResampling = false;
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (group != null) {
			DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister().remove(group.getId());
			group = null;
		}
	}

	// Below we rewrite 2 method from
	// com.seibel.distanthorizons.core.render.renderer.generic.CloudRenderHandler

	//rewrite of original constructor (more likes entire delete?)
	//to dynamic build cloud RenderableBoxGroup, we replaced picture parse to grid convertor,
	//and move it to thread-ify method updateCloudGrid
	public void updateDHRenderer() {
		if (!DhApi.Delayed.configs.graphics().renderingEnabled().getValue())
			return;  //save battery if DH render was disabled.
		if (cloudGrid == null) {
			updateCloudGrid();
		} else if (isGridNeedToUpdate()) {
			this.startGridUpdateThread();
		}
	}

	//to calc RenderableBoxGroup pos and culling, etc..
	private void preRender(IDhApiRenderableBoxGroup group) {
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		RenderTickCounter tickCounter = MinecraftClient.getInstance().getRenderTickCounter();

		//pos calc copy from net.minecraft.client.render.WorldRenderer
		float cloudPhase = tickCounter.getTickProgress(false)
				+ (float)((WorldRendererAccessor)MinecraftClient.getInstance().worldRenderer).getTicks();
		double x = cameraPos.x + (double)(cloudPhase * 0.030000001F);
		double z = cameraPos.z + 3.9600000381469727;
		double height = cloudHeight + Common.CONFIG.getDhHeightEnhance();
		x -= cloudGrid.centerX() * CLOUD_BLOCK_WIDTH;  //trans to offset
		z -= cloudGrid.centerZ() * CLOUD_BLOCK_WIDTH;

		/* TODO: culling?
		    but we have only one group. considering is unnecessary..
		    if we want, try slicing cloudGrid into convertor, to get RenderableBoxGroup[] of sliced cloudGrid.
		 */

		//color
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world != null && !group.isEmpty()) {
			int iColor = world.getCloudsColor(tickCounter.getTickProgress(false));
			Color color = new Color(iColor);
			if (!group.get(0).color.equals(color)) {
				for (DhApiRenderableBox box : group)
					box.color = color;
				group.triggerBoxChange();
			}
		}

		group.setOriginBlockPos(new DhApiVec3d(-x, height, -z));
	}
}
