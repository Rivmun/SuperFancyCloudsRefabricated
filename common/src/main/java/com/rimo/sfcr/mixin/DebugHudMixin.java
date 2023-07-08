package com.rimo.sfcr.mixin;

import java.util.List;

import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.rimo.sfcr.SFCReMod;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	@Inject(method = "getLeftText", at = @At("RETURN"))
	public List<String> getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();

		// Add Debug Strings
		if (SFCReMod.COMMON_CONFIG.isEnableMod())
			list.add("[SFCR] Mesh Built: " +
					 SFCReMod.RENDERER.cullStateShown + " / " +
					(SFCReMod.RENDERER.cullStateSkipped + SFCReMod.RENDERER.cullStateShown) + " faces, " +
					 SFCReMod.RENDERER.cullStateSkipped + " Skipped.");

		return list;
	}
}
