package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Client.RENDERER;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow private @Nullable ClientLevel level;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 1)
	public void sfcr$render(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (level != null && CONFIG.isEnableRender()) {
			RENDERER.render(poseStack, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, level);
			ci.cancel();
		}
	}

}
