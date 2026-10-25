package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	//~ if < 1.21.1 'tickPrecipitation' -> 'tickChunk'
	@ModifyExpressionValue(method = "tickPrecipitation", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
	))
	private boolean sfcr$stopSnowLayerPileUp(boolean original, @Local(ordinal = 0) BlockPos pos) {
		if (original && sfcr$get((Level) (Object) this, pos))
			return false;
		return original;
	}

	//? if < 1.20.1 {
	/*@ModifyExpressionValue(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$stopCauldronPileUp(Biome.Precipitation original, @Local(ordinal = 1) BlockPos pos) {
		if (original != Biome.Precipitation.NONE && sfcr$get((Level) (Object) this, pos))
			return Biome.Precipitation.NONE;
		return original;
	}
	*///? } else {
	//~ if < 1.21.1 'tickPrecipitation' -> 'tickChunk'
	@ModifyVariable(method = "tickPrecipitation", at = @At("STORE"))
	private Biome.Precipitation sfcr$stopCauldronPileUp(Biome.Precipitation precipitation, @Local(ordinal = 1) BlockPos pos) {
		if (precipitation != Biome.Precipitation.NONE && sfcr$get((Level) (Object) this, pos))
			return Biome.Precipitation.NONE;
		return precipitation;
	}
	//? }

	@Unique private boolean sfcr$get(Level level, BlockPos pos) {
		return Common.CONFIG.isCloudRainLogically() && Common.isNoCloudCovered(level, pos.getX(), pos.getY(), pos.getZ());
	}
}
