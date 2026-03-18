package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
	@SuppressWarnings("ConstantConditions")
	@Inject(method = "isRainingAt", at = @At("RETURN"), cancellable = true)
	private void sfcr$hasRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			cir.setReturnValue(false);
	}
}
