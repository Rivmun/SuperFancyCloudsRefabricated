package com.rimo.sfcr.mixin;

//~ if > 26.2 '.blaze3d' -> '.renderpearl.api'
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.rimo.sfcr.core.Renderer;
import net.minecraft.client.renderer.RenderPipelines;
//? if > 26.2
import net.minecraft.client.renderer.oit.OitPipelineSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin {
	@Shadow public static RenderPipeline register(RenderPipeline renderPipeline) {return null;}
	//? if > 26.2
	@Shadow public static OitPipelineSet register(OitPipelineSet oitPipelineSet) {return null;}

	@Inject(method = "<clinit>", at = @At(value = "RETURN"), require = 1)
	private static void sfcr$registerCustomRenderPipelines(CallbackInfo ci) {
		register(Renderer.SUPER_FANCY_CLOUDS);
		register(Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS);
		//? if > 26.2 {
		register(Renderer.OIT_CLOUDS);
		register(Renderer.OIT_CLOUD_NOTHICKNESS);
		//? }
	}
}
