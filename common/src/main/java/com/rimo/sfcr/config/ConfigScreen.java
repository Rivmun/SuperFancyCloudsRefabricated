package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class ConfigScreen {
	ConfigBuilder builder = ConfigBuilder.create();
	final boolean oldEnableDHCompat = CONFIG.isEnableDHCompat();
	String dimensionName;

	public Screen build() {
		ClientLevel world = Minecraft.getInstance().level;
		dimensionName = world != null ? world.dimension().identifier().toString() : "null";
		//cull mode
		BooleanListEntry cullMode = builder.entryBuilder()
				.startBooleanToggle(Component.translatable("text.sfcr.option.cullMode"),
						CONFIG.getEnableViewCulling())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.cullMode.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableViewCulling)
				.build();
		//debug
		BooleanListEntry debug = builder.entryBuilder()
				.startBooleanToggle(Component.translatable("text.sfcr.option.debug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.debug.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build();
		//cloudHeightOffset
		IntegerSliderEntry cloudHeightOffset = builder.entryBuilder()
				.startIntSlider(Component.translatable("text.sfcr.option.cloudHeight")
						, CONFIG.getCloudHeightOffset()
						, - 128
						, 127)
				.setDefaultValue(0)
				.setTextGetter(value -> value < 0 ?
						Component.translatable("text.sfcr.option.followVanilla") :
						Component.nullToEmpty(value.toString())
				)
				.setTooltip(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudHeightOffset)
				.build();
		//dhCompat
		BooleanListEntry dhCompatEntry = builder.entryBuilder()
				.startBooleanToggle(Component.translatable("text.sfcr.option.dHCompat"),
						CONFIG.isEnableDHCompat())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.dHCompat.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDHCompat)
				.setRequirement(Requirement.isTrue(() -> Client.isDistantHorizonsLoaded))
				.build();
		// (i love it...
		return builder.setParentScreen(Minecraft.getInstance().screen)
				.setTitle(Client.isCustomDimensionConfig ?
						Component.translatable("text.sfcr.title.customDimensionMode", dimensionName) :
						Component.translatable("text.sfcr.title")
				)
				.setSavingRunnable(() -> {
					if (CONFIG.isCloudRenderDistanceFitToView())
						CONFIG.setCloudRenderDistance(Minecraft.getInstance().options.getEffectiveRenderDistance() * 12);
					if (Client.isCustomDimensionConfig) {
						CONFIG.save(dimensionName);
					} else {
						CONFIG.save();
					}
					Common.clearConfigCache(dimensionName);
					DATA.setConfig(CONFIG);
					Client.applyConfigChange(oldEnableDHCompat);
				})
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.general"))
						// Custom Dimension Warning
						.addEntry(builder.entryBuilder()
								.startTextDescription(Component.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
										"§b" + dimensionName
								))
								.setDisplayRequirement(Requirement.isTrue(() -> Client.isCustomDimensionConfig))
								.build())
						// Config Override Warning
						.addEntry(builder.entryBuilder()
								.startTextDescription(Component.translatable("text.sfcr.option.configHasBeenOverride.@PrefixText"))
								.setDisplayRequirement(Requirement.isTrue(() -> Client.isConfigHasBeenOverride))
								.build())
						// enable cloud
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableMod")
										, CONFIG.isEnableRender())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableMod.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableRender)
								.build())
						//enable server
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableServer")
										, CONFIG.isEnableServer())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableServer.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableServer)
								.build())
						//culling
						.addEntry(cullMode)
						//cull radian multiplier
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.cullRadianMultiplier")
										,(int) (CONFIG.getCullRadianMultiplier() * 10)
										,8
										,20)
								.setDefaultValue(12)
								.setTextGetter(value -> Component.nullToEmpty(value / 10f + "x"))
								.setTooltip(Component.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip"))
								.setDisplayRequirement(Requirement.isTrue(cullMode))
								.setSaveConsumer(value -> CONFIG.setCullRadianMultiplier(value / 10f))
								.build())
						//remesh interval
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.rebuildInterval")
										, CONFIG.getRebuildInterval()
										,0
										,30)
								.setDefaultValue(10)
								.setTextGetter(value -> value == 0 ?
										Component.translatable("text.sfcr.disabled") :
										Component.translatable("text.sfcr.frame", value)
								)
								.setTooltip(Component.translatable("text.sfcr.option.rebuildInterval.@Tooltip"))
								.setDisplayRequirement(Requirement.isTrue(cullMode))
								.setSaveConsumer(CONFIG::setRebuildInterval)
								.build())
						//DEBUG
						.addEntry(debug)
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.clouds"))
						//cloud height
						.addEntry(cloudHeightOffset)
						//cloud thickness
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.cloudLayerThickness")
										, CONFIG.getCloudLayerThickness()
										,3
										,66)
								.setDefaultValue(10)
								.setTextGetter(value -> {
									cloudHeightOffset.setMaximum(128 - value + 2);
									return Component.nullToEmpty(String.valueOf(value - 2));
								})
								.setTooltip(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudLayerThickness)
								.build())
						//cloud distance
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.cloudRenderDistance")
										, CONFIG.getCloudRenderDistance()
										,31
										,128)
								.setDefaultValue(31)
								.setTextGetter(value -> {
									if (value == 31)
										return Component.translatable("text.sfcr.option.followVanilla").append(": " + Minecraft.getInstance().options.cloudRange().get());
									return Component.nullToEmpty(value.toString());
								})
								.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRenderDistance)
								.build())
						//cloud distance fit to view
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
										, CONFIG.isCloudRenderDistanceFitToView())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRenderDistanceFitToView)
								.build())
						//cloud sample steps
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.sampleSteps")
										, CONFIG.getSampleSteps()
										,1
										,3)
								.setDefaultValue(2)
								.setTextGetter(value -> Component.nullToEmpty(value.toString()))
								.setTooltip(Component.translatable("text.sfcr.option.sampleSteps.@Tooltip"))
								.setSaveConsumer(CONFIG::setSampleSteps)
								.build())
						//terrain dodge
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableTerrainDodge")
										, CONFIG.isEnableTerrainDodge())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableTerrainDodge)
								.build())
						//cloud color
						.addEntry(builder.entryBuilder()
								.startAlphaColorField(Component.translatable("text.sfcr.option.cloudColor")
										, CONFIG.getCloudColor())
								.setDefaultValue(0xFFFFFFFF)
								.setSaveConsumer(CONFIG::setCloudColor)
								.build())
						//dusk blush
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableDuskBlush")
										, CONFIG.isEnableDuskBlush())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableDuskBlush.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableDuskBlush)
								.build())
						//bottomDim
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableBottomDim")
										, CONFIG.isEnableBottomDim())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableBottomDim.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableBottomDim)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.density"))
						// threshold
						.addEntry(builder.entryBuilder()
								.startFloatField(Component.translatable("text.sfcr.option.densityThreshold")
										, CONFIG.getDensityThreshold())
								.setDefaultValue(1.3f)
								.setMax(2f)
								.setMin(-1f)
								.setTooltip(Component.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
								.setSaveConsumer(CONFIG::setDensityThreshold)
								.build())
						// threshold multiplier
						.addEntry(builder.entryBuilder()
								.startFloatField(Component.translatable("text.sfcr.option.thresholdMultiplier")
										, CONFIG.getThresholdMultiplier())
								.setDefaultValue(1.5f)
								.setMax(3f)
								.setMin(0f)
								.setTooltip(Component.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
								.setSaveConsumer(CONFIG::setThresholdMultiplier)
								.build())
						//weather
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableWeatherDensity")
										, CONFIG.isEnableWeatherDensity())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableWeatherDensity)
								.build())
						//density
						.addEntry(builder.entryBuilder()
								.startTextDescription(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
								.build())
						//cloud common density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.cloudDensity")
										, CONFIG.getCloudDensityPercent()
										,0
										,100)
								.setDefaultValue(25)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setCloudDensityPercent)
								.build())
						//rain density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.rainDensity")
										, CONFIG.getRainDensityPercent()
										,0
										,100)
								.setDefaultValue(60)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setRainDensityPercent)
								.build())
						//thunder density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.thunderDensity")
										, CONFIG.getThunderDensityPercent()
										,0
										,100)
								.setDefaultValue(90)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setThunderDensityPercent)
								.build())
						//night density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.densityAtNight"),
										(int) (CONFIG.getDensityAtNight() * 10),
										0,
										10)
								.setDefaultValue(7)
								.setTextGetter(value -> Component.nullToEmpty(value * 10 + "%"))
								.setSaveConsumer(value -> CONFIG.setDensityAtNight(value / 10f))
								.build())
						//weather pre-detect time
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.weatherPreDetectTime")
										, CONFIG.getWeatherPreDetectTime()
										,0
										,30)
								.setDefaultValue(5)
								.setTextGetter(value -> value == 0 ?
										Component.translatable("text.sfcr.disabled") :
										Component.translatable("text.sfcr.second", value)
								)
								.setTooltip(Component.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
								.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
								.build())
						//cloud refresh speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Component.translatable("text.sfcr.option.cloudRefreshSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getNormalRefreshSpeed())
								.setDefaultValue(CloudRefreshSpeed.SLOW)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setNormalRefreshSpeed)
								.build())
						//weather refresh speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Component.translatable("text.sfcr.option.weatherRefreshSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getWeatherRefreshSpeed())
								.setDefaultValue(CloudRefreshSpeed.FAST)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
								.build())
						//density changing speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Component.translatable("text.sfcr.option.densityChangingSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getDensityChangingSpeed())
								.setDefaultValue(CloudRefreshSpeed.SLOW)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setDensityChangingSpeed)
								.build())
						//precipitation info
						.addEntry(builder.entryBuilder()
								.startTextDescription(Component.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
								.build())
						//snow
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.snowDensity")
										, CONFIG.getSnowDensity()
										,0
										,100)
								.setDefaultValue(60)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setSnowDensity)
								.build())
						//rain
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
										, CONFIG.getRainDensity()
										,0
										,100)
								.setDefaultValue(90)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setRainDensity)
								.build())
						//none
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.noneDensity")
										, CONFIG.getNoneDensity()
										,0
										,100)
								.setDefaultValue(0)
								.setTextGetter(value -> Component.nullToEmpty(value + "%"))
								.setSaveConsumer(CONFIG::setNoneDensity)
								.build())
						//biome density affect by chunk
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityByChunk")
										, CONFIG.isBiomeDensityByChunk())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeDensityByChunk)
								.build())
						//biome density detect loaded chunk
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
										, CONFIG.isBiomeDensityUseLoadedChunk())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeDensityUseLoadedChunk)
								.build())
						//biome filter
						.addEntry(builder.entryBuilder()
								.startStrList(Component.translatable("text.sfcr.option.biomeFilter")
										, CONFIG.getBiomeFilterList())
								.setDefaultValue(Config.DEF_BIOME_FILTER_LIST)
								.setTooltip(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeFilterList)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.fog"))
						//NO CLOUD NO RAIN
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.isCloudRain")
										, CONFIG.isEnableCloudRain())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.isCloudRain.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableCloudRain)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.compat"))
						//custom dimension
						.addEntry(builder.entryBuilder()
								.startTextDescription(Component.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
										(Client.isCustomDimensionConfig ? "§a" : "§c") + dimensionName
								))
								.setTooltip(Component.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
								.build())
						//distant horizons
						.addEntry(dhCompatEntry)
						//distant horizons renderdistance
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance"),
										CONFIG.getDhDistanceMultipler(),
										1,
										8)
								.setDefaultValue(1)
								.setTextGetter(value -> Component.nullToEmpty(value + "x"))
								.setTooltip(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance.@Tooltip"))
								.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
								.setSaveConsumer(CONFIG::setDhDistanceMultipler)
								.build())
						//distant horizons height
						.addEntry(builder.entryBuilder()
								.startIntSlider(Component.translatable("text.sfcr.option.DHCompat.enhanceHeight"),
										CONFIG.getDhHeightEnhance() / 16,
										0,
										32)
								.setDefaultValue(0)
								.setTextGetter(value -> Component.nullToEmpty(String.valueOf(value * 16)))
								.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
								.setSaveConsumer(value -> CONFIG.setDhHeightEnhance(value * 16))
								.build())
						//particle rain
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Component.translatable("text.sfcr.option.particleRainCompat"),
										CONFIG.isEnableParticleRainCompat())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.particleRainCompat.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableParticleRainCompat)
								.setRequirement(Requirement.isTrue(() -> Client.isParticleRainLoaded))
								.build()
						)
				)
				.build();
	}
}
