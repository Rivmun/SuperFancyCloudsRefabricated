package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

	@Shadow private @Nullable ClientWorld world;
	@Shadow private int ticks;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V", at = @At("HEAD"), cancellable = true)
	public void sfcr$render(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (world != null && Common.CONFIG.isEnableRender() && world.getDimension().hasSkyLight()) {
			Client.RENDERER.render(matrices, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, world, ticks);
			ci.cancel();
		}
	}

	/* - - NO CLOUD NO RAIN - - */
	@Unique private double sfcr$x, sfcr$y, sfcr$z;

	@Redirect(method = "renderWeather", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(DDD)Lnet/minecraft/util/math/BlockPos$Mutable;"
	))
	private BlockPos.Mutable sfcr$getRenderWeatherPos(BlockPos.Mutable instance, double x, double y, double z) {
		sfcr$x = x;
		sfcr$y = y;
		sfcr$z = z;
		return instance.set(x, y, z);
	}

	@Redirect(method = "renderWeather", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;hasPrecipitation()Z"
	))
	private boolean sfcr$redirectHasPrecipitation(Biome biome) {
		if (! Client.RENDERER.isHasCloud(sfcr$x, sfcr$y, sfcr$z))
			return false;
		return biome.hasPrecipitation();
	}

	@Redirect(method = "tickRainSplashing", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitation(Biome instance, BlockPos pos) {
		if (! Client.RENDERER.isHasCloud(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return instance.getPrecipitation(pos);
	}

}
