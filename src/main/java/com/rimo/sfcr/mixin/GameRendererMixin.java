package com.rimo.sfcr.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if ! 1.16.5 {
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? } else
//import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	//Prevent cloud be culled
	//? if ! 1.16.5 {
	@Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
	private void sfcr$extendDepthFar(CallbackInfoReturnable<Float> cir) {
		if (CONFIG.isEnableRender())
			cir.setReturnValue(cir.getReturnValue() * (CONFIG.getAutoFogMaxDistance() + 2));
	}
	//? } else {
	/*@ModifyArg(method = "getProjectionMatrix", at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/math/Matrix4f;perspective(DFFF)Lcom/mojang/math/Matrix4f;"
	), index = 3)
	private float sfcr$extendDepthFar(float dist) {
		if (CONFIG.isEnableRender())
			return dist * (CONFIG.getAutoFogMaxDistance() * 2);
		return dist;
	}
	*///? }
}
