package com.rimo.sfcr.config;

import net.minecraft.text.Text;

public enum RefreshSpeed {

	VERY_SLOW(40, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));

	private final int value;
	private final Text text;

	RefreshSpeed(int value, Text text) {
		this.value = value;
		this.text = text;
	}

	public int getValue() {
		return this.value;
	}

	public Text getStringKey() {
		return this.text;
	}

}
