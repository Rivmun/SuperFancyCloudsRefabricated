package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
//? if < 26.2 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
*///? } else {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//? }

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	// NO CLOUD NO RAIN
	//? if < 26.2 {
	/*// this method controlled both rain & splash on version < 26.2.
	@ModifyReturnValue(method = "getPrecipitationAt", at = @At(value = "RETURN"))
	private Biome.Precipitation sfcr$getPrecipitationAt(Biome.Precipitation original, @Local(argsOnly = true) BlockPos pos) {
	*///? } else {
	// rain only. splash was moved to ClientLevel on version > 26.1.
	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectPrecipitation(Biome.Precipitation original, @Local BlockPos.MutableBlockPos pos) {
	//? }
		return sfcr$get(original, pos);
	}

	@Unique private Biome.Precipitation sfcr$get(Biome.Precipitation original, BlockPos pos) {
		if (original != Biome.Precipitation.NONE &&
				Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original;
	}
}
