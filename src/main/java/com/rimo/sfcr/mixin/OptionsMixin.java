package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
	// always fancy, to prevent DH disabled vanilla cloudRenderer
	//~ if = 1.21.11 'getCloudStatus' -> 'getCloudsType'
	@Inject(method = "getCloudStatus", at = @At("RETURN"), cancellable = true)
	private void sfcr$getCloudType(CallbackInfoReturnable<CloudStatus> cir) {
		if (Common.CONFIG.isEnableRender())
			cir.setReturnValue(CloudStatus.FANCY);
	}
}
