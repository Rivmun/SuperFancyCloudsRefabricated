package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin {
	@ModifyReturnValue(method = "precipitationAt", at = @At("RETURN"))
	private Biome.Precipitation sfcr$hasRain(Biome.Precipitation original, @Local(argsOnly = true) BlockPos pos) {
		if (original != Biome.Precipitation.NONE &&
				Common.CONFIG.isCloudRainLogically() && Common.isNoCloudCovered((Level) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original;
	}
}
