package com.rimo.sfcr.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if < 1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///? }
//? if >= 1.20
import net.minecraft.client.gui.GuiGraphics;
//? if < 1.19
//import net.minecraft.network.chat.TranslatableComponent;

/**
 * A simple screen to notify player install Cloth Config, not just ClassNotFound in Modmenu button.
 */
//~ if < 1.19 'Component.translatable' -> 'new TranslatableComponent' {
public class MissingConfigLibScreen extends Screen {
	private final Screen parent;
	private final Component message = Component.translatable("text.traceableprint.config.missing_dependency");

	public MissingConfigLibScreen(Screen parent) {
		super(Component.translatable("text.traceableprint.config.missing_dependency"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		//? if < 1.19.3 {
		/*this.addWidget(new Button((this.width - 100) / 2, this.height / 2 + 6, 100, 20,
				CommonComponents.GUI_DONE, button -> this.onClose()));
		*///? }
		//? if >= 1.19.3 {
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds((this.width - 100) / 2, this.height / 2 + 6, 100, 20)
				.build());
		//? }
	}

	//? if < 1.20 {
	/*@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float partialTick) {
		super.render(matrices, mouseX, mouseY, partialTick);
		int textWidth = this.font.width(this.message);
		this.font.draw(matrices, this.message, (float) (this.width - textWidth) / 2.0F, this.height / 2 - 24, 0xFFFFFF);
	}
	*///? }
	//? if >= 1.20 {
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.message, this.width / 2, this.height / 2 - 24, 0xFFFFFF);
	}
	//? }

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
//~ }
