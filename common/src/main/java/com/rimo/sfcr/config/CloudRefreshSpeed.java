package com.rimo.sfcr.config;

import net.minecraft.text.Text;

public enum CloudRefreshSpeed {
	VERY_SLOW(40, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, Text.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));

	private final int value;
	private final Text name;

	CloudRefreshSpeed(int value, Text name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public Text getName() {
		return name;
	}
}
