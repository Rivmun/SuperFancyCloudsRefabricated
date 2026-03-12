package com.rimo.sfcr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Client.RENDERER;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

	@Shadow private @Nullable ClientWorld world;
	@Shadow private int ticks;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V", at = @At("HEAD"), cancellable = true)
	public void sfcr$render(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (world != null && CONFIG.isEnableRender() && world.getDimension().hasSkyLight()) {
			RENDERER.render(matrices, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, world, ticks);
			ci.cancel();
		}
	}

	/* - - NO CLOUD NO RAIN - - */

	@WrapOperation(method = "renderWeather", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitationRain(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

	@WrapOperation(method = "tickRainSplashing", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitationSplash(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

}
