package com.rimo.sfcr.core;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.MOD_ID;

/* Some note:
	DH official cloud renderer parse a picture to a renderableBoxGroup, then copy it 11*11 times as a cloud matrix,
	Its culling method cut matrix group by group, not by a single box
	We convert our cloudGrid to renderableBoxGroup and add it to DH's renderPass
	Also, thread-ify it.
	@see com.seibel.distanthorizons.core.render.renderer.generic.CloudRenderHandler
 */
public class RendererDHCompat extends Renderer {
	private final DhApiRenderableBoxGroupShading cloudShading = createCloudShading();
	private IDhApiRenderableBoxGroup group;
	private float cloudHeight = 192.33F;
	private boolean isRemeshing = false;
	private Thread remeshingThread;

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

	@Override
	public void stop() {
		super.stop();
		try {
			if (remeshingThread != null)
				remeshingThread.join();
		} catch (Exception ignore) {}
		if (group != null) {
			try {
				DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister().remove(group.getId());
			} catch (IllegalStateException ignore) {}
			group = null;
		}
	}

	@Override
	public void render(int cloudColor, float cloudHeight, Vec3 camPos, float partialTick, MappableRingBuffer infoBuffer, MappableRingBuffer faceBuffer, int renderRange, Level level) {
		if (!DhApi.Delayed.configs.graphics().renderingEnabled().getValue())
			return;  //save battery if DH render was disabled.
		this.cloudHeight = cloudHeight;
		super.render(cloudColor, cloudHeight, camPos, partialTick, infoBuffer, faceBuffer, renderRange, level);
	}

	@Override
	protected void _render(int gridX, int gridY, int gridZ, MappableRingBuffer faceBuffer, MappableRingBuffer infoBuffer, int renderRange, int cloudColor, float offsetX, float offsetY, float offsetZ) {
		if (! Minecraft.getInstance().isPaused() && ! isRemeshing && (
				rebuildTick >= 999 ||
				gridX != this.gridX || gridZ != this.gridZ || (this.gridY != gridY && this.gridY >= 0 && this.gridY < CONFIG.getCloudLayerThickness())
		)) {
			isRemeshing = true;
			rebuildTick = 0;
			this.gridX = gridX;
			this.gridY = gridY;
			this.gridZ = gridZ;

			if (CONFIG.isThreadifyDHRemesh()) {
				remeshingThread = new Thread(() -> {
					buildMesh(renderRange, cloudGrid, cloudColor);
					isRemeshing = false;
				});
				remeshingThread.start();
			} else {
				buildMesh(renderRange, cloudGrid, cloudColor);
				isRemeshing = false;
			}
		}
	}

	private void buildMesh(int renderRange, CloudGrid cloudGrid, int cloudColor) {
		if (cloudGrid == null)
			return;
		int customColor = CONFIG.getCloudColor();  //apply custom color
		Vec3 color = new Vec3(((cloudColor & 0xFF0000) >> 16) / 255F, ((cloudColor & 0xFF00) >> 8) / 255F, (cloudColor & 0xFF) / 255F)
				.multiply(((customColor & 0xFF0000) >> 16) / 255F, ((customColor & 0xFF00) >> 8) / 255F, (customColor & 0xFF) / 255F);
		float alpha = (customColor >>> 24) / 255F;

		long debugTime = System.nanoTime();
		//transform grid to boxes
		List<DhApiRenderableBox> boxList = new ArrayList<>();
		if (gridY > 0 && gridY <= cloudGrid.grids()[0][0].length &&
				cloudGrid.grids()[renderRange][renderRange][gridY]) {  //inner faces check
			for (Direction direction : Direction.values()) {
				addBox(boxList, 0, gridY, 0, direction, 16, color, alpha);
			}
		} else {
			for (int l = 0; l <= 2 * renderRange; ++ l) {
				for (int xOffset = - l; xOffset <= l; ++ xOffset) {
					int zOffset = l - Math.abs(xOffset);
					if (zOffset >= 0 && zOffset <= renderRange && xOffset * xOffset + zOffset * zOffset <= renderRange * renderRange) {
						if (zOffset != 0) {
							tryBuildCellProxy(boxList, xOffset, - zOffset, renderRange, cloudGrid, color, alpha);
						}
						tryBuildCellProxy(boxList, xOffset, zOffset, renderRange, cloudGrid, color, alpha);
					}
				}
			}
		}
		debugBuiltTime = (System.nanoTime() - debugTime) / 1000000F;
		debugBuiltCounter = boxList.size();
		debugCullCounter = 0;

		//build group
		IDhApiRenderableBoxGroup newGroup = DhApi.Delayed.customRenderObjectFactory.createRelativePositionedGroup(
				MOD_ID + ":clouds",
				new DhApiVec3d(),
				boxList
		);
		newGroup.setBlockLight(15);
		newGroup.setSkyLight(15);
		newGroup.setSsaoEnabled(false);
		newGroup.setShading(cloudShading);
		newGroup.setPreRenderFunc(param -> preRender(newGroup));

		//register add/remove
		IDhApiCustomRenderRegister renderRegister = DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister();
		renderRegister.add(newGroup);
		if (group != null) {
			long oldId = group.getId();
			group.setPreRenderFunc(param -> renderRegister.remove(oldId));
		}
		group = newGroup;
	}

