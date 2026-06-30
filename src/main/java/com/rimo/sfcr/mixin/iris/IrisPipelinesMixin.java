package com.rimo.sfcr.mixin.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.rimo.sfcr.core.Renderer;
import it.unimi.dsi.fastutil.Function;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisPipelines.class)
public abstract class IrisPipelinesMixin {
	@Shadow private static void assignToMain(RenderPipeline pipeline, Function<IrisRenderingPipeline, ShaderKey> o) {}

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void sfcr$registerCustomPipelines(CallbackInfo ci) {
		assignToMain(Renderer.SUPER_FANCY_CLOUDS, (p) -> ShaderKey.CLOUDS);
		assignToMain(Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS, (p) -> ShaderKey.CLOUDS);
	}
}
