package com.rimo.sfcr.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	/*
		inject custom cloud color
	 */
	@Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
	public void getCloudsColor(float tickProgress, CallbackInfoReturnable<Integer> cir) {
		if (! CONFIG.isEnableMod() || ! CONFIG.isEnableDuskBlush())
			return;

		int color = cir.getReturnValue();
		long t = ((ClientWorld)(Object)this).getTimeOfDay() % 24000L;
		int r = (CONFIG.getCloudColor() & 0xFF0000) >> 16;
		int g = (CONFIG.getCloudColor() & 0x00FF00) >> 8;
		int b = (CONFIG.getCloudColor() & 0x0000FF);

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, clamp value to [0, 2000]
			t = t > 22500 ? t - 22500 : t + 1500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		}
		cir.setReturnValue(ColorHelper.mix(color, ColorHelper.getArgb(r, g, b)));
	}
}
