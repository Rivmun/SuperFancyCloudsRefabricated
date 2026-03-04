package com.rimo.sfcr.mixin;

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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
	@Unique private double sfcr$x, sfcr$y, sfcr$z;

	@Redirect(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(DDD)Lnet/minecraft/core/BlockPos$MutableBlockPos;"
	))
	private BlockPos.MutableBlockPos sfcr$getRenderWeatherPos(BlockPos.MutableBlockPos instance, double x, double y, double z) {
		sfcr$x = x;
		sfcr$y = y;
		sfcr$z = z;
		return instance.set(x, y, z);
	}

	@Redirect(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;hasPrecipitation()Z"
	))
	private boolean sfcr$redirectHasPrecipitation(Biome instance) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return false;
		return instance.hasPrecipitation();
	}

	@Redirect(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitation(Biome instance, BlockPos pos) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return instance.getPrecipitationAt(pos);
	}

}
