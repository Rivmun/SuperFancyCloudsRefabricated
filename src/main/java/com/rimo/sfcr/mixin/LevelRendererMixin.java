package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if > 1.20 {
/*import org.joml.Matrix4f;
 *///? } else {
import com.mojang.math.Matrix4f;
//? }

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Client.RENDERER;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow private @Nullable ClientLevel level;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 1)
	//? if < 1.21.1 {
	public void sfcr$render(PoseStack poseStack, Matrix4f matrix4f, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (level != null && CONFIG.isEnableRender()) {
			RENDERER.render(poseStack, matrix4f, tickDelta, cameraX, cameraY, cameraZ, level);
			ci.cancel();
		}
	}
	//? } else {
	/*public void sfcr$render(PoseStack poseStack, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (level != null && CONFIG.isEnableRender()) {
			RENDERER.render(poseStack, matrix4f, matrix4f2, tickDelta, cameraX, cameraY, cameraZ, level);
			ci.cancel();
		}
	}
	*///? }

}
