package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.option.GameOptions;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
	
	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "Lnet/minecraft/client/option/GameOptions;setServerViewDistance(I)V", at = @At("RETURN"), cancellable = true)
	private void updateCloudRenderDistance(CallbackInfo ci) {
		SFCReMod.RENDERER.UpdateRenderData(SFCReMod.CONFIG.getConfig());
		ci.cancel();
	}
}
