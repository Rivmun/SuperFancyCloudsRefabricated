//? if > 1.20 {
package com.rimo.sfcr.mixin.particlerain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.rimo.sfcr.Client;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import pigcart.particlerain.ParticleSpawner;
import pigcart.particlerain.config.ParticleData;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(ParticleSpawner.class)
public abstract class ParticleSpawnerMixin {
	@Final @Shadow private static BlockPos.MutableBlockPos pos;

	// Rain
	@WrapOperation(method = "tickSkyFX", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
	private static Collection<ParticleData> sfcr$filterSkyFXParticles(Map<String, ParticleData> instance, Operation<Collection<ParticleData>> original) {
		if (CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return instance.values().stream().filter(data ->
					data.weather != ParticleData.Weather.DURING_WEATHER || data.precipitation.contains(Biome.Precipitation.NONE)
			).collect(Collectors.toList());
		return original.call(instance);
	}

	// still splash? do it again!
	@WrapOperation(method = "tickBlockFX", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
	private static Collection<ParticleData> sfcr$filterBlockFXParticles(Map<String, ParticleData> instance, Operation<Collection<ParticleData>> original,
	                                                                    @Local(argsOnly = true) BlockPos.MutableBlockPos sourcePos) {
		if (CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()))
			return instance.values().stream().filter(data ->
					data.weather != ParticleData.Weather.DURING_WEATHER || data.precipitation.contains(Biome.Precipitation.NONE)
			).collect(Collectors.toList());
		return original.call(instance);
	}
}
//? }
