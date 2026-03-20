package com.rimo.sfcr.config;

//~ if < 1.19 'Component' -> 'TranslatableComponent'
import net.minecraft.network.chat.TranslatableComponent;

public enum CloudRefreshSpeed {
	//~ if < 1.19 'Component.translatable' -> 'new TranslatableComponent' {
	VERY_SLOW(40, new TranslatableComponent("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, new TranslatableComponent("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, new TranslatableComponent("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, new TranslatableComponent("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, new TranslatableComponent("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));
	//~ }

	//~ if < 1.19 'Component' -> 'TranslatableComponent' {
	private final int value;
	private final TranslatableComponent name;

	CloudRefreshSpeed(int value, TranslatableComponent name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public TranslatableComponent getName() {
		return name;
	}
	//~ }
}
