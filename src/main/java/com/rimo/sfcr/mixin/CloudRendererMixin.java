package com.rimo.sfcr.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if > 26.2 {
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.oit.OitStage;
import com.rimo.sfcr.core.Renderer;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//? }

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.CONFIG;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
	@Shadow @Final private MappableRingBuffer ubo;
	@Shadow private MappableRingBuffer utb;
	//? if > 26.2
	@Shadow private int quadCount;
	@Shadow	private static int getSizeForCloudDistance(int i) {return 0;}

	//~ if > 26.2 'render' -> 'prepare(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V'
	@Inject(method = "prepare(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V", at = @At("HEAD"), cancellable = true, require = 1)
	//~ if = 1.21.11 'int range, Vec3 cameraPos' -> 'Vec3 cameraPos'
	private void sfcr$render(int color, CloudStatus mode, float cloudHeight, int range, Vec3 cameraPos, long l, float cloudPhase, CallbackInfo ci) {
		Level level = Minecraft.getInstance().level;
		if (level == null || ! CONFIG.isEnableRender())
			return;

		int cloudRange = CONFIG.getCloudRenderDistance() < 32 ?
				//~ if = 1.21.11 'range' -> 'Minecraft.getInstance().options.cloudRange().get()'
				range * 16 :
				CONFIG.getCloudRenderDistance() * 16;
		int renderRange = Mth.ceil((float)cloudRange / CONFIG.getCloudBlockSize());

		if (! CONFIG.isEnableDHCompat()) {
			int bufferSize = getSizeForCloudDistance(renderRange);
			if (utb == null || utb.currentBuffer().size() != (long) bufferSize) {
				if (utb != null) {
					utb.close();
				}
				utb = new MappableRingBuffer(() -> "Cloud UTB", 258, bufferSize);
			}
		}

		RENDERER.render(color, cloudHeight, cameraPos, cloudPhase, ubo, utb, renderRange, level);
		//? if > 26.2
		quadCount = RENDERER.getQuadCount();
		ci.cancel();
	}

	//? if > 26.2 {
	@ModifyArg(method = "render(Lnet/minecraft/client/CloudStatus;Lcom/mojang/renderpearl/api/commands/RenderPass;)V", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/CloudRenderer;render(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)V"
	), require = 1)
	private RenderPipeline sfcr$modifyClassicRenderPipeline(RenderPipeline pipeline) {
		if (! CONFIG.isEnableRender())
			return pipeline;
		return CONFIG.isEnableBottomDim() ? Renderer.SUPER_FANCY_CLOUDS : Renderer.SUPER_FANCY_CLOUDS_NOTHICKNESS;
	}

	@ModifyArg(method = "renderOit", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/CloudRenderer;render(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)V"
	), require = 1)
	private RenderPipeline sfcr$modifyOitRenderPipeline(RenderPipeline renderPipeline, @Local(argsOnly = true) OitStage stage) {
		if (! CONFIG.isEnableRender())
			return renderPipeline;
		return (CONFIG.isEnableBottomDim() ? Renderer.OIT_CLOUDS : Renderer.OIT_CLOUD_NOTHICKNESS).getPipeline(stage);
	}
	//? }
}
