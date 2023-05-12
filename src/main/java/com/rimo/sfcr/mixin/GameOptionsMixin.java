package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

	@Shadow
	private SimpleOption<Integer> viewDistance;
	
	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "write", at = @At("RETURN"), cancellable = true)
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (SFCReMain.config.isCloudRenderDistanceFitToView()) {
			SFCReMain.config.setCloudRenderDistance(viewDistance.getValue() * 12);
			SFCReMain.CONFIGHOLDER.save();
		}
		ci.cancel();
	}
}
