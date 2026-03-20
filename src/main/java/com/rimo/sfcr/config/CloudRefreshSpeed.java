package com.rimo.sfcr.config;

//~ if < 1.19 'Component' -> 'TranslatableComponent'
import net.minecraft.network.chat.Component;

public enum CloudRefreshSpeed {
	//~ if < 1.19 'Component.translatable' -> 'new TranslatableComponent' {
	VERY_SLOW(40, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));
	//~ }

	//~ if < 1.19 'Component' -> 'TranslatableComponent' {
	private final int value;
	private final Component name;

	CloudRefreshSpeed(int value, Component name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public Component getName() {
		return name;
	}
	//~ }
}
