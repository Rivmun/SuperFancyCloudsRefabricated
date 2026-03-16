package com.rimo.sfcr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.block.LeavesBlock;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@WrapOperation(method = "randomDisplayTick", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/util/ParticleUtil;spawnParticle(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/particle/ParticleEffect;)V"
	))
	private void sfcr$disableDrippingWater(World world, BlockPos pos, Random random, ParticleEffect effect, Operation<Void> original) {
		if (Common.CONFIG.isEnableCloudRain() && effect == ParticleTypes.DRIPPING_WATER && Client.isNoCloudCovered(pos.getX(),  pos.getY(), pos.getZ()))
			return;
		original.call(world, pos, random, effect);
	}
}
