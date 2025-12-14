package com.rimo.sfcr.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow
	private ClientLevel level;

	/*
		inject custom cloud color
	 */
	@ModifyVariable(method = "addCloudsPass", at = @At(value = "HEAD"), argsOnly = true)
	private int modifyColor(int color) {
		if (! CONFIG.isEnableMod())
			return color;

		long t = level.getDayTime() % 24000L;
		int r = (CONFIG.getCloudColor() & 0xFF0000) >> 16;
		int g = (CONFIG.getCloudColor() & 0x00FF00) >> 8;
		int b = (CONFIG.getCloudColor() & 0x0000FF);

		if (CONFIG.isEnableDuskBlush()) {
			// Color changed by time...
			if (t > 22500 || t < 500) {        //Dawn, clamp value to [0, 2000]
				t = t > 22500 ? t - 22500 : t + 1500;
				r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
				g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
				b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
			} else if (t < 13500 && t > 11500) {        //Dusk, reverse order
				t -= 11500;
				r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
				g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
				b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
			}
		}
		return ARGB.multiply(color, ARGB.color(r, g, b));
	}
}
