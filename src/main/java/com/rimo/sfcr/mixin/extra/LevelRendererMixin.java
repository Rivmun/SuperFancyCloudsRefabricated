package com.rimo.sfcr.mixin.extra;

import com.rimo.sfcr.Client;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if < 1.20 {
/*import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
*///? } else {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? }

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	/* - - NO CLOUD NO RAIN - - */

	//? if > 1.20 {
	@WrapOperation(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$redirectGetPrecipitationRain(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}

	@WrapOperation(method = "tickRain", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$redirectGetPrecipitationSplash(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Client.isNoCloudCovered(pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}
	//? } else {
	/*@Unique
	private double sfcr$x, sfcr$y, sfcr$z;

	// DO NOT USE ModifyArgs on forge, see https://github.com/SpongePowered/Mixin/issues/584 (I say f)
	@Inject(method = "renderSnowAndRain", at = @At(
			value = "INVOKE",
			//~ if ! 1.16.5 'III' -> 'DDD'
			target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(DDD)Lnet/minecraft/core/BlockPos$MutableBlockPos;",
			shift = At.Shift.AFTER
	), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void sfcr$getRenderWeatherPos(CallbackInfo ci, @Local BlockPos.MutableBlockPos pos) {
		sfcr$x = pos.getX();
		sfcr$y = pos.getY();
		sfcr$z = pos.getZ();
	}

	@ModifyVariable(method = "renderSnowAndRain", at = @At("STORE"))
	private Biome sfcr$modifyBiomeRain(Biome biome) {
		if (Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return BuiltinRegistries.BIOME.get(Biomes.SAVANNA);
		return biome;
	}

	@ModifyVariable(method = "tickRain", at = @At("STORE"), ordinal = 2)  //DO NOT use name that cannot found
	private BlockPos sfcr$catchSplashingPos(BlockPos pos) {
		sfcr$x = pos.getX();
		sfcr$y = pos.getY();
		sfcr$z = pos.getZ();
		return pos;
	}

	@ModifyVariable(method = "tickRain", at = @At("STORE"))
	private Biome sfcr$modifyBiomeSplash(Biome biome) {
		if (Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return BuiltinRegistries.BIOME.get(Biomes.SAVANNA);
		return biome;
	}
	*///? }
}
