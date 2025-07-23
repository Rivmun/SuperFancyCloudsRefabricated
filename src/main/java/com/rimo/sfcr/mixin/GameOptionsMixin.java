package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

	@Final @Shadow private SimpleOption<Integer> viewDistance;
	@Final @Shadow private SimpleOption<Integer> cloudRenderDistance;

	/*
		refresh RENDERER setting
	 */
	@Inject(method = "write", at = @At("RETURN"))
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (Client.RENDERER == null)
			return;
		Client.RENDERER.setRenderDistance(viewDistance.getValue(), cloudRenderDistance.getValue());
	}
}
