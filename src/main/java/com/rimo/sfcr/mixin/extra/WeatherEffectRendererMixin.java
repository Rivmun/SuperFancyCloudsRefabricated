package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Client;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//? }

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	// NO CLOUD NO RAIN
	//? if < 26.2 {
	/*@Inject(method = "getPrecipitationAt", at = @At(value = "HEAD"), cancellable = true)
	private void sfcr$getPrecipitationAt(Level level, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ())) {
			cir.setReturnValue(Biome.Precipitation.NONE);
		}
	}
	*///? } else {
	@Unique private int sfcr$x, sfcr$y, sfcr$z;
	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private BlockPos sfcr$getPos(BlockPos pos) {
		sfcr$x = pos.getX();
		sfcr$y = pos.getY();
		sfcr$z = pos.getZ();
		return pos;
	}
	@ModifyVariable(method = "extractRenderState", at = @At(value = "STORE"))
	private Biome.Precipitation sfcr$redirectPrecipitation(Biome.Precipitation precipitation) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return Biome.Precipitation.NONE;
		return precipitation;
	}
	//? }
}
