package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	// Add Debug Strings
	@Inject(method = "getGameInformation", at = @At("RETURN"), cancellable = true)
	public void sfcr$getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();
		if (Common.CONFIG.isEnableRender()) {
			list.add(Client.RENDERER.getDebugString());
			list.add(Common.DATA.getDebugString());
			list.add(Common.getDebugString());
			list.add(Client.getDebugString());
		}
		callback.setReturnValue(list);
	}
}
