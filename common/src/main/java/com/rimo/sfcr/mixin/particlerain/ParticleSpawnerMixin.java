package com.rimo.sfcr.mixin.particlerain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Client;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pigcart.particlerain.ParticleSpawner;
import pigcart.particlerain.config.ParticleData;

import java.util.Iterator;

import static com.rimo.sfcr.Common.CONFIG;

@Mixin(ParticleSpawner.class)
public abstract class ParticleSpawnerMixin {
	@Unique private static double sfcr$x, sfcr$y, sfcr$z;
	@Unique private static boolean sfcr$shouldCancel;

	// rain
	@ModifyArgs(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(DDD)Lnet/minecraft/util/math/BlockPos$Mutable;"
	))
	private static void sfcr$catchRenderPos(Args args) {
		sfcr$x = args.get(0);
		sfcr$y = args.get(1);
		sfcr$z = args.get(2);
	}
	@WrapOperation(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Ljava/util/Iterator;next()Ljava/lang/Object;"  //inject into for(Object o : ArrayList<o> list)
	))
	private static Object sfcr$catchSkyFXParticleData(Iterator<ParticleData> instance, Operation<Object> original) {
		ParticleData data = (ParticleData) original.call(instance);
		sfcr$shouldCancel = data.weather == ParticleData.Weather.DURING_WEATHER && ! data.precipitation.contains(Biome.Precipitation.NONE);
		return data;
	}
	@WrapOperation(method = "tickSkyFX", at = @At(
			value = "INVOKE",
			target = "Lpigcart/particlerain/config/ParticleData$SpawnPos;equals(Ljava/lang/Object;)Z"
	))
	private static boolean sfcr$shouldCancelSkyFX(ParticleData.SpawnPos instance, Object o, Operation<Boolean> original) {
		if (sfcr$shouldCancel && CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return false;
		return original.call(instance, o);
	}

	// still splash? do it again!
	@Inject(method = "tickBlockFX", at = @At("HEAD"))
	private static void sfcr$catchBlockPos(BlockPos.Mutable sourcePos, BlockState state, CallbackInfo ci) {
		sfcr$x = sourcePos.getX();
		sfcr$y = sourcePos.getY();
		sfcr$z = sourcePos.getZ();
	}
	@WrapOperation(method = "tickBlockFX", at = @At(
			value = "INVOKE",
			target = "Ljava/util/Iterator;next()Ljava/lang/Object;"
	))
	private static Object sfcr$catchBlockFXParticleData(Iterator<ParticleData> instance, Operation<Object> original) {
		ParticleData data = (ParticleData) original.call(instance);
		sfcr$shouldCancel = data.weather == ParticleData.Weather.DURING_WEATHER && ! data.precipitation.contains(Biome.Precipitation.NONE);
		return data;
	}
	@ModifyVariable(method = "tickBlockFX", at = @At("STORE"), name = "direction")
	private static Direction sfcr$shouldCancelBlockFX(Direction d) {
		if (sfcr$shouldCancel && CONFIG.isEnableParticleRainCompat() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return null;
		return d;
	}
}
