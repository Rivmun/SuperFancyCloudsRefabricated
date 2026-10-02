package com.rimo.sfcr;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * The platform layer must init before Common.CONFIG otherwise it will throw a NPE while SharedConfig.DEFAULT_PATH getConfigFolder.
 * So we move platform layer into this independent class to prevent that.
 */
public final class PlatformUtil {
	private PlatformUtil() {}

	/**
	 * Must init at the beginning of each loader's entrypoint initialization.
	 */
	public static IPlatform PLATFORM;

	// Platform specific function interface
	public interface IPlatform {
		boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type);
		void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
		void sendToServer(CustomPacketPayload payload);
		boolean isModLoaded(String id);
		Path getConfigFolder();
		boolean isFabric();
		String getClothID();
	}
}
