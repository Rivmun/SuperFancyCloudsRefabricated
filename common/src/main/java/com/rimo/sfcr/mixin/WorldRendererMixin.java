package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Client.RENDERER;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

	@Shadow private @Nullable ClientWorld world;
	@Shadow private int ticks;

	// MAIN FUNCTION CALL...
	// Do not use wildcard here that will make mixin inject failure when test without dev env!!
	@Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FDDD)V", at = @At("HEAD"), cancellable = true)
	public void sfcr$render(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (world != null && CONFIG.isEnableRender() && world.getDimension().hasSkyLight()) {
			RENDERER.render(matrices, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, world, ticks);
			ci.cancel();
		}
	}

	/* - - NO CLOUD NO RAIN - - */
	@Unique private double sfcr$x, sfcr$y, sfcr$z;

	@ModifyArgs(method = "renderWeather", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(DDD)Lnet/minecraft/util/math/BlockPos$Mutable;"
	))
	private void sfcr$getRenderWeatherPos(Args args) {
		sfcr$x = args.get(0);
		sfcr$y = args.get(1);
		sfcr$z = args.get(2);
	}

	@ModifyVariable(method = "renderWeather", at = @At("STORE"))
	private Biome sfcr$modifyBiomeRain(Biome biome) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return BuiltinRegistries.BIOME.get(BiomeKeys.SAVANNA);
		return biome;
	}

	@ModifyVariable(method = "tickRainSplashing", at = @At("STORE"), ordinal = 2)  //DO NOT use name that cannot found
	private BlockPos sfcr$catchSplashingPos(BlockPos pos) {
		sfcr$x = pos.getX();
		sfcr$y = pos.getY();
		sfcr$z = pos.getZ();
		return pos;
	}

	@ModifyVariable(method = "tickRainSplashing", at = @At("STORE"))
	private Biome sfcr$modifyBiomeSplash(Biome biome) {
		if (CONFIG.isEnableCloudRain() && Client.isNoCloudCovered(sfcr$x, sfcr$y, sfcr$z))
			return BuiltinRegistries.BIOME.get(BiomeKeys.SAVANNA);
		return biome;
	}

}
