package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.rimo.sfcr.Renderer;
import net.minecraft.client.CloudStatus;
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
	int oldX, oldY, oldZ;
	@Unique
	float oldCloudHeight;
	@Shadow
	private int quadCount;
	@Shadow
	public abstract void markForRebuild();
	@Shadow
	private CloudRenderer.TextureData texture;

	/*
		grabbing camera pos of grid
	 */
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void render(int color, CloudStatus mode, float cloudHeight, Vec3 cameraPos, long l, float cloudPhase, CallbackInfo ci) {
		if (!CONFIG.isEnableMod())
			return;

		//keep xOffset compute as same as vanilla
		float o = (float)(l % ((long) this.texture.width() * 400L)) + cloudPhase;
		double d = cameraPos.x + (double)(o * 0.030000001F);
		double e = cameraPos.z + 3.96F;

		float f = (float)(cameraPos.y - (double)cloudHeight);
		int x = Mth.floor(d / Renderer.CLOUD_BLOCK_WIDTH);
		int y = Mth.floor(f / Renderer.CLOUD_BLOCK_HEIGHT);
		int z = Mth.floor(e / Renderer.CLOUD_BLOCK_WIDTH);

		if (oldX != x || oldY != y || oldZ != z) {
			if (oldY != y && (
					f > RENDERER.getCloudHeight() ||
					f < RENDERER.getCloudHeight() + CONFIG.getCloudThickness() * Renderer.CLOUD_BLOCK_HEIGHT))
				markForRebuild();  //forcibly vanilla cloud to update when player in cloud and Y changed.
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
	@ModifyVariable(method = "render", at = @At("STORE"))
	private RenderPipeline setRenderPipeline(RenderPipeline pipeline) {
		if (!CONFIG.isEnableMod())
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
		if (!CONFIG.isEnableMod())
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
		if (!CONFIG.isEnableMod())
			return;
		RENDERER.buildMesh(byteBuffer, isFancy);
		ci.cancel();
	}

}
