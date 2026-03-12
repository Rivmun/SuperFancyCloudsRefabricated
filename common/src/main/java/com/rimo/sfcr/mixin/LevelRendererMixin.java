package com.rimo.sfcr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rimo.sfcr.Client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
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
	@Shadow private int ticks;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void sfcr$render(PoseStack poseStack, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (level != null && RENDERER != null && CONFIG.isEnableRender() && level.dimensionType().hasSkyLight()) {
			RENDERER.render(poseStack, matrix4f, matrix4f2, tickDelta, cameraX, cameraY, cameraZ, level, ticks);
			ci.cancel();
		}
	}

	/* - - NO CLOUD NO RAIN - - */
	@WrapOperation(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$getPrecipitationAtRain(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

	@WrapOperation(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$redirectGetPrecipitation(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

}
