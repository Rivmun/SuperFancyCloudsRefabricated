package com.rimo.sfcr;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import static com.rimo.sfcr.Common.MOD_ID;

public class VersionUtil {
	public static Identifier getId(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static float getLastFrameDuration() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	public static void sendSystemMessage(CommandContext<CommandSourceStack> c, String message) {
		c.getSource().sendSystemMessage(Component.nullToEmpty(message));
	}

	public static void sendMessage(Player player, String message) {
		player.displayClientMessage(Component.nullToEmpty(message), false);
	}
}
