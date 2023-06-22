package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMod;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	
	//Prevent cloud be culled
	@Inject(method = "method_32796", at = @At("RETURN"), cancellable = true)
	private void extend_distance(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(cir.getReturnValue() * (SFCReMod.COMMON_CONFIG.getAutoFogMaxDistance() + 2));
	}
}