package com.rimo.sfcr.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rimo.sfcr.Common;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
//? if > 1.20 {
import org.joml.Matrix4f;
//? } else if ! 1.16.5 {
/*import com.mojang.math.Matrix4f;
*///? }

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.rimo.sfcr.Common.*;

/* Some note:
	DH official cloud renderer parse a picture to a renderableBoxGroup, then copy it 11*11 times as a cloud matrix,
	Its culling method cut matrix group by group, not by a single box
	We convert our cloudGrid to renderableBoxGroup and add it to DH's renderPass
	Also, thread-ify it.
	@see com.seibel.distanthorizons.core.render.renderer.generic.CloudRenderHandler
 */
public class RendererDHCompat extends Renderer {
	private final DhApiRenderableBoxGroupShading cloudShading = createCloudShading();
	private List<IDhApiRenderableBoxGroup> groupList = new ArrayList<>();

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
	//? if = 1.16.5 {
	/*public void render(PoseStack poseStack, float tickDelta, double cameraX, double cameraY, double cameraZ,
	*///? } else if < 1.21.1 {
	/*public void render(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ,
	*///? } else {
	public void render(PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ,
	//? }
	                   ClientLevel level) {
		if (!DhApi.Delayed.configs.graphics().renderingEnabled().getValue())
			return;  //save battery if DH render was disabled.
		//? if = 1.16.5 {
		/*super.render(poseStack, tickDelta, cameraX, cameraY, cameraZ, level);
		*///? } else if < 1.21.1 {
		/*super.render(poseStack, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, level);
		*///? } else {
		super.render(poseStack, projectionMatrix, matrix4f2, tickDelta, cameraX, cameraY, cameraZ, level);
		//? }
	}

	@Override
	//? if = 1.16.5 {
	/*public void _render(PoseStack poseStack, Vec3 cloudColor, float xOffsetInGrid, double cloudY, float zOffsetInGrid) {
	 *///? } else if < 1.21.1 {
	/*public void _render(PoseStack poseStack, Matrix4f projectionMatrix, Vec3 cloudColor, float xOffsetInGrid, double cloudY, float zOffsetInGrid) {
	*///? } else {
	public void _render(PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f matrix4f2, Vec3 cloudColor, float xOffsetInGrid, double cloudY, float zOffsetInGrid) {
	//? }
		if (! Minecraft.getInstance().isPaused() && isMarkedForRebuild()) {
			rebuildTimer = 0;
			rebuildCloudMesh(cloudColor);
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (DhApi.Delayed.worldProxy.worldLoaded()) {
			for (IDhApiRenderableBoxGroup group : groupList)
				DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister().remove(group.getId());
			groupList.clear();
		}
	}

	// turns quad vertex into AABB box for DH renderer
	private List<DhApiRenderableBox> getGroup(CloudData data, Vec3 color, float alpha) {
		List<DhApiRenderableBox> group = new ArrayList<>();
		for(CloudData.CompressedFace face : data.meshData) {
			Vec3 newColor = color.scale(CONFIG.isEnableBottomDim() ?
					Mth.clamp((255 - face.getThickness() * 8) / 255f, 0f, 1f) :
					1F
			);
			int[][] vertex = face.getVertexList();
			group.add(new DhApiRenderableBox(
					new DhApiVec3d(
							(vertex[0][0] - 0.33F) * cloudBlockWidth,
							vertex[0][1] * cloudBlockHeight,
							(vertex[0][2] - 0.33F) * cloudBlockWidth
					),
					new DhApiVec3d(
							(vertex[2][0] - 0.33F) * cloudBlockWidth,
							vertex[2][1] * cloudBlockHeight,
							(vertex[2][2] - 0.33F) * cloudBlockWidth
					),
					new Color((float) newColor.x, (float) newColor.y, (float) newColor.z, alpha * 0.8F),
					EDhApiBlockMaterial.UNKNOWN
			));
		}
		return group;
	}

	// RenderableBoxGroup build and replace.
	private void rebuildCloudMesh(Vec3 cloudColor) {
		int customColor = CONFIG.getCloudColor();  //apply custom color
		Vec3 color = cloudColor.multiply(getBlushColorByTime(Minecraft.getInstance().level.getDayTime()))
				.multiply(((customColor & 0xFF0000) >> 16) / 255F, ((customColor & 0xFF00) >> 8) / 255F, (customColor & 0xFF) / 255F);
		float alpha = (customColor >>> 24) / 255F;

		cullStateSkipped = 0;
		cullStateShown = 0;
		int refreshSpeed = CONFIG.getNormalRefreshSpeed().getValue();
		List<IDhApiRenderableBoxGroup> newGroupList = new ArrayList<>();
		for(CloudData data : cloudDataGroup) {
			switch (data.getDataType()) {  // Smooth Change: Alpha changed by cloud type and lifetime
				case TRANS_IN: alpha *= 1F - data.getLifeTime() / refreshSpeed * 5F; break;
				case TRANS_OUT: alpha *= data.getLifeTime() / refreshSpeed * 5F; break;
				default: break;
			}
			IDhApiRenderableBoxGroup newGroup = DhApi.Delayed.customRenderObjectFactory.createRelativePositionedGroup(
					Common.MOD_ID + ":clouds",
					new DhApiVec3d(),
					getGroup(data, color, alpha)
			);
			newGroup.setBlockLight(15);
			newGroup.setSkyLight(15);
			newGroup.setSsaoEnabled(false);
			newGroup.setShading(cloudShading);
			newGroup.setPreRenderFunc(renderParam -> preRender(newGroup));
			newGroupList.add(newGroup);
			cullStateShown += newGroup.size();
		}

		IDhApiCustomRenderRegister renderRegister = DhApi.Delayed.worldProxy.getSinglePlayerLevel().getRenderRegister();
		for(IDhApiRenderableBoxGroup group : groupList)
			// since DH 3.0 it will get flicker if we directly remove, so...
			group.setPreRenderFunc(renderParam -> renderRegister.remove(group.getId()));
		for(IDhApiRenderableBoxGroup group : newGroupList)
			renderRegister.add(group);
		groupList = newGroupList;
	}

	// calc RenderableBoxGroup pos and culling, etc..
	private void preRender(IDhApiRenderableBoxGroup group) {

		/* TODO: culling?
		    but we have only one group. considering is unnecessary..
		    if we want, try slicing cloudGrid into convertor, to get RenderableBoxGroup[] of sliced cloudGrid.
		 */

		//color
		// Change color here will override bottomDim color, we move it to groupBuilder.
		// We replace whole meshGroup in frequent anyway so why not set color in meshBuilding?
		//@see getGroup()

		//pos
		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		double cloudX = cameraPos.x() - xOffset;
		double cloudZ = cameraPos.z() - zOffset;
		double cloudY = getCloudHeight() + 0.33F;
		/* Suddenly I realized that there should be simply "cameraPos - offset" ...
		 * W T F to my brain (╯‵□′)╯︵┻━┻ */
		group.setOriginBlockPos(new DhApiVec3d(cloudX, cloudY, cloudZ));
	}
}
