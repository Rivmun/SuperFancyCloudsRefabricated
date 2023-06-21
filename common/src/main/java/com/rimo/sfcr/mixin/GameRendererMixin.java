package com.rimo.sfcr.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	
	//Prevent cloud be culled
//	@Inject(method = "method_32796", at = @At("RETURN"), cancellable = true)
//	private void extend_distance(CallbackInfoReturnable<Float> cir) {
//		cir.setReturnValue(cir.getReturnValue() * (SFCReMain.config.getAutoFogMaxDistance() + 2));
//	}
}