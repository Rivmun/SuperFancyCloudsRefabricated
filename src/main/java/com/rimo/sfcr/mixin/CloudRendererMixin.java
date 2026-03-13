package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.rimo.sfcr.core.Renderer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.CONFIG;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {

	@Unique
	int sfcr$oldX, sfcr$oldY, sfcr$oldZ, sfcr$rebuildTick = 0;
	@Unique
	float sfcr$oldCloudHeight;
	@Shadow
	private int quadCount;
	@Shadow
	public abstract void markForRebuild();
	@Shadow
	private CloudRenderer.TextureData texture;

	/*
		grabbing camera pos of grid
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void render(int color, CloudStatus cloudStatus, float bottomY, int range, Vec3 cameraPosition, long gameTime, float partialTicks, CallbackInfo ci) {
		if (!CONFIG.isEnableRender())
			return;

		//keep xOffset compute as same as vanilla
		float o = (float)(gameTime % ((long) this.texture.width() * 400L)) + partialTicks;
		double d = cameraPosition.x + (double)(o * 0.030000001F);
		double e = cameraPosition.z + 3.96F;

		float f = (float)(cameraPosition.y - (double)bottomY);
		int x = Mth.floor(d / RENDERER.getCloudBlockWidth());
		int y = Mth.floor(f / RENDERER.getCloudBlockHeight());
		int z = Mth.floor(e / RENDERER.getCloudBlockWidth());

		if (sfcr$oldX != x || sfcr$oldY != y || sfcr$oldZ != z) {
			if (sfcr$oldY != y && (
					f > RENDERER.getCloudHeight() ||
					f < RENDERER.getCloudHeight() + CONFIG.getCloudThickness() * RENDERER.getCloudBlockHeight()))
				markForRebuild();  //forcibly vanilla cloud to update when player in cloud and Y changed.
			sfcr$oldX = x;
			sfcr$oldY = y;
			sfcr$oldZ = z;
			RENDERER.setGridPos(x, y, z);
		}

		if (bottomY != sfcr$oldCloudHeight) {
			sfcr$oldCloudHeight = bottomY;
			RENDERER.setCloudHeight(bottomY);
		}

		RENDERER.counting(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false) / 20.0);
		if (CONFIG.getEnableViewCulling() && ++ sfcr$rebuildTick > CONFIG.getRebuildInterval()) {
			sfcr$rebuildTick = 0;
			markForRebuild();  //manually update
		}
	}

	/*
		redirect renderPipeline
	 */
	@ModifyVariable(method = "render", at = @At("STORE"))
	private RenderPipeline setRenderPipeline(RenderPipeline pipeline) {
		if (!CONFIG.isEnableRender())
			return pipeline;
		if (!CONFIG.isEnableBottomDim())
			return Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS;
		return Renderer.SUPER_FANCY_CLOUDS;
	}

	/*
		Modifying instanceCount/quadCount
		cuz we put more than 3 bits of vanilla to cloudFaces buffer.
	 */
	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/CloudRenderer;quadCount:I", opcode = Opcodes.GETFIELD))
	private int getQuadCount(CloudRenderer renderer) {
		if (!CONFIG.isEnableRender())
			return quadCount;
		if (!CONFIG.isEnableBottomDim())
			return quadCount * 3 / 4;
		return quadCount * 3 / 5;
	}

	/*
		Redirect cells building is enough.
		original x/z is reference to pixel pos of clouds.png, which is unnecessary to our sample method.
		relativeCameraPos use to cull top/bottom face of clouds, but we make a multi layer clouds that needs culled by ourselves, this arg is useless.
	 */
	@Inject(method = "buildMesh", at = @At("HEAD"), cancellable = true)
	private void buildMesh(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer byteBuffer, int x, int z, boolean isFancy, int cloudRange, CallbackInfo ci) {
		if (!CONFIG.isEnableRender())
			return;
		RENDERER.buildMesh(byteBuffer);
		ci.cancel();
	}

}
