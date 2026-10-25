package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	/* - - NO CLOUD NO RAIN - - */

	//? if < 1.20.1 {
	/*@ModifyExpressionValue(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$disableRainPartices(Biome.Precipitation original, @Local BlockPos.MutableBlockPos pos) {
		return sfcr$getPrecipitation(original, pos);
	}

	@ModifyExpressionValue(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$disableRainSplash(Biome.Precipitation original, @Local(ordinal = 2) BlockPos pos) {
		return sfcr$getPrecipitation(original, pos);
	}
	*///? } else {
	@ModifyExpressionValue(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitationRain(Biome.Precipitation original, @Local BlockPos.MutableBlockPos pos) {
		return sfcr$getPrecipitation(original, pos);
	}

	@ModifyExpressionValue(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectGetPrecipitationSplash(Biome.Precipitation original, @Local(ordinal = 2) BlockPos pos) {
		return sfcr$getPrecipitation(original, pos);
	}
	//? }

	@Unique private Biome.Precipitation sfcr$getPrecipitation(Biome.Precipitation result, BlockPos pos) {
		if (result != Biome.Precipitation.NONE &&
				Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return result;
	}
}
