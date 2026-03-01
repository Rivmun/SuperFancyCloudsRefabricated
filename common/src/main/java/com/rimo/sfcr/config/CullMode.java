package com.rimo.sfcr.config;

import net.minecraft.text.Text;

public enum CullMode {
	NONE(Text.translatable("text.sfcr.disabled")),
	CIRCULAR(Text.translatable("text.sfcr.enum.cullMode.circular")),
	RECTANGULAR(Text.translatable("text.sfcr.enum.cullMode.rectangular"));

	private final Text name;

	CullMode(Text name) {
		this.name = name;
	}

	public Text getName() {
		return name;
	}
}
