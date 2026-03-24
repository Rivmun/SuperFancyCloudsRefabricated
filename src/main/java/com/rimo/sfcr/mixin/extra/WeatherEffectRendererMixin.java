package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Client;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	// NO CLOUD NO RAIN
	@Inject(method = "getPrecipitationAt", at = @At(value = "HEAD"), cancellable = true)
	private void sfcr$getPrecipitationAt(Level level, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ())) {
			cir.setReturnValue(Biome.Precipitation.NONE);
		}
	}
}
