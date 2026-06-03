package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.List;

import static com.rimo.sfcr.Common.DATA;

public class ConfigScreenYACL {
	private final Config CONFIG = Common.CONFIG;
	private final boolean oldEnableMod = CONFIG.isEnableRender();
	private final boolean oldBottomDim = CONFIG.isEnableBottomDim();
	private final String dimensionName;
	private final boolean isCustomDimension;

	public ConfigScreenYACL() {
		ClientLevel level = Minecraft.getInstance().level;
		dimensionName = level != null ? level.dimension().identifier().toString() : Config.OVERWORLD;
		isCustomDimension = ! dimensionName.equals(Config.OVERWORLD);
	}

	public Screen buildScreen(Screen parent) {
		//cloudLayerThickness
		Option<Integer> cloudLayerThickness = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.cloudLayerThickness"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip")))
				.binding(34, CONFIG::getCloudLayerThickness, CONFIG::setCloudLayerThickness)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(3, 66)
						.step(1)
						.formatValue(value -> Component.nullToEmpty(String.valueOf(value - 2))))
				.build();
		//cloudRenderDistance
		Option<Integer> cloudDistance = Option.<Integer>createBuilder()
				.name(Component.translatable("text.sfcr.option.cloudRenderDistance"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip")))
				.binding(31, CONFIG::getCloudRenderDistance, CONFIG::setCloudRenderDistance)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(31, 128)
						.step(1)
						.formatValue(value -> {
							if (value == 31)
								return Component.translatable("text.sfcr.option.cloudHeight.followVanilla").append(": " + Minecraft.getInstance().options.cloudRange().get());
							return Component.nullToEmpty(value.toString());
						}))
				.build();
		//cullMode
		Option<Boolean> cullMode = Option.<Boolean>createBuilder()
				.name(Component.translatable("text.sfcr.option.cullMode"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.cullMode.@Tooltip")))
				.binding(true, CONFIG::getEnableViewCulling, CONFIG::setEnableViewCulling)
				.controller(BooleanControllerBuilder::create)
				.build();
		//deleteAfterQuit
		Option<Boolean> deleteAfterQuit = Option.<Boolean>createBuilder()
				.name(Component.translatable("text.sfcr.option.deleteDimensionAfterQuit", Component.translatable("yacl.gui.finished.tooltip")))
				.binding(false, () -> false, _ -> {})
				.controller(BooleanControllerBuilder::create)
				.build();
		Option<Boolean> enableDynamic = Option.<Boolean>createBuilder()
				.name(Component.translatable("text.sfcr.option.enableWeatherDensity"))
				.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip")))
				.binding(true, CONFIG::isEnableDynamic, CONFIG::setEnableDynamic)
				.controller(BooleanControllerBuilder::create)
				.build();
		// well..
		return YetAnotherConfigLib.createBuilder()
				.title(isCustomDimension ?
						Component.translatable("text.sfcr.title.customDimensionMode", dimensionName) :
						Component.translatable("text.sfcr.title"))
				.save(() -> {
					if (deleteAfterQuit.pendingValue()) {
						Config.delete(dimensionName);
						Common.setDimensionConfigJson(dimensionName, "");
						CONFIG.load();
						Client.isCustomDimensionConfig = false;
					} else {
						Common.setDimensionConfigJson(dimensionName, CONFIG.toString());
						CONFIG.save(dimensionName);
						if (isCustomDimension)
							Client.isCustomDimensionConfig = true;
					}
					DATA.setConfig(CONFIG);
					Client.applyConfigChange();
					if (Minecraft.getInstance().level != null && (oldEnableMod != CONFIG.isEnableRender() || oldBottomDim != CONFIG.isEnableBottomDim()))
						Minecraft.getInstance().levelRenderer.cloudRenderer().markForRebuild();  //notify vanilla cloudRenderer to update
				})
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("text.sfcr.category.general"))
						.optionIf(Client.isCustomDimensionConfig, LabelOption.create(Component.translatable("text.sfcr.option.customDimensionMode.@PrefixText")))
						.optionIf(Client.isConfigHasBeenOverride, LabelOption.create(Component.translatable("text.sfcr.option.configHasBeenOverride.@PrefixText")))
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableMod"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableMod.@Tooltip")))
								.binding(true, CONFIG::isEnableRender, CONFIG::setEnableRender)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.enableServer"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.enableServer.@Tooltip")))
								.binding(true, CONFIG::isEnableServer, CONFIG::setEnableServer)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option(cullMode)
						.optionIf(cullMode.pendingValue(), Option.<Float>createBuilder()
								.name(Component.translatable("text.sfcr.option.cullRadianMultiplier"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip")))
								.binding(1.2F, CONFIG::getCullRadianMultiplier, CONFIG::setCullRadianMultiplier)
								.controller(opt1 -> FloatSliderControllerBuilder.create(opt1)
										.range(0.8F, 2.0F)
										.step(0.1F)
										.formatValue(value1 -> Component.nullToEmpty(value1 + "x")))
								.build())
						.optionIf(cullMode.pendingValue(), Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.rebuildInterval"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.rebuildInterval.@Tooltip")))
								.binding(10, CONFIG::getRebuildInterval, CONFIG::setRebuildInterval)
								.controller(opt2 -> IntegerSliderControllerBuilder.create(opt2)
										.range(0, 30)
										.step(1)
										.formatValue(value2 -> value2 == 0 ?
												Component.translatable("text.sfcr.disabled") :
												Component.translatable("text.sfcr.frame", value2)
										))
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.debug"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.debug.@Tooltip")))
								.binding(false, CONFIG::isEnableDebug, CONFIG::setEnableDebug)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("text.sfcr.category.clouds"))
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudHeight"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip")))
								.binding(0, CONFIG::getCloudHeight, CONFIG::setCloudHeight)
								.controller(opt -> IntegerSliderControllerBuilder.create(opt)
										.range(-192, 192)
										.step(1)
										.formatValue(value -> value == 0 ?
												Component.translatable("text.sfcr.option.cloudHeight.followVanilla") :
												Component.nullToEmpty(value.toString())
										))
								.build())
						.option(Option.<Integer>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudBlockSize"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudBlockSize.@Tooltip")))
								.binding(12, CONFIG::getCloudBlockSize, CONFIG::setCloudBlockSize)
								.controller(opt -> CyclingListControllerBuilder.create(opt)
										.values(List.of(8, 12, 16))
										.formatValue(value -> Component.nullToEmpty(value.toString())))
								.build())
						.option(cloudLayerThickness)
						.option(cloudDistance)
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
								.binding(1.5f, CONFIG::getThresholdMaxReduction, CONFIG::setThresholdMaxReduction)
								.controller(opt -> FloatSliderControllerBuilder.create(opt)
										.range(0f, 3f)
										.step(0.1f))
								.build())
						.option(enableDynamic)
						.groupIf(enableDynamic.pendingValue(), OptionGroup.createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
								.collapsed(false)
								.option(Option.<Integer>createBuilder()
										.name(Component.translatable("text.sfcr.option.cloudDensity"))
										.binding(25, CONFIG::getCloudDensityPercent, CONFIG::setCloudDensityPercent)
										.controller(opt -> IntegerSliderControllerBuilder.create(opt)
												.range(0, 100)
												.step(1)
												.formatValue(value -> Component.nullToEmpty(value + "%")))
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
								.option(Option.<Float>createBuilder()
										.name(Component.translatable("text.sfcr.option.densityAtNight"))
										.description(OptionDescription.of(Component.translatable("text.sfcr.option.densityAtNight.@Tooltip")))
										.binding(0.7F, CONFIG::getDensityAtNight, CONFIG::setDensityAtNight)
										.controller(opt -> FloatSliderControllerBuilder.create(opt)
												.range(0F, 1F)
												.step(1F))
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
								.option(Option.<CloudRefreshSpeed>createBuilder()
										.name(Component.translatable("text.sfcr.option.cloudRefreshSpeed"))
										.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip")))
										.binding(CloudRefreshSpeed.SLOW, CONFIG::getNormalRefreshSpeed, CONFIG::setNormalRefreshSpeed)
										.controller(opt -> EnumControllerBuilder.create(opt)
												.enumClass(CloudRefreshSpeed.class)
												.formatValue(CloudRefreshSpeed::getStringKey))
										.build())
								.option(Option.<CloudRefreshSpeed>createBuilder()
										.name(Component.translatable("text.sfcr.option.weatherRefreshSpeed"))
										.description(OptionDescription.of(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip")))
										.binding(CloudRefreshSpeed.FAST, CONFIG::getWeatherRefreshSpeed, CONFIG::setWeatherRefreshSpeed)
										.controller(opt -> EnumControllerBuilder.create(opt)
												.enumClass(CloudRefreshSpeed.class)
												.formatValue(CloudRefreshSpeed::getStringKey))
										.build())
								.option(Option.<CloudRefreshSpeed>createBuilder()
										.name(Component.translatable("text.sfcr.option.densityChangingSpeed"))
										.description(OptionDescription.of(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip")))
										.binding(CloudRefreshSpeed.SLOW, CONFIG::getDensityChangingSpeed, CONFIG::setDensityChangingSpeed)
										.controller(opt -> EnumControllerBuilder.create(opt)
												.enumClass(CloudRefreshSpeed.class)
												.formatValue(CloudRefreshSpeed::getStringKey))
										.build())
								.build())
						.groupIf(enableDynamic.pendingValue(), OptionGroup.createBuilder()
								.name(Component.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
								.collapsed(false)
								.option(Option.<Integer>createBuilder()
										.name(Component.translatable("text.sfcr.option.biomeDensityMultiplier"))
										.description(OptionDescription.of(Component.translatable("text.sfcr.option.biomeDensityMultiplier.@Tooltip")))
										.binding(70, CONFIG::getBiomeAffectPercent, CONFIG::setBiomeAffectPercent)
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
										.binding(false, CONFIG::isBiomeDensityByChunk, CONFIG::setBiomeDensityByChunk)
										.controller(TickBoxControllerBuilder::create)
										.build())
								.build())
						.groupIf(enableDynamic.pendingValue(), ListOption.<String>createBuilder()
								.name(Component.translatable("text.sfcr.option.biomeFilter"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip")))
								.binding(Config.DEF_BIOME_FILTER_LIST, CONFIG::getBiomeFilterList, CONFIG::setBiomeFilterList)
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
								.optionIf(isCustomDimension, deleteAfterQuit)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.isCloudRain"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.isCloudRain.@Tooltip")))
								.binding(false, CONFIG::isEnableCloudRain, CONFIG::setEnableCloudRain)
								.controller(BooleanControllerBuilder::create)
								.build())
						.option(Option.<Boolean>createBuilder()
								.name(Component.translatable("text.sfcr.option.cloudRainLogically"))
								.description(OptionDescription.of(Component.translatable("text.sfcr.option.cloudRainLogically.@Tooltip")))
								.binding(false, CONFIG::isCloudRainLogically, CONFIG::setCloudRainLogically)
								.controller(TickBoxControllerBuilder::create)
								.build())
						.build())
				.build()
				.generateScreen(parent);
	}
}
