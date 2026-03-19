package com.rimo.sfcr;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import static com.rimo.sfcr.Common.MOD_ID;

public class VersionUtil {
	public static ResourceLocation getId(String path) {
		//? if <= 1.20.1 {
		return new ResourceLocation(MOD_ID, path);
		 //? } else {
		/*return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
		*///? }
	}

	public static float getLastFrameDuration() {
		//? if <= 1.20.1 {
		return Minecraft.getInstance().getDeltaFrameTime();
		//? } else {
		/*return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		*///? }
	}
}