	private void tryBuildCellProxy(List<DhApiRenderableBox> group, int xOffset, int zOffset, int renderDistance, CloudGrid cloudGrid, Vec3 color, float alpha) {
		int x = xOffset + renderDistance + gridX - cloudGrid.centerX();  //transform to grids index
		int z = zOffset + renderDistance + gridZ - cloudGrid.centerZ();
		boolean[][][] grids = cloudGrid.grids();
		if (x < 0 || x >= grids.length || z < 0 || z >= grids.length)
			return;  //check bound

		boolean enableBottomDim = CONFIG.isEnableBottomDim();
		int thickness = 0;
		for (int y = grids.length - 1; y >= 0; y--) {
			if (y >= grids[x][z].length)
				continue;  //check y bound
			if (! grids[x][z][y]) {
				if (thickness > 0)
					thickness --;
				continue;  //jumping empty cell
			}

			Vec3 newColor = color.scale(enableBottomDim ?
					Mth.clamp((255 - thickness * 8) / 255f, 0f, 1f) :
					1F
			);
			tryBuildCell(group, x, y, z, renderDistance, grids, newColor, alpha);
			thickness++;
		}
	}

	private void tryBuildCell(List<DhApiRenderableBox> group, int x, int y, int z, int renderDistance, boolean[][][] grids, Vec3 color, float alpha) {
		//check neighbor and push it to next
		boolean borderTop    = !(y + 1 <  grids[x][z].length && grids[x][z][y + 1]);  //outOfBound || Not has neighbor -> built border
		boolean borderBottom = !(y - 1 >= 0                  && grids[x][z][y - 1]);
		boolean borderEast   = !(x + 1 <  grids.length       && grids[x + 1][z][y]);
		boolean borderWest   = !(x - 1 >= 0                  && grids[x - 1][z][y]);
		boolean borderSouth  = !(z + 1 <  grids.length       && grids[x][z + 1][y]);
		boolean borderNorth  = !(z - 1 >= 0                  && grids[x][z - 1][y]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0);

		x -= renderDistance + gridX - cloudGrid.centerX();  //transform to relative pos
		z -= renderDistance + gridZ - cloudGrid.centerZ();
		buildExtrudedCell(group, x, y, z, cellState, color, alpha);
	}

	private void buildExtrudedCell(List<DhApiRenderableBox> group, int x, int y, int z, int cellState, Vec3 color, float alpha) {
		if (hasBorderTop(cellState)    && y < gridY) addBox(group, x, y, z, Direction.UP,    0, color, alpha);
		if (hasBorderBottom(cellState) && y > gridY) addBox(group, x, y, z, Direction.DOWN,  0, color, alpha);
		if (hasBorderSouth(cellState)  && z < 0)     addBox(group, x, y, z, Direction.SOUTH, 0, color, alpha);
		if (hasBorderNorth(cellState)  && z > 0)     addBox(group, x, y, z, Direction.NORTH, 0, color, alpha);
		if (hasBorderEast(cellState)   && x < 0)     addBox(group, x, y, z, Direction.EAST,  0, color, alpha);
		if (hasBorderWest(cellState)   && x > 0)     addBox(group, x, y, z, Direction.WEST,  0, color, alpha);
	}

	private void addBox(List<DhApiRenderableBox> group, int x, int y, int z, Direction direction, int flag, Vec3 color, float alpha) {
		int x2 = x, y2 = y, z2 = z;  //DHBox is AABB box that needs 2 vector
		switch (direction) {
			case EAST  -> {x++;           x2++; y2++; z2++;}  //transform block pos to vector pos based on facing
			case WEST  -> {                     y2++; z2++;}
			case UP    -> {     y++;      x2++; y2++; z2++;}
			case DOWN  -> {               x2++;       z2++;}
			case SOUTH -> {          z++; x2++; y2++; z2++;}
			case NORTH -> {               x2++; y2++;      }
		}
		if (flag == 16) {  //if only has inner face, scale it to prevent DH culling.
			int scale = (int) (12F / CONFIG.getCloudBlockSize());
			switch (direction) {  //push out along facing axis, and scale on others.
				case EAST  -> {x += scale; y -= scale; z -= scale; x2 += scale; y2 += scale; z2 += scale;}
				case WEST  -> {x -= scale; y -= scale; z -= scale; x2 -= scale; y2 += scale; z2 += scale;}
				case UP    -> {x -= scale; y += scale; z -= scale; x2 += scale; y2 += scale; z2 += scale;}
				case DOWN  -> {x -= scale; y -= scale; z -= scale; x2 += scale; y2 -= scale; z2 += scale;}
				case SOUTH -> {x -= scale; y -= scale; z += scale; x2 += scale; y2 += scale; z2 += scale;}
				case NORTH -> {x -= scale; y -= scale; z -= scale; x2 += scale; y2 += scale; z2 -= scale;}
			}
		}
		group.add(new DhApiRenderableBox(
				new DhApiVec3d(
						x * cloudBlockWidth,
						y * cloudBlockHeight,
						z * cloudBlockWidth
				),
				new DhApiVec3d(
						x2 * cloudBlockWidth,
						y2 * cloudBlockHeight,
						z2 * cloudBlockWidth
				),
				new Color((float) color.x, (float) color.y, (float) color.z, alpha * 0.7F),
				EDhApiBlockMaterial.UNKNOWN
		));
	}

	//to calc RenderableBoxGroup pos and culling, etc...
	private void preRender(IDhApiRenderableBoxGroup group) {

		/* TODO: culling?
		    but we have only one group. considering is unnecessary..
		    if we want, try slicing cloudGrid into convertor, to get RenderableBoxGroup[] of sliced cloudGrid.
		 */

		//color
		// Change color here will override bottomDim color, we move it to groupBuilder.
		// We replace whole meshGroup in frequent anyway so why not set color in meshBuilding?
		//@see getGroup()

		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		//simply 'cameraPos - offset'...
		group.setOriginBlockPos(new DhApiVec3d(cameraPos.x() - xOffset, cloudHeight, cameraPos.z() - zOffset));
	}
}
