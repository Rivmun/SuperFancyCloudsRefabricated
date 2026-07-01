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
			list.add(Common.debugString);
			Level level = Minecraft.getInstance().level;
			if (level != null) {
				Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
				boolean debugIsCloud = Common.isCloud(level, pos.x, pos.y, pos.z);
				boolean debugIsCloudClient = Client.isCloud(pos.x, pos.y, pos.z);
				list.add("[SFCR] isCloud:" + debugIsCloud + ", isCloudClient:" + debugIsCloudClient);
			}
		}
		callback.setReturnValue(list);
	}
}
