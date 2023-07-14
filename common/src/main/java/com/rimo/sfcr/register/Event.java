package com.rimo.sfcr.register;

import com.rimo.sfcr.SFCReMod;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;

public class Event {
	public static void register() {
		LifecycleEvent.SERVER_LEVEL_LOAD.register(SFCReMod.RUNTIME::init);
		TickEvent.SERVER_PRE.register(SFCReMod.RUNTIME::tick);
		PlayerEvent.PLAYER_JOIN.register(SFCReMod.RUNTIME::addPlayer);
		PlayerEvent.PLAYER_QUIT.register(SFCReMod.RUNTIME::removePlayer);
		LifecycleEvent.SERVER_STOPPING.register(SFCReMod.RUNTIME::end);
	}

	public static void registerClient() {
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> SFCReMod.RENDERER.init());
		ClientTickEvent.CLIENT_PRE.register(client -> SFCReMod.RENDERER.tick());
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			SFCReMod.RENDERER.clean();
			SFCReMod.RUNTIME.clientEnd();
			SFCReMod.COMMON_CONFIG.load();
			SFCReMod.RENDERER.updateConfig(SFCReMod.COMMON_CONFIG);
		});
	}
}
