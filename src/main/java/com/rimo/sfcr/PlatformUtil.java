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

	/*
	 * Something learned from Iris: use ServiceLoader.load(PlatformUtil.IPlatform).findFirst.get() can autoload
	 * an implementation class of IPlatform from META-INF/services/<full-class-namepath> at the early load of beginning.
	 * But it's a platform specific way that must write the impl class full namepath will conflict with Forgix merging.
	 * So keep instantiate it by loaders entrypoint naturally.
	 */
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
