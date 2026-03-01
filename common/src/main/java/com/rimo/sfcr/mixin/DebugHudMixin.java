package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.rimo.sfcr.Client.RENDERER;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	// Add Debug Strings
	@Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
	public void getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();
		if (Common.CONFIG.isEnableRender())
			list.add("[SFCR] build " + RENDERER.cullStateShown + "/" +
					(RENDERER.cullStateSkipped + RENDERER.cullStateShown) + " faces, cost " +
					RENDERER.debugRebuildTime + "ms, upload in " +
					RENDERER.debugUploadTime + "ms"
			);
		callback.setReturnValue(list);
	}
}
