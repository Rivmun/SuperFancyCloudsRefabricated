package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMod;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

	@Shadow
	public SimpleOption<Integer> viewDistance;

	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "write", at = @At("RETURN"), cancellable = true)
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (SFCReMod.COMMON_CONFIG.isCloudRenderDistanceFitToView()) {
			SFCReMod.COMMON_CONFIG.setCloudRenderDistance(viewDistance.getValue() * 12);
			SFCReMod.COMMON_CONFIG.save();
		}
		ci.cancel();
	}
}
