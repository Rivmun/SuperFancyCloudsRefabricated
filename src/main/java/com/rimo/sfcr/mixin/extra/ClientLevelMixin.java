//? if > 26.1 {
package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

	// NO CLOUD NO RAIN, splash & sound.

	@WrapOperation(method = "tickWeatherEffects", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectPrecipitation(ClientLevel instance, BlockPos blockPos, Operation<Biome.Precipitation> original) {
		if (Common.CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, blockPos);
	}
}
//? }
