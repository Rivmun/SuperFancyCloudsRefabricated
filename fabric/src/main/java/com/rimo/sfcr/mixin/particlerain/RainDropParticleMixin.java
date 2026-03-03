package com.rimo.sfcr.mixin.particlerain;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.ParticleRainClient;
import pigcart.particlerain.particle.RainDropParticle;
import pigcart.particlerain.particle.WeatherParticle;

import java.util.Random;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(RainDropParticle.class)
public abstract class RainDropParticleMixin extends WeatherParticle {
	@Unique	private static final Random sfcr$random = new Random();
	@Unique	private static int sfcr$counter = 0;

	protected RainDropParticleMixin(ClientWorld level, double x, double y, double z, float red, float green, float blue, float gravity, SpriteProvider provider) {
		super(level, x, y, z, red, green, blue, gravity, provider);
	}

	// Particle Rain original sound is play by vanilla tickRainSplash invoke which is mixin by Particle Rain, we cannot modify it through normal method.
	// It's causing that raining sound always played even we disabled rain particles.
	// So we inject to Particle Rain 's getSound function to disable that sound.
	// To play it correctly, we made a proxy that invoked by particle itself when it's dropped instead of vanilla tick.
	@Inject(method = "tick", at = @At("TAIL"))
	private void sfcr$playRainSound(CallbackInfo ci) {
		if (! CONFIG.isEnableRender() || ! CONFIG.isEnableParticleRainCompat())
			return;
		if (this.onGround && sfcr$random.nextInt(3) < sfcr$counter ++) {
			sfcr$counter = 0;
			BlockPos pos = new BlockPos(this.x, this.y, this.z);
			if (this.y > MinecraftClient.getInstance().gameRenderer.getCamera().getPos().getY()) {
				SoundEvent sound = sfcr$getSoundEventProxy(this.world.getBiome(pos), true);
				this.world.playSound(pos, sound, SoundCategory.WEATHER, 0.1F, 0.5F, false);
			} else {
				SoundEvent sound = sfcr$getSoundEventProxy(this.world.getBiome(pos), false);
				this.world.playSound(pos, sound, SoundCategory.WEATHER, 0.2F, 1F, false);
			}
		}
	}

	// original getSound is disabled by ourselves, so we must make a proxy.
	@Unique
	private SoundEvent sfcr$getSoundEventProxy(RegistryEntry<Biome> biome, boolean isAbove) {
		if (biome.value().getTemperature() >= 0.15F) {
			return isAbove ? SoundEvents.WEATHER_RAIN_ABOVE : SoundEvents.WEATHER_RAIN;
		} else {
			return isAbove ? ParticleRainClient.WEATHER_SNOW_ABOVE : ParticleRainClient.WEATHER_SNOW;
		}
	}
}
