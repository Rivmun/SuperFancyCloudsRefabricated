package com.rimo.sfcr.register;

import com.rimo.sfcr.SFCReMod;
//import com.rimo.sfcr.network.ConfigSyncMessage;
//import com.rimo.sfcr.network.RuntimeSyncMessage;
//import com.rimo.sfcr.network.Network;
import dev.architectury.event.events.common.LifecycleEvent;
//import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;

public class Event {
	public static void register() {
		LifecycleEvent.SERVER_LEVEL_LOAD.register(SFCReMod.RUNTIME::init);
		TickEvent.SERVER_PRE.register(SFCReMod.RUNTIME::tick);
		/* @TODO: we're listening request from client now, so this have no sense..
		PlayerEvent.PLAYER_JOIN.register(player -> {
			Network.CONFIGCHANNEL.sendToPlayer(player, new ConfigSyncMessage(SFCReMod.COMMON_CONFIG));
			Network.RUNTIMECHANNEL.sendToPlayer(player, new RuntimeSyncMessage(SFCReMod.RUNTIME));
		});
		LifecycleEvent.SERVER_STOPPING.register(server -> RUNTIME.end());
		 */
	}

	public static void registerClient() {
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> SFCReMod.RENDERER.init());
		ClientTickEvent.CLIENT_PRE.register(client -> SFCReMod.RENDERER.tick());
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			SFCReMod.RENDERER.clean();
			SFCReMod.RUNTIME.clientEnd();
			if (SFCReMod.COMMON_CONFIG.isEnableServerConfig()) {
				SFCReMod.COMMON_CONFIG_HOLDER.load();
				SFCReMod.updateConfig();
			}
		});
	}
}
