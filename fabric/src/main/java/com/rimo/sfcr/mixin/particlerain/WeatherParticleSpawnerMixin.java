package com.rimo.sfcr.mixin.particlerain;

import com.rimo.sfcr.Client;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import pigcart.particlerain.WeatherParticleSpawner;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(WeatherParticleSpawner.class)
public abstract class WeatherParticleSpawnerMixin {
	@Unique private double sfcr$x, sfcr$y, sfcr$z;

	@ModifyVariable(method = "update", at = @At("STORE"), ordinal = 0)
	private BlockPos sfcr$getBlockPos(BlockPos value) {
		sfcr$x = value.getX();
		sfcr$y = value.getY();
		sfcr$z = value.getZ();
		return value;
	}

	@Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getPrecipitation()Lnet/minecraft/world/biome/Biome$Precipitation;"))
	private Biome.Precipitation sfcr$redirectPrecipitation(Biome instance) {
		if (CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return Biome.Precipitation.NONE;
		return instance.getPrecipitation();
	}
}
