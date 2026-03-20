package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import net.minecraft.client.CloudStatus;
//? if > 1.19 {
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
//? }
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {

	//? if > 1.19 {
	@Final @Shadow private OptionInstance<Integer> renderDistance;
	//? } else
	//@Shadow public int renderDistance;

	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "save", at = @At("RETURN"))
	private void sfcr$updateCloudRenderDistance(CallbackInfo ci) {
		if (Common.CONFIG.isCloudRenderDistanceFitToView()) {
			//~ if < 1.19 'renderDistance.get()' -> 'renderDistance'
			Common.CONFIG.setCloudRenderDistance(renderDistance.get() * 12);
			Common.CONFIG.save();
		}
	}

	/*
		always fancy, to prevent DH disabled vanilla cloudRenderer
	 */
	@Inject(method = "getCloudsType", at = @At("RETURN"), cancellable = true)
	private void sfcr$getCloudType(CallbackInfoReturnable<CloudStatus> cir) {
		if (Common.CONFIG.isEnableRender())
			cir.setReturnValue(CloudStatus.FANCY);
	}
}
