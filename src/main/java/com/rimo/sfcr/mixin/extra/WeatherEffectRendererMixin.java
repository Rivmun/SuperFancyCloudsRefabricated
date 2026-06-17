package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if < 26.2 {
/*import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///? } else {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
//? }

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	// NO CLOUD NO RAIN
	//? if < 26.2 {
	/*@Inject(method = "getPrecipitationAt", at = @At(value = "HEAD"), cancellable = true)
	private void sfcr$getPrecipitationAt(Level level, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		if (Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ())) {
			cir.setReturnValue(Biome.Precipitation.NONE);
		}
	}
	*///? } else {
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectPrecipitation(ClientLevel instance, BlockPos blockPos, Operation<Biome.Precipitation> original) {
		if (Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, blockPos);
	}
	//? }
}
