package com.rimo.sfcr.util;

import net.minecraft.text.Text;

public enum CullMode {
	NONE,
	CIRCULAR,
	RECTANGULAR;

	public Text getName() {
		switch (this) {
			case NONE -> {return Text.translatable("text.sfcr.disabled");}
			case CIRCULAR -> {return Text.translatable("text.sfcr.enum.cullMode.circular");}
			case RECTANGULAR -> {return Text.translatable("text.sfcr.enum.cullMode.rectangular");}
		}
		return null;
	}
}
