package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Common;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
	@SuppressWarnings("ConstantConditions")
	@Inject(method = "hasRain", at = @At("RETURN"), cancellable = true)
	private void sfcr$hasRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (Common.isNoCloudCovered((World) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			cir.setReturnValue(false);
	}
}
