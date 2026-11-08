package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SereneSeasons @Override(1.16.5) / cancel at HEAD (all version) the isRainingAt method that leads normal mixin
 *  annotation cannot work without crash.<br>
 * We must use {@link ModifyReturnValue} that no relies on function signature, it should be works...
 */
@Mixin(Level.class)
public abstract class LevelMixin {
	@ModifyReturnValue(method = "isRainingAt", at = @At("RETURN"))
	private boolean sfcr$isRainingAt(boolean original, @Local(argsOnly = true) BlockPos pos) {
		if (original)
			return !Common.CONFIG.isCloudRainLogically() || !Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ());
		return original;
	}
}
