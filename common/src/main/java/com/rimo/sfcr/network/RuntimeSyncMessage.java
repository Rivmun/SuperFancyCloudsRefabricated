package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.core.Runtime;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;

import java.util.function.Supplier;

public class RuntimeSyncMessage {
	private final double time;
	private final int fullOffset;
	private final double partialOffset;

	public RuntimeSyncMessage(Runtime data) {
		this.time = data.time;
		this.fullOffset = data.fullOffset;
		this.partialOffset = data.partialOffset;
	}

	public RuntimeSyncMessage(PacketByteBuf packet) {
		this.time = packet.readDouble();
		this.fullOffset = packet.readInt();
		this.partialOffset = packet.readDouble();
	}

	public void encode(PacketByteBuf packet) {
		packet.writeDouble(this.time);
		packet.writeInt(this.fullOffset);
		packet.writeDouble(this.partialOffset);
	}

	public void receive(Supplier<NetworkManager.PacketContext> contextSupplier) {
		if (!SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;

		synchronized (SFCReMod.RUNTIME) {
			try {
				SFCReMod.RUNTIME.time = this.time;
				SFCReMod.RUNTIME.fullOffset = this.fullOffset;
				SFCReMod.RUNTIME.partialOffset = this.partialOffset;
			} catch (Exception e) {
				MinecraftClient.getInstance().player.sendMessage(new TranslatableText("text.sfcr.command.sync_fail"), false);
				SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
				SFCReMod.COMMON_CONFIG_HOLDER.save();
			}
		}

		if (SFCReMod.COMMON_CONFIG.isEnableDebug())
			MinecraftClient.getInstance().player.sendMessage(new TranslatableText("text.sfcr.command.sync_succ"), false);
	}
}
