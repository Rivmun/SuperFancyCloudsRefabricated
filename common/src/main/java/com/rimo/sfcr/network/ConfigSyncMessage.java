package com.rimo.sfcr.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CoreConfig;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class ConfigSyncMessage {
	private final long seed;
	private final String data;
	private static final Gson gson = new Gson();

	public ConfigSyncMessage(long seed, CoreConfig config) {
		this.seed = seed;
		this.data = gson.toJson(config);
	}

	public ConfigSyncMessage(PacketByteBuf packet) {
		this.seed = packet.readLong();
		this.data = packet.readString();
	}

	public static void encode(ConfigSyncMessage message, PacketByteBuf packet) {
		packet.writeLong(message.seed);
		packet.writeString(message.data);
	}

	public static void receive(ConfigSyncMessage message, Supplier<NetworkManager.PacketContext> contextSupplier) {
		if (!SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;
		if (contextSupplier.get().getEnv() != EnvType.CLIENT)
			return;

		contextSupplier.get().queue(() -> {
			try {
				SFCReMod.RUNTIME.seed = message.seed;
				SFCReMod.COMMON_CONFIG.setCoreConfig(gson.fromJson(message.data, CoreConfig.class));
			} catch (JsonSyntaxException e) {
				SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
				SFCReMod.COMMON_CONFIG_HOLDER.save();
				contextSupplier.get().getPlayer().sendMessage(Text.translatable("text.sfcr.command.sync_fail"), false);
				return;
			}

			SFCReMod.RENDERER.init();		// Reset renderer.
			SFCReMod.RENDERER.updateConfig(SFCReMod.COMMON_CONFIG);
			if (SFCReMod.COMMON_CONFIG.isEnableDebug())
				contextSupplier.get().getPlayer().sendMessage(Text.translatable("text.sfcr.command.sync_full_succ"), false);
		});
	}
}
