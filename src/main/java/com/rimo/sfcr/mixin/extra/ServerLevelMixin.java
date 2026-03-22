package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if < 1.20 {
/*import org.spongepowered.asm.mixin.Unique;
//? if > 1.17
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///? }

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	//? if < 1.21.1 {
	/*@WrapOperation(method = "tickChunk", at = @At(
	*///? } else
	@WrapOperation(method = "tickPrecipitation", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
	))
	private boolean sfcr$stopSnowLayerPileUp(Biome instance, LevelReader level, BlockPos pos, Operation<Boolean> original) {
		if (Common.isNoCloudCovered((Level) level, pos.getX(), pos.getY(), pos.getZ()))
			return false;
		return original.call(instance, level, pos);
	}

	//? if = 1.16.5 {
	/*@Unique private double sfcr$x, sfcr$y, sfcr$z;
	@ModifyVariable(method = "tickChunk", at = @At("STORE"), ordinal = 1)
	private BlockPos sfcr$getBlockPos(BlockPos original) {
		sfcr$x = original.getX();
		sfcr$y = original.getY();
		sfcr$z = original.getZ();
		return original;
	}
	@WrapOperation(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$stopCauldronLayerPileUp(Biome instance, Operation<Biome.Precipitation> original) {
		if (Common.isNoCloudCovered((Level) (Object) this, sfcr$x, sfcr$y, sfcr$z))
			return Biome.Precipitation.NONE;
		return original.call(instance);
	}
	*///? } else if < 1.20 {
	/*@Unique private double sfcr$x, sfcr$y, sfcr$z;
	@ModifyArg(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
			ordinal = 1
	))
	private BlockPos sfcr$getBlockState(BlockPos pos) {
		sfcr$x = pos.getX();
		sfcr$y = pos.getY();
		sfcr$z = pos.getZ();
		return pos;
	}
	@ModifyVariable(method = "tickChunk", at = @At("STORE"))
	private Biome.Precipitation sfcr$stopCauldronLayerPileUp(Biome.Precipitation original) {
		if (Common.isNoCloudCovered((Level) (Object) this, sfcr$x, sfcr$y, sfcr$z))
			return Biome.Precipitation.NONE;
		return original;
	}
	*///? } else if < 1.21 {
	/*@SuppressWarnings("ConstantConditions")
	//? if < 1.21 {
	/^@WrapOperation(method = "tickChunk", at = @At(
	^///? } else
	@WrapOperation(method = "tickPrecipitation", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$stopCauldronLayerPileUp(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}
	*///? }
}
