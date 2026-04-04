package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.core.BlockPos;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@WrapOperation(method = "makeDrippingWaterParticles", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"
	))
	private static boolean sfcr$disableDrippingWater(Level instance, BlockPos pos, Operation<Boolean> original) {
		if (Client.isNoCloudCovered(pos.getX(),  pos.getY(), pos.getZ()))
			return false;
		return original.call(instance, pos);
	}
}
