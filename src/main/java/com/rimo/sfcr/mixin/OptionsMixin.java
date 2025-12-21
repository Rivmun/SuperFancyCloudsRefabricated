package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {

	@Final @Shadow private OptionInstance<Integer> renderDistance;
	@Final @Shadow private OptionInstance<Integer> cloudRange;

	/*
		grabbing renderDistance
	 */
	@Inject(method = "save", at = @At("RETURN"))
	private void vanillaConfigListener(CallbackInfo ci) {
		if (Client.RENDERER == null)
			return;
		Client.RENDERER.setRenderDistance(renderDistance.get(), cloudRange.get());
	}

	/*
		always fancy
	 */
	@Inject(method = "getCloudsType", at = @At("RETURN"), cancellable = true)
	private void getCloudsType(CallbackInfoReturnable<CloudStatus> cir) {
		if (Common.CONFIG.isEnableMod())
			cir.setReturnValue(CloudStatus.FANCY);
	}

}
