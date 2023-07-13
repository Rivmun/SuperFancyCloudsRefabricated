package com.rimo.sfcr.register;

import com.rimo.sfcr.SFCReMod;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import me.shedaniel.architectury.event.events.TickEvent;
import me.shedaniel.architectury.event.events.client.ClientPlayerEvent;
import me.shedaniel.architectury.event.events.client.ClientTickEvent;

public class Event {
	public static void register() {
		LifecycleEvent.SERVER_WORLD_LOAD.register(SFCReMod.RUNTIME::init);
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
			SFCReMod.COMMON_CONFIG_HOLDER.load();
			SFCReMod.RENDERER.updateConfig(SFCReMod.COMMON_CONFIG);
		});
	}
}
