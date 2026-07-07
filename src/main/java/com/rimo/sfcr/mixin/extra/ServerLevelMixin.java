package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	//~ if < 1.21.1 'tickPrecipitation' -> 'tickChunk'
	@WrapOperation(method = "tickPrecipitation", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
	))
	private boolean sfcr$stopSnowLayerPileUp(Biome instance, LevelReader level, BlockPos pos, Operation<Boolean> original) {
		if (Common.isNoCloudCovered((Level) level, pos.getX(), pos.getY(), pos.getZ()))
			return false;
		return original.call(instance, level, pos);
	}

	//? if < 1.20.1 {
	/*@WrapOperation(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$stopCauldronPileUp(Biome instance, Operation<Biome.Precipitation> original, @Local(ordinal = 1) BlockPos pos) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance);
	}
	*///? } else {
	//~ if < 1.21.1 'tickPrecipitation' -> 'tickChunk'
	@ModifyVariable(method = "tickPrecipitation", at = @At("STORE"))
	private Biome.Precipitation sfcr$stopCauldronPileUp(Biome.Precipitation precipitation, @Local(ordinal = 1) BlockPos pos) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return precipitation;
	}
	//? }
}
