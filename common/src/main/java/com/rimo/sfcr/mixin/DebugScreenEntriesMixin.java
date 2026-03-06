package com.rimo.sfcr.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.MOD_ID;

@Mixin(DebugScreenEntries.class)
public class DebugScreenEntriesMixin {
	@Shadow
	private static Identifier register(Identifier resourceLocation, DebugScreenEntry debugScreenEntry) {
		return null;
	}

	// register debug entry
	@Inject(method = "<clinit>()V", at = @At("RETURN"))
	private static void onInit(CallbackInfo ci) {
		register(Identifier.fromNamespaceAndPath(MOD_ID, "debug"), (displayer, level, levelChunk, levelChunk2) ->
				displayer.addLine("[SFCR] encode " + RENDERER.debugBuiltCounter + " face(s), " + RENDERER.debugCullCounter + " cell(s) skipped.")
		);
	}
}
