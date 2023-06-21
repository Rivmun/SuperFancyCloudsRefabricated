package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CoreConfig;
import me.shedaniel.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class ConfigSyncMessage {
	private final byte[] data;
	private static final DataSerializer<CoreConfig> serializer = new DataSerializer<>();

	public ConfigSyncMessage(byte[] data) {
		this.data = data;
	}

	public ConfigSyncMessage(CoreConfig config) {
		this(serializer.serialize(config));
	}

	public static void encode(ConfigSyncMessage message, PacketByteBuf packet) {
		packet.writeByteArray(message.data);
	}

	public static ConfigSyncMessage decode(PacketByteBuf packet) {
		return new ConfigSyncMessage(packet.readByteArray());
	}

	public static void receive(ConfigSyncMessage message, Supplier<NetworkManager.PacketContext> contextSupplier) {
		NetworkManager.PacketContext context = contextSupplier.get();
		context.queue(() -> {
			if (context.getEnv() == EnvType.CLIENT) {
				serializer.deserialize(message.data).ifPresent(SFCReMod::setCommonConfig);
			}
		});
	}
}
