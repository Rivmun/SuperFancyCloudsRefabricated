package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import net.irisshaders.iris.config.IrisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisConfig.class)
public abstract class IrisConfigMixin {
	@Inject(method = "setShadersEnabled", at = @At("HEAD"))
	public void setShaderEnabled(boolean enabled, CallbackInfo ci) {
		Client.isIrisLoadedShader = enabled;
		if (enabled) {
			Common.CONFIG.setEnableMod(false);
			if (Minecraft.getInstance().player != null)
				Minecraft.getInstance().player.displayClientMessage(Component.translatable("text.sfcr.shaderpackEnabled"), false);
		}
	}
}
