package com.rimo.sfcr.mixin.particlerain;

import com.rimo.sfcr.Client;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.WeatherParticleManager;
import pigcart.particlerain.config.ConfigData;

import java.util.Iterator;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(WeatherParticleManager.class)
public abstract class WeatherParticleManagerMixin {
	@Unique private static double sfcr$x, sfcr$y, sfcr$z;
	@Unique private static boolean sfcr$shouldCancel;

	// rain
	@Redirect(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(DDD)Lnet/minecraft/util/math/BlockPos$Mutable;"
	))
	private static BlockPos.Mutable sfcr$catchRenderPos(BlockPos.Mutable instance, double x, double y, double z) {
		sfcr$x = x;
		sfcr$y = y;
		sfcr$z = z;
		return instance.set(x, y, z);
	}
	@Redirect(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Ljava/util/Iterator;next()Ljava/lang/Object;"  //inject into for(Object o : ArrayList<o> list)
	))
	private static Object sfcr$catchSkyFXParticleData(Iterator<ConfigData.ParticleData> instance) {
		ConfigData.ParticleData data = instance.next();
		sfcr$shouldCancel = data.weather == ConfigData.Weather.DURING_WEATHER && ! data.precipitation.contains(Biome.Precipitation.NONE);
		return data;
	}
	@Redirect(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Lpigcart/particlerain/config/ConfigData$SpawnPos;equals(Ljava/lang/Object;)Z"
	))
	private static boolean sfcr$shouldCancelSkyFX(ConfigData.SpawnPos instance, Object o) {
		if (sfcr$shouldCancel && CONFIG.isEnableRender() && CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return false;
		return instance.equals(o);
	}

	// still splash? do it again!
	@Inject(method = "tickBlockFX", at = @At("HEAD"))
	private static void sfcr$catchBlockPos(BlockPos.Mutable sourcePos, BlockState state, CallbackInfo ci) {
		sfcr$x = sourcePos.getX();
		sfcr$y = sourcePos.getY();
		sfcr$z = sourcePos.getZ();
	}
	@Redirect(method = "tickBlockFX", at = @At(
			value = "INVOKE",
			target = "Ljava/util/Iterator;next()Ljava/lang/Object;"
	))
	private static Object sfcr$catchBlockFXParticleData(Iterator<ConfigData.ParticleData> instance) {
		ConfigData.ParticleData data = instance.next();
		sfcr$shouldCancel = data.weather == ConfigData.Weather.DURING_WEATHER && ! data.precipitation.contains(Biome.Precipitation.NONE);
		return data;
	}
	@ModifyVariable(method = "tickBlockFX", at = @At("STORE"), name = "direction")
	private static Direction sfcr$shouldCancelBlockFX(Direction d) {
		if (sfcr$shouldCancel && CONFIG.isEnableRender() && CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return null;
		return d;
	}
}
