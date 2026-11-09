package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@ModifyExpressionValue(method = "makeDrippingWaterParticles", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
	private static boolean sfcr$disableDrippingWater(boolean original, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
		if (original &&
				Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(),  pos.getY(), pos.getZ()))
			return false;
		return original;
	}
}
