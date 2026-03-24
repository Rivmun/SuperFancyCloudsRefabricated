package com.rimo.sfcr.mixin;

import com.rimo.sfcr.Common;
import com.rimo.sfcr.VersionUtil;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.DATA;

@Mixin(DebugScreenEntries.class)
public class DebugScreenEntriesMixin {
	@Unique	private static final Identifier sfcr$ID = VersionUtil.getId("debug");
	@Shadow public static Identifier register(Identifier resourceLocation, DebugScreenEntry debugScreenEntry) {return null;}

	@Inject(method = "<clinit>()V", at = @At("RETURN"))
	private static void sfcr$registerDebugEntry(CallbackInfo ci) {
		register(sfcr$ID, (displayer, level, levelChunk, levelChunk2) -> {
			List<String> list = new ArrayList<>();
			list.add(RENDERER.getDebugString());
			list.add(DATA.getDebugString());
			list.add(Common.debugString);
			displayer.addToGroup(sfcr$ID, list);
		});
	}
}
