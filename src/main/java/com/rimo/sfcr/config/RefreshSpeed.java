package com.rimo.sfcr.config;


import net.minecraft.network.chat.Component;

public enum RefreshSpeed {

	VERY_SLOW(40, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));

	private final int value;
	private final Component text;

	RefreshSpeed(int value, Component text) {
		this.value = value;
		this.text = text;
	}

	public int getValue() {
		return this.value;
	}

	public Component getStringKey() {
		return this.text;
	}

}
