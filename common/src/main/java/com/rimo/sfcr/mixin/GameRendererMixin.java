package com.rimo.sfcr.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	//Prevent cloud be culled
	@Inject(method = "getFarPlaneDistance", at = @At("RETURN"), cancellable = true)
	private void sfcr$extend_distance(CallbackInfoReturnable<Float> cir) {
		if (CONFIG.isEnableRender())
			cir.setReturnValue(cir.getReturnValue() * (CONFIG.getAutoFogMaxDistance() + 2));
	}
}
