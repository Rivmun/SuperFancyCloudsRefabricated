package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if > 1.20 {
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
//? }

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@WrapOperation(method = "animateTick", at = @At(
			value = "INVOKE",
	//? if > 1.20 {
			target = "Lnet/minecraft/util/ParticleUtils;spawnParticleBelow(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/particles/ParticleOptions;)V"
	))
	private void sfcr$disableDrippingWater(Level level, BlockPos pos, RandomSource random, ParticleOptions particle, Operation<Void> original) {
		if (particle == ParticleTypes.DRIPPING_WATER && Client.isNoCloudCovered(pos.getX(),  pos.getY(), pos.getZ()))
			return;
		original.call(level, pos, random, particle);
	}
//? } else {
			/*target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
	))
	public void sfcr$disableDrippingWater(Level level, ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz, Operation<Void> original) {
		if (particle == ParticleTypes.DRIPPING_WATER && Client.isNoCloudCovered(x, y, z))
			return;
		original.call(level, particle, x, y, z, vx, vy, vz);
	}
*///? }
}
