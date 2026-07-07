package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if < 1.20.1
//import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin {
	@WrapOperation(method = "isRainingAt", at = @At(
			value = "INVOKE",
	//? if < 1.20.1 {
			/*target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitation()Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$hasRain(Biome instance, Operation<Biome.Precipitation> original, @Local(argsOnly = true) BlockPos pos) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance);
	*///? } else {
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$hasRain(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	//? }
	}
}
