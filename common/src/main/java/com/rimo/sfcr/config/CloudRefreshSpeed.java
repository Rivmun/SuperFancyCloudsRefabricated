package com.rimo.sfcr.config;

import net.minecraft.text.TranslatableText;

public enum CloudRefreshSpeed {
	VERY_SLOW(40, new TranslatableText("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW")),
	SLOW(30, new TranslatableText("text.sfcr.enum.cloudRefreshSpeed.SLOW")),
	NORMAL(20, new TranslatableText("text.sfcr.enum.cloudRefreshSpeed.NORMAL")),
	FAST(10, new TranslatableText("text.sfcr.enum.cloudRefreshSpeed.FAST")),
	VERY_FAST(5, new TranslatableText("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST"));

	private final int value;
	private final TranslatableText name;

	CloudRefreshSpeed(int value, TranslatableText name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public TranslatableText getName() {
		return name;
	}
}
