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

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.CONFIG;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
	@Shadow @Final private MappableRingBuffer ubo;
	@Shadow private MappableRingBuffer utb;
	@Shadow	private static int getSizeForCloudDistance(int i) {return 0;}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
	private void sfcr$render(int color, CloudStatus mode, float cloudHeight, Vec3 cameraPos, long l, float cloudPhase, CallbackInfo ci) {
		Level level = Minecraft.getInstance().level;
		if (level == null || ! CONFIG.isEnableRender())
			return;

		int cloudRange = CONFIG.getCloudRenderDistance() < 32 ?
				Minecraft.getInstance().options.cloudRange().get() * 16 :
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
		ci.cancel();
	}
}
