package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	// Add Debug Strings
	@Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
	public void getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();
		if (Common.CONFIG.isEnableMod())
			list.add("[SFCR] mesh: " +
					 Client.RENDERER.cullStateShown + " built, " +
					 Client.RENDERER.cullStateSkipped + " skipped, " +
					(Client.RENDERER.cullStateSkipped + Client.RENDERER.cullStateShown) + " total.");
		callback.setReturnValue(list);
	}
}
