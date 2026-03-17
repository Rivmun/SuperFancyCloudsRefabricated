package com.rimo.sfcr.mixin.extra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rimo.sfcr.Common;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
	@WrapOperation(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;canSetSnow(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z"
	))
	private boolean sfcr$stopSnowLayerPileUp(Biome instance, WorldView world, BlockPos pos, Operation<Boolean> original) {
		if (Common.isNoCloudCovered((World) world, pos.getX(), pos.getY(), pos.getZ()))
			return false;
		return original.call(instance, world, pos);
	}

	@SuppressWarnings("ConstantConditions")
	@WrapOperation(method = "tickChunk", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"
	))
	private Biome.Precipitation sfcr$stopCauldronLayerPileUp(Biome instance, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Common.isNoCloudCovered((World) (Object) this, pos.getX(), pos.getY(), pos.getZ()))
			return Biome.Precipitation.NONE;
		return original.call(instance, pos);
	}
}
