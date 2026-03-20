//? if fabric && = 1.19.2 {
/*package com.rimo.sfcr.mixin.particlerain;

import com.rimo.sfcr.Client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pigcart.particlerain.WeatherParticleSpawner;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(WeatherParticleSpawner.class)
public abstract class WeatherParticleSpawnerMixin {
	@Inject(method = "spawnParticle", at = @At("HEAD"), cancellable = true)
	private static void sfcr$shouldRainSpawn(ClientLevel level, Holder<Biome> biome, double x, double y, double z, CallbackInfo ci) {
		if (CONFIG.isEnableParticleRainCompat() && biome.value().getPrecipitation() != Biome.Precipitation.NONE && Client.isNoCloudCovered(x, y, z))
			ci.cancel();
	}

	// it's levelRendererMixin always play sound when raining, we can't modify it through normally way.
	// But fortunately the sound it getting by itself, we try to disable it.
	@Inject(method = "getBiomeSound", at = @At("HEAD"), cancellable = true)
	private static void sfcr$disableSound(Holder<Biome> biome, boolean above, CallbackInfoReturnable<SoundEvent> cir) {
		if (CONFIG.isEnableParticleRainCompat() && biome.value().getPrecipitation() != Biome.Precipitation.NONE)
			cir.setReturnValue(null);
	}
}
*///? }
