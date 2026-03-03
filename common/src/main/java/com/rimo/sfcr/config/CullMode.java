package com.rimo.sfcr.config;

import net.minecraft.text.TranslatableText;

public enum CullMode {
	NONE(new TranslatableText("text.sfcr.disabled")),
	CIRCULAR(new TranslatableText("text.sfcr.enum.cullMode.circular")),
	RECTANGULAR(new TranslatableText("text.sfcr.enum.cullMode.rectangular"));

	private final TranslatableText name;

	CullMode(TranslatableText name) {
		this.name = name;
	}

	public TranslatableText getName() {
		return name;
	}
}
