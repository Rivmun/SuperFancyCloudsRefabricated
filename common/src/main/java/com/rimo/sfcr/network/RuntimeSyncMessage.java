package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.core.Runtime;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;

import java.util.function.Supplier;

public class RuntimeSyncMessage {
	private final double time;
	private final int fullOffset;
	private final double partialOffset;

	public RuntimeSyncMessage(Runtime data) {
		synchronized (SFCReMod.RUNTIME) {
			this.time = data.time;
			this.fullOffset = data.fullOffset;
			this.partialOffset = data.partialOffset;
		}
	}

	public RuntimeSyncMessage(PacketByteBuf packet) {
		this.time = packet.readDouble();
		this.fullOffset = packet.readInt();
		this.partialOffset = packet.readDouble();
	}

	public static void encode(RuntimeSyncMessage message, PacketByteBuf packet) {
		packet.writeDouble(message.time);
		packet.writeInt(message.fullOffset);
		packet.writeDouble(message.partialOffset);
	}

	public static void receive(RuntimeSyncMessage message, Supplier<NetworkManager.PacketContext> contextSupplier) {
		if (!SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;
		if (contextSupplier.get().getEnv() != EnvType.CLIENT)
			return;

		contextSupplier.get().queue(() -> {
			synchronized (SFCReMod.RUNTIME) {
				try {
					SFCReMod.RUNTIME.time = message.time;
					SFCReMod.RUNTIME.fullOffset = message.fullOffset;
					SFCReMod.RUNTIME.partialOffset = message.partialOffset;
				} catch (Exception e) {
					SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
					SFCReMod.COMMON_CONFIG_HOLDER.save();
					contextSupplier.get().getPlayer().sendMessage(new TranslatableText("text.sfcr.command.sync_fail"), false);
					return;
				}
			}
			if (SFCReMod.COMMON_CONFIG.isEnableDebug())
				contextSupplier.get().getPlayer().sendMessage(new TranslatableText("text.sfcr.command.sync_succ"), false);
		});
	}
}
