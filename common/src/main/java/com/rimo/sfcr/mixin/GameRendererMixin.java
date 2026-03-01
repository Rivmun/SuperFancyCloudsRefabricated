package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	//Prevent cloud be culled
	@Inject(method = "getFarPlaneDistance", at = @At("RETURN"), cancellable = true)
	private void sfcr$extend_distance(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(cir.getReturnValue() * (Common.CONFIG.getAutoFogMaxDistance() + 2));
	}
}
