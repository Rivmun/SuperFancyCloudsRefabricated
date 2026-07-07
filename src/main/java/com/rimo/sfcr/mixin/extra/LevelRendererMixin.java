package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if < 1.20 {
/*import com.llamalad7.mixinextras.sugar.Local;
*///? }

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	/* - - NO CLOUD NO RAIN - - */

	//? if > 1.20 {
	@WrapOperation(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$redirectGetPrecipitationRain(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

	@WrapOperation(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$redirectGetPrecipitationSplash(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}
	//? } else {
	/*@WrapOperation(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$disableRainPartices(Biome instance, Operation<Biome.Precipitation> original, @Local BlockPos.MutableBlockPos pos) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance);
	}

	@WrapOperation(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$disableRainSplash(Biome instance, Operation<Biome.Precipitation> original, @Local(ordinal = 2) BlockPos pos) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance);
	}
	*///? }
}
