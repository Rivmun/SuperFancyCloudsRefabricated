package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.rimo.sfcr.core.Renderer;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin {
	@Shadow
	public static RenderPipeline register(RenderPipeline renderPipeline) {return null;}

	@Inject(method = "<clinit>", at = @At(value = "RETURN"))
	private static void onInit(CallbackInfo ci) {
		register(Renderer.SUPER_FANCY_CLOUDS);
		register(Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS);
	}
}
