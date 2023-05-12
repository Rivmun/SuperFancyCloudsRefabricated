package com.rimo.sfcr.mixin;

import java.util.List;

import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.rimo.sfcr.SFCReClient;
import com.rimo.sfcr.SFCReMain;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	@Inject(method = "getLeftText", at = @At("RETURN"))
	public List<String> getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();

		// Add Debug Strings
		if (SFCReMain.config.isEnableMod())
			list.add("[SFCR] Mesh Built: " + SFCReClient.RENDERER.cullStateShown + " / " + (SFCReClient.RENDERER.cullStateSkipped + SFCReClient.RENDERER.cullStateShown) + " faces, " + SFCReClient.RENDERER.cullStateSkipped + " Skipped.");
		SFCReClient.RENDERER.cullStateSkipped = 0;		// reset counter
		SFCReClient.RENDERER.cullStateShown = 0;

		return list;
	}
}
