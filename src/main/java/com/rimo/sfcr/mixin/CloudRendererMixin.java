package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Renderer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.render.CloudRenderer.ViewMode;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {

	@Unique
	int oldX, oldY, oldZ;
	@Unique
	float oldCloudHeight;

	/*
		grabbing camera pos of grid
	 */
	@Inject(method = "renderClouds", at = @At("INVOKE"))
	private void renderClouds(int color, CloudRenderMode mode, float cloudHeight, Vec3d cameraPos, float cloudPhase, CallbackInfo ci) {
		double d = cameraPos.x + (double)(cloudPhase * 0.030000001F);
		double e = cameraPos.z + 3.9600000381469727;
		float f = (float)(cameraPos.y - (double)cloudHeight);
		int x = MathHelper.floor(d / 12.0);
		int y = MathHelper.floor(f / 4.0);
		int z = MathHelper.floor(e / 12.0);

		if (oldX != x || oldY != y || oldZ != z) {
			oldX = x;
			oldY = y;
			oldZ = z;
			Client.RENDERER.setGridPos(x, y, z);
		}

		if (cloudHeight != oldCloudHeight) {
			oldCloudHeight = cloudHeight;
			Client.RENDERER.setCloudHeight(cloudHeight);
		}
	}

	/*
		Modifying instanceCount
		cuz we put 4 bits to CloudFaces buffer instead of 3.
	 */
	@Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/CloudRenderer;instanceCount:I", opcode = Opcodes.GETFIELD))
	private int getInstanceCount(CloudRenderer renderer) {
		return renderer.instanceCount * 3 / 4;
	}

	/*
		Redirect cells building is enough.
		original x/z is reference to pixel pos of clouds.png, which is unnecessary to our sample method.
		viewMode use to cull top/bottom face of clouds, but we make a multi layer clouds that needs culled by ourselves, this arg is useless.
	 */
	@Inject(method = "buildCloudCells", at = @At("HEAD"), cancellable = true)
	private void buildCloudCells(ViewMode viewMode, ByteBuffer byteBuffer, int x, int z, boolean isFancy, int renderDistance, CallbackInfo ci) {
		Client.RENDERER.buildCloudCells(byteBuffer, isFancy, renderDistance);
		ci.cancel();
	}

}
