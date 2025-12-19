package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ConfigScreenYACL {
	private final Config CONFIG = Common.CONFIG;
	private final boolean oldEnableMod = CONFIG.isEnableMod();
	private final boolean oldDHCompat = CONFIG.isEnableDHCompat();
	private final boolean oldBottomDim = CONFIG.isEnableBottomDim();

	public Screen buildScreen(Screen parent) {
		//pre build
		Option<Integer> cloudLayerThickness = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.cloudLayerThickness"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip")))
				.binding(34, CONFIG::getCloudThickness, CONFIG::setCloudThickness)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(3, 66)
						.step(1)
						.formatValue(value -> Component.nullToEmpty(String.valueOf(value - 2))))
				.build();
		Option<Integer> cloudDistance = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.cloudRenderDistance"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip")))
				.binding(31, CONFIG::getRenderDistance, CONFIG::setRenderDistance)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(31, 128)
						.step(1)
						.formatValue(value -> {
							if (value == 31)
								return Component.translatable("text.sfcr.option.followVanilla").append(": " + Minecraft.getInstance().options.cloudRange().get());
							return Component.nullToEmpty(value.toString());
						}))
				.build();
		Option<Boolean> biomeDetectUseLoadedChunk = Option.<Boolean>createBuilder()
				.name(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip")))
				.binding(false, CONFIG::isEnableBiomeDensityUseLoadedChunk, CONFIG::setEnableBiomeDensityUseLoadedChunk)
				.controller(TickBoxControllerBuilder::create)
				.build();
		Option<Integer> dHEnhanceDistance = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance.@Tooltip")))
				.binding(1, CONFIG::getDhDistanceMultipler, CONFIG::setDhDistanceMultipler)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(1, 8)
						.step(1)
						.formatValue(value -> Component.nullToEmpty(value + "x")))
				.build();
		Option<Integer> dHEnhanceHeight = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.DHCompat.enhanceHeight"))
				.binding(1, CONFIG::getDhHeightEnhance, CONFIG::setDhHeightEnhance)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(0, 512)
						.step(16))
				.build();
		LabelOption dHDetectBiomeByChunk = LabelOption.create(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHChunk",
//				CONFIG.isEnableBiomeDensityByChunk() ?
//						Component.translatable("text.cloth-config.boolean.value.true") :
						Component.translatable("text.cloth-config.boolean.value.false")
		));
		LabelOption dHDetectBiomeUseLoadedChunk = LabelOption.create(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHLoadedChunk",
//				CONFIG.isEnableBiomeDensityUseLoadedChunk() ?
//						Component.translatable("text.cloth-config.boolean.value.true") :
						Component.translatable("text.cloth-config.boolean.value.false")
		));
		// well..
		return YetAnotherConfigLib.createBuilder()
				.title(Client.isCustomDimensionConfig ?
						Component.translatable("text.sfcr.option.title.customDimensionMode") :
						Component.translatable("text.sfcr.option.title"))
				.save(() -> {
					if (Client.isCustomDimensionConfig)
						Config.save(CONFIG, Minecraft.getInstance().level.dimension().identifier().toString());
					else
						Config.save(CONFIG);
					Client.applyConfigChange(oldEnableMod, oldDHCompat);
					if (Minecraft.getInstance().level != null && (oldEnableMod != CONFIG.isEnableMod() || oldBottomDim != CONFIG.isEnableBottomDim()))  //notify vanilla cloudRenderer to update
						Minecraft.getInstance().levelRenderer.getCloudRenderer().markForRebuild();
				})
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("text.sfcr.category.general"))
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableMod"))
								.binding(true, CONFIG::isEnableMod, CONFIG::setEnableMod)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudHeight"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip")))
								.binding(0, CONFIG::getCloudHeightOffset, CONFIG::setCloudHeightOffset)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(-128, 128)
										.step(1))
								.addListener(((option, event) -> {
									switch (event) {case INITIAL, STATE_CHANGE -> {
										if (option.pendingValue() > 128 - cloudLayerThickness.pendingValue() + 2)
											option.requestSet(128 - cloudLayerThickness.pendingValue() + 2);
									}}
								}))
								.build())
						.option(cloudLayerThickness)
						.option(cloudDistance)
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip")))
								.binding(false, CONFIG::isEnableRenderDistanceFitToView, CONFIG::setEnableRenderDistanceFitToView)
								.controller(TickBoxControllerBuilder::create)
								.addListener((option, event) -> {
									switch (event) {case STATE_CHANGE, INITIAL -> cloudDistance.setAvailable(!option.pendingValue());}
								})
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.sampleSteps"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.sampleSteps.@Tooltip")))
								.binding(2, CONFIG::getSampleSteps, CONFIG::setSampleSteps)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(1, 3)
										.step(1))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableTerrainDodge"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip")))
								.binding(false, CONFIG::isEnableTerrainDodge, CONFIG::setEnableTerrainDodge)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.option(Option.<Color>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudColor"))
								.binding(new Color(0xFFFFFF), () -> new Color(CONFIG.getCloudColor()), value -> CONFIG.setCloudColor(value.getRGB()))
								.controller(ColorControllerBuilder::create)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableDuskBlush"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableDuskBlush.@Tooltip")))
								.binding(true, CONFIG::isEnableDuskBlush, CONFIG::setEnableDuskBlush)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableBottomDim"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableBottomDim.@Tooltip")))
								.binding(true, CONFIG::isEnableBottomDim, CONFIG::setEnableBottomDim)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableDebug"))
								.binding(false, CONFIG::isEnableDebug, CONFIG::setEnableDebug)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("text.sfcr.category.density"))
						.option(Option.<Float>createBuilder()
								.name(Component.translatable("text.sfcr.option.densityThreshold"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.densityThreshold.@Tooltip")))
								.binding(1.3f, CONFIG::getDensityThreshold, CONFIG::setDensityThreshold)
								.controller(opt -> FloatSliderControllerBuilder.create(opt)
										.range(-1f, 2f)
										.step(0.1f))
								.build())
						.option(Option.<Float>createBuilder()
								.name(Component.translatable("text.sfcr.option.thresholdMultiplier"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip")))
								.binding(1.5f, CONFIG::getThresholdMultiplier, CONFIG::setThresholdMultiplier)
								.controller(opt -> FloatSliderControllerBuilder.create(opt)
										.range(0f, 3f)
										.step(0.1f))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableDynamic"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableDynamic.@Tooltip")))
								.binding(true, CONFIG::isEnableDynamic, CONFIG::setEnableDynamic)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option((LabelOption.create(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"))))
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudDensity"))
								.binding(25, CONFIG::getDensityPercent, CONFIG::setDensityPercent)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(0, 100)
										.step(1)
										.formatValue(value -> Component.nullToEmpty(value + "%")))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableWeatherDensity"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip")))
								.binding(true, CONFIG::isEnableWeatherDensity, CONFIG::setEnableWeatherDensity)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.rainDensity"))
								.binding(60, CONFIG::getRainDensityPercent, CONFIG::setRainDensityPercent)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(0, 100)
										.step(1)
										.formatValue(value -> Component.nullToEmpty(value + "%")))
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.thunderDensity"))
								.binding(90, CONFIG::getThunderDensityPercent, CONFIG::setThunderDensityPercent)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(0, 100)
										.step(1)
										.formatValue(value -> Component.nullToEmpty(value + "%")))
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.weatherPreDetectTime"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip")))
								.binding(10, CONFIG::getWeatherPreDetectTime, CONFIG::setWeatherPreDetectTime)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(0, 30)
										.step(1)
										.formatValue(value -> {
											if (value == 0)
												return Component.translatable("text.sfcr.disabled");
											return Component.translatable("text.sfcr.second", value);
										}))
								.build())
						.option(Option.<RefreshSpeed>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudRefreshSpeed"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip")))
								.binding(RefreshSpeed.SLOW, CONFIG::getRefreshSpeed, CONFIG::setRefreshSpeed)
								.controller(opt -> EnumControllerBuilder.create(opt)
										.enumClass(RefreshSpeed.class)
										.formatValue(RefreshSpeed::getStringKey))
								.build())
						.option(Option.<RefreshSpeed>createBuilder()
								.name(Component.translatable("text.sfcr.option.weatherRefreshSpeed"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip")))
								.binding(RefreshSpeed.FAST, CONFIG::getWeatherRefreshSpeed, CONFIG::setWeatherRefreshSpeed)
								.controller(opt -> EnumControllerBuilder.create(opt)
										.enumClass(RefreshSpeed.class)
										.formatValue(RefreshSpeed::getStringKey))
								.build())
						.option(Option.<RefreshSpeed>createBuilder()
								.name(Component.translatable("text.sfcr.option.densityChangingSpeed"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip")))
								.binding(RefreshSpeed.SLOW, CONFIG::getDensityChangingSpeed, CONFIG::setDensityChangingSpeed)
								.controller(opt -> EnumControllerBuilder.create(opt)
										.enumClass(RefreshSpeed.class)
										.formatValue(RefreshSpeed::getStringKey))
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.biomeDensityMultiplier"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.biomeDensityMultiplier.@Tooltip")))
								.binding(50, CONFIG::getBiomeDensityPercent, CONFIG::setBiomeDensityPercent)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(0, 100)
										.step(1)
										.formatValue(value -> {
											if (value == 0)
												return Component.translatable("text.sfcr.disabled");
											return Component.nullToEmpty(value + "%");
										}))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.isBiomeDensityByChunk"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip")))
								.binding(false, CONFIG::isEnableBiomeDensityByChunk, CONFIG::setEnableBiomeDensityByChunk)
								.controller(TickBoxControllerBuilder::create)
								.addListener((option, event) -> {
									switch (event) {case INITIAL, STATE_CHANGE -> biomeDetectUseLoadedChunk.setAvailable(option.pendingValue());}
								})
								.build())
						.option(biomeDetectUseLoadedChunk)
						.option(ListOption.<String>createBuilder()
								.name(Component.translatable("text.sfcr.option.biomeFilter"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip")))
								.binding(Config.DEF_BIOME_BLACKLIST, CONFIG::getBiomeBlackList, CONFIG::setBiomeBlackList)
								.controller(StringControllerBuilder::create)
								.initial("")
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("text.sfcr.category.compat"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
										Minecraft.getInstance().level != null ?
												(Client.isCustomDimensionConfig ? "§a" : "§c") + Minecraft.getInstance().level.dimension().identifier() :
												"§7null"
								))
								.option(LabelOption.create(Component.translatable("text.sfcr.option.dimensionCompat.@Tooltip")))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.DHCompat"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.DHCompat.@Tooltip")))
								.binding(false, CONFIG::isEnableDHCompat, CONFIG::setEnableDHCompat)
								.controller(BooleanControllerBuilder::create)
								.addListener((option, event) -> {
									switch (event) {case INITIAL, STATE_CHANGE -> {
										dHEnhanceDistance.setAvailable(option.pendingValue());
										dHEnhanceHeight.setAvailable(option.pendingValue());
//										dHDetectBiomeByChunk.setAvailable(option.pendingValue());
//										dHDetectBiomeUseLoadedChunk.setAvailable(option.pendingValue());
									}}
								})
								.available(Client.isDistantHorizonsLoaded)
								.build())
						.option(dHEnhanceDistance)
						.option(dHEnhanceHeight)
						.option(dHDetectBiomeByChunk)
						.option(dHDetectBiomeUseLoadedChunk)
						.build())
				.build()
				.generateScreen(parent);
	}
}
