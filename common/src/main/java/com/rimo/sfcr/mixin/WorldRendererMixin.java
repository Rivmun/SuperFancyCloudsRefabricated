package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

	@Shadow private @Nullable ClientWorld world;
	@Shadow private int ticks;

	@Inject(method = "renderClouds*", at = @At("HEAD"), cancellable = true)
	public void renderSFC(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (world != null && Common.CONFIG.isEnableMod() && world.getDimension().hasSkyLight()) {
			Client.RENDERER.render(matrices, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, world, ticks);
			ci.cancel();
		}
	}
}
