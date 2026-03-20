package com.rimo.sfcr;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static com.rimo.sfcr.Common.MOD_ID;

public class VersionUtil {
	public static ResourceLocation getId(String path) {
		//? if <= 1.20.1 {
		/*return new ResourceLocation(MOD_ID, path);
		 *///? } else {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
		//? }
	}

	public static float getLastFrameDuration() {
		//? if <= 1.20.1 {
		/*return Minecraft.getInstance().getDeltaFrameTime();
		*///? } else {
		return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		//? }
	}

	public static void sendSystemMessage(CommandContext<CommandSourceStack> c, String message) {
		//? if < 1.19 {
		/*c.getSource().sendSuccess(Component.nullToEmpty(message), false);
		*///? } else {
		c.getSource().sendSystemMessage(Component.nullToEmpty(message));
		//? }
	}

	public static void sendMessage(Player player, String message) {
		//? if < 1.19 {
		/*player.displayClientMessage(Component.nullToEmpty(message), false);
		*///? } else {
		player.sendSystemMessage(Component.nullToEmpty(message));
		//? }
	}
}
