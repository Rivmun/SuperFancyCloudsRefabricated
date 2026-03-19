//? if fabric && < 1.20 {
package com.rimo.sfcr.mixin.particlerain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;
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

	protected RainDropParticleMixin(ClientLevel level, double x, double y, double z, float red, float green, float blue, float gravity, SpriteSet provider) {
		super(level, x, y, z, red, green, blue, gravity, provider);
	}

	// Particle Rain original sound is play by vanilla tickRainSplash invoke which is mixin by Particle Rain, we cannot modify it through normal method.
	// It's causing that raining sound always played even we disabled rain particles.
	// So we inject to Particle Rain 's getSound function to disable that sound.
	// To play it correctly, we made a proxy that invoked by particle itself when it's dropped instead of vanilla tick.
	@Inject(method = "tick", at = @At("TAIL"))
	private void sfcr$playRainSound(CallbackInfo ci) {
		if (! CONFIG.isEnableParticleRainCompat())
			return;
		if (this.onGround && sfcr$random.nextInt(3) < sfcr$counter ++) {
			sfcr$counter = 0;
			BlockPos pos = new BlockPos(this.x, this.y, this.z);
			if (this.y > Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y()) {
				SoundEvent sound = sfcr$getSoundEventProxy(this.level.getBiome(pos), true);
				this.level.playLocalSound(pos, sound, SoundSource.WEATHER, 0.1F, 0.5F, false);
			} else {
				SoundEvent sound = sfcr$getSoundEventProxy(this.level.getBiome(pos), false);
				this.level.playLocalSound(pos, sound, SoundSource.WEATHER, 0.2F, 1F, false);
			}
		}
	}

	// original getSound is disabled by ourselves, so we must make a proxy.
	@Unique
	private SoundEvent sfcr$getSoundEventProxy(Holder<Biome> biome, boolean isAbove) {
		if (biome.value().getBaseTemperature() >= 0.15F) {
			return isAbove ? SoundEvents.WEATHER_RAIN_ABOVE : SoundEvents.WEATHER_RAIN;
		} else {
			return isAbove ? ParticleRainClient.WEATHER_SNOW_ABOVE : ParticleRainClient.WEATHER_SNOW;
		}
	}
}
//? }
