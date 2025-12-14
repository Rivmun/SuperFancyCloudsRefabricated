package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {

	@Final @Shadow private OptionInstance<Integer> renderDistance;
	@Final @Shadow private OptionInstance<Integer> cloudRange;

	/*
		grabbing renderDistance
	 */
	@Inject(method = "save", at = @At("RETURN"))
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (Client.RENDERER == null)
			return;
		Client.RENDERER.setRenderDistance(renderDistance.get(), cloudRange.get());
	}
}
