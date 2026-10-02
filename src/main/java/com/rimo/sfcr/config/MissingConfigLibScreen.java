package com.rimo.sfcr.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * A simple screen to notify player install Cloth Config, not just ClassNotFound in Modmenu button.
 */
public class MissingConfigLibScreen extends Screen {
	private final Screen parent;

	public MissingConfigLibScreen(Screen parent) {
		super(Component.empty());
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		MultiLineTextWidget label = new MultiLineTextWidget(
				Component.translatable("text.traceableprint.config.missing_dependency"), this.font)
				.setMaxWidth(this.width - 60);
		label.setX((this.width - label.getWidth()) / 2);
		label.setY(this.height / 2 - 24);
		this.addRenderableWidget(label);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds((this.width - 100) / 2, this.height / 2 + 6, 100, 20)
				.build());
	}

	@Override
	public void onClose() {
		//~ if < 26.2 'minecraft.gui.setScreen' -> 'minecraft.setScreen'
		this.minecraft.gui.setScreen(this.parent);
	}
}
