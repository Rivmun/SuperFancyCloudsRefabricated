package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

	@Final @Shadow private SimpleOption<Integer> viewDistance;

	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "write", at = @At("RETURN"))
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (Common.CONFIG.isCloudRenderDistanceFitToView()) {
			Common.CONFIG.setCloudRenderDistance(viewDistance.getValue() * 12);
			Common.CONFIG.save();
		}
	}

	/*
		always fancy, to prevent DH disabled vanilla cloudRenderer
	 */
	@Inject(method = "getCloudRenderModeValue", at = @At("RETURN"), cancellable = true)
	private void getCloudType(CallbackInfoReturnable<CloudRenderMode> cir) {
		if (Common.CONFIG.isEnableMod())
			cir.setReturnValue(CloudRenderMode.FANCY);
	}
}
