package com.rimo.sfcr.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(targets = "net.minecraft.world.attribute.EnvironmentAttributeProbe$ValueProbe")
public abstract class ValueProbeMixin {
	@Inject(method = "get", at = @At("TAIL"), cancellable = true)
	private <Value> void sfcr$get(EnvironmentAttribute<Value> environmentAttribute, float f, CallbackInfoReturnable<Object> cir) {
		Level level = Minecraft.getInstance().level;
		if (! CONFIG.isEnableRender() || level == null)
			return;

		// inject custom cloud color
		if (EnvironmentAttributes.CLOUD_COLOR.equals(environmentAttribute)) {
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
			cir.setReturnValue(ARGB.multiply((Integer) cir.getReturnValue(), ARGB.color(r, g, b)));
		}

		// inject custom cloud height
		if (EnvironmentAttributes.CLOUD_HEIGHT.equals(environmentAttribute) && CONFIG.getCloudHeight() != 0) {
			cir.setReturnValue((Float) cir.getReturnValue() + CONFIG.getCloudHeight());
		}
	}
}
