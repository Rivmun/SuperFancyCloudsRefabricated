package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.rimo.sfcr.Renderer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.render.CloudRenderer.ViewMode;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
	int oldX, oldY, oldZ;
	@Unique
	float oldCloudHeight;
	@Shadow
	public int instanceCount;
	@Shadow
	public abstract void scheduleTerrainUpdate();

	/*
		grabbing camera pos of grid
	 */
	@Inject(method = "renderClouds", at = @At("INVOKE"), cancellable = true)
	private void renderClouds(int color, CloudRenderMode mode, float cloudHeight, Vec3d cameraPos, float cloudPhase, CallbackInfo ci) {
		if (!CONFIG.isEnableMod())
			return;

		double d = cameraPos.x + (double)(cloudPhase * 0.030000001F);
		double e = cameraPos.z + 3.9600000381469727;
		float f = (float)(cameraPos.y - (double)cloudHeight);
		int x = MathHelper.floor(d / Renderer.CLOUD_BLOCK_WIDTH);
		int y = MathHelper.floor(f / Renderer.CLOUD_BLOCK_HEIGHT);
		int z = MathHelper.floor(e / Renderer.CLOUD_BLOCK_WIDTH);

		if (oldX != x || oldY != y || oldZ != z) {
			if (oldY != y && (
					f > RENDERER.getCloudHeight() ||
					f < RENDERER.getCloudHeight() + CONFIG.getCloudThickness() * Renderer.CLOUD_BLOCK_HEIGHT
			))
				scheduleTerrainUpdate();  //forcibly vanilla cloud to update when Y changed.
			oldX = x;
			oldY = y;
			oldZ = z;
			RENDERER.setGridPos(x, y, z);
		}

		if (cloudHeight != oldCloudHeight) {
			oldCloudHeight = cloudHeight;
			RENDERER.setCloudHeight(cloudHeight);
		}

		if (CONFIG.isEnableDHCompat())
			ci.cancel();  //cancel vanilla build & render, only get pos for DHCompat.
	}

	/*
		redirect renderPipeline
	 */
	@ModifyVariable(method = "renderClouds", at = @At("STORE"))
	private RenderPipeline setRenderPipeline(RenderPipeline pipeline) {
		if (!CONFIG.isEnableMod())
			return pipeline;
		if (!CONFIG.isEnableBottomDim())
			return Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS;
		return Renderer.SUPER_FANCY_CLOUDS;
	}

	/*
		Modifying instanceCount
		cuz we put more than 3 bits of vanilla to cloudFaces buffer.
	 */
	@Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/CloudRenderer;instanceCount:I", opcode = Opcodes.GETFIELD))
	private int getInstanceCount(CloudRenderer renderer) {
		if (!CONFIG.isEnableMod())
			return instanceCount;
		if (!CONFIG.isEnableBottomDim())
			return instanceCount * 3 / 4;
		return instanceCount * 3 / 5;
	}

	/*
		Redirect cells building is enough.
		original x/z is reference to pixel pos of clouds.png, which is unnecessary to our sample method.
		viewMode use to cull top/bottom face of clouds, but we make a multi layer clouds that needs culled by ourselves, this arg is useless.
	 */
	@Inject(method = "buildCloudCells", at = @At("INVOKE"), cancellable = true)
	private void buildCloudCells(ViewMode viewMode, ByteBuffer byteBuffer, int x, int z, boolean isFancy, int renderDistance, CallbackInfo ci) {
		if (!CONFIG.isEnableMod())
			return;
		RENDERER.buildCloudCells(byteBuffer, isFancy);
		ci.cancel();
	}

}
