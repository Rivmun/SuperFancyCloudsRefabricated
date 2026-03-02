package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

import java.util.List;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class ConfigScreen {
	ConfigBuilder builder = ConfigBuilder.create();
	final boolean oldEnableDHCompat = CONFIG.isEnableDHCompat();
	int fogMin, fogMax;
	String dimensionName;

	public Screen build() {
		ClientWorld world = MinecraftClient.getInstance().world;
		dimensionName = world != null ? world.getRegistryKey().getValue().toString() : "null";
		//cull mode
		EnumListEntry<CullMode> cullMode = builder.entryBuilder()
				.startEnumSelector(Text.translatable("text.sfcr.option.cullMode")
						, CullMode.class
						, CONFIG.getCullMode())
				.setDefaultValue(CullMode.RECTANGULAR)
				.setEnumNameProvider(value -> ((CullMode) value).getName())
				.setTooltip(Text.translatable("text.sfcr.option.cullMode.@Tooltip"))
				.setSaveConsumer(CONFIG::setCullMode)
				.build();
		//auto fog
		BooleanListEntry autoFog = builder.entryBuilder()
				.startBooleanToggle(Text.translatable("text.sfcr.option.fogAutoDistance")
						, CONFIG.isFogAutoDistance())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.fogAutoDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setFogAutoDistance)
				.build();
		// (i love it...
		return builder.setParentScreen(MinecraftClient.getInstance().currentScreen)
				.setTitle(Client.isCustomDimensionConfig ?
						Text.translatable("text.sfcr.title.customDimensionMode", dimensionName) :
						Text.translatable("text.sfcr.title")
				)
				.setSavingRunnable(() -> {
					if (CONFIG.isCloudRenderDistanceFitToView())
						CONFIG.setCloudRenderDistance(MinecraftClient.getInstance().options.getClampedViewDistance() * 12);
					CONFIG.setFogDisance(fogMin, fogMax);
					if (Client.isCustomDimensionConfig) {
						CONFIG.save(dimensionName);
					} else {
						CONFIG.save();
					}
					Common.clearConfigCache(dimensionName);
					DATA.setConfig(CONFIG);
					Client.applyConfigChange(oldEnableDHCompat);
				})
				.setFallbackCategory(builder.getOrCreateCategory(Text.translatable("text.sfcr.category.general"))
						// Custom Dimension Warning
						.addEntry(builder.entryBuilder()
								.startTextDescription(Text.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
										"§b" + dimensionName
								))
								.setDisplayRequirement(Requirement.isTrue(() -> Client.isCustomDimensionConfig))
								.build())
						// Config Override Warning
						.addEntry(builder.entryBuilder()
								.startTextDescription(Text.translatable("text.sfcr.option.configHasBeenOverride.@PrefixText"))
								.setDisplayRequirement(Requirement.isTrue(() -> Client.isConfigHasBeenOverride))
								.build())
						// enable cloud
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableMod")
										, CONFIG.isEnableRender())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableMod.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableRender)
								.build())
						//enable server
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableServer")
										, CONFIG.isEnableServer())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableServer.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableServer)
								.build())
						.addEntry(cullMode)
						//cull radian multiplier
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cullRadianMultiplier")
										,(int) (CONFIG.getCullRadianMultiplier() * 10)
										,5
										,15)
								.setDefaultValue(10)
								.setTextGetter(value -> Text.of(value / 10f + "x"))
								.setTooltip(Text.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip"))
								.setDisplayRequirement(Requirement.isValue(cullMode, CullMode.CIRCULAR, CullMode.RECTANGULAR))
								.setSaveConsumer(value -> CONFIG.setCullRadianMultiplier(value / 10f))
								.build())
						//remesh interval
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.rebuildInterval")
										, CONFIG.getRebuildInterval()
										,0
										,30)
								.setDefaultValue(10)
								.setTextGetter(value -> value == 0 ?
										Text.translatable("text.sfcr.disabled") :
										Text.translatable("text.sfcr.frame", value)
								)
								.setTooltip(Text.translatable("text.sfcr.option.rebuildInterval.@Tooltip"))
								.setDisplayRequirement(Requirement.isValue(cullMode, CullMode.CIRCULAR, CullMode.RECTANGULAR))
								.setSaveConsumer(CONFIG::setRebuildInterval)
								.build())
						//DEBUG
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.debug")
										, CONFIG.isEnableDebug())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.debug.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableDebug)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Text.translatable("text.sfcr.category.clouds"))
						//cloud height
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cloudHeight")
										, CONFIG.getCloudHeight()
										,-1
										,384)
								.setDefaultValue(192)
								.setTextGetter(value -> value < 0 ?
										Text.translatable("text.sfcr.option.cloudHeight.followVanilla") :
										Text.of(value.toString())
								)
								.setTooltip(Text.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudHeight)
								.build())
						//cloud block size
						.addEntry(builder.entryBuilder()
								.startDropdownMenu(Text.translatable("text.sfcr.option.cloudBlockSize")
										,TopCellElementBuilder.of(CONFIG.getCloudBlockSize(), Integer::parseInt))
								.setDefaultValue(12)
								.setSuggestionMode(false)
								.setSelections(List.of(2, 4, 8, 12, 16))
								.setTooltip(Text.translatable("text.sfcr.option.cloudBlockSize.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudBlockSize)
								.build())
						//cloud thickness
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cloudLayerThickness")
										, CONFIG.getCloudLayerThickness()
										,3
										,66)
								.setDefaultValue(10)
								.setTextGetter(value -> Text.of(String.valueOf(value - 2)))
								.setTooltip(Text.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudLayerThickness)
								.build())
						//cloud distance
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cloudRenderDistance")
										, CONFIG.getCloudRenderDistance()
										,32
										,192)
								.setDefaultValue(64)
								.setTextGetter(value -> Text.of(value.toString()))
								.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRenderDistance)
								.build())
						//cloud distance fit to view
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
										, CONFIG.isCloudRenderDistanceFitToView())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRenderDistanceFitToView)
								.build())
						//cloud sample steps
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.sampleSteps")
										, CONFIG.getSampleSteps()
										,1
										,3)
								.setDefaultValue(2)
								.setTextGetter(value -> Text.of(value.toString()))
								.setTooltip(Text.translatable("text.sfcr.option.sampleSteps.@Tooltip"))
								.setSaveConsumer(CONFIG::setSampleSteps)
								.build())
						//terrain dodge
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableTerrainDodge")
										, CONFIG.isEnableTerrainDodge())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableTerrainDodge)
								.build())
						//cloud color
						.addEntry(builder.entryBuilder()
								.startAlphaColorField(Text.translatable("text.sfcr.option.cloudColor")
										, CONFIG.getCloudColor())
								.setDefaultValue(0xFFFFFFFF)
								.setSaveConsumer(CONFIG::setCloudColor)
								.build())
						//cloud bright multiplier
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cloudBright")
										, (int) (CONFIG.getCloudBrightMultiplier() * 10)
										, 0
										, 10)
								.setDefaultValue(1)
								.setTextGetter(value -> Text.of(value * 10 + "%"))
								.setSaveConsumer(value -> CONFIG.setCloudBrightMultiplier(value / 10f))
								.build())
						//dusk blush
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableDuskBlush")
										, CONFIG.isEnableDuskBlush())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableDuskBlush.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableDuskBlush)
								.build())
						//bottomDim
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableBottomDim")
										, CONFIG.isEnableBottomDim())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableBottomDim.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableBottomDim)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Text.translatable("text.sfcr.category.density"))
						// threshold
						.addEntry(builder.entryBuilder()
								.startFloatField(Text.translatable("text.sfcr.option.densityThreshold")
										, CONFIG.getDensityThreshold())
								.setDefaultValue(1.3f)
								.setMax(2f)
								.setMin(-1f)
								.setTooltip(Text.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
								.setSaveConsumer(CONFIG::setDensityThreshold)
								.build())
						// threshold multiplier
						.addEntry(builder.entryBuilder()
								.startFloatField(Text.translatable("text.sfcr.option.thresholdMultiplier")
										, CONFIG.getThresholdMultiplier())
								.setDefaultValue(1.5f)
								.setMax(3f)
								.setMin(0f)
								.setTooltip(Text.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
								.setSaveConsumer(CONFIG::setThresholdMultiplier)
								.build())
						//weather
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableWeatherDensity")
										, CONFIG.isEnableWeatherDensity())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableWeatherDensity)
								.build())
						//density
						.addEntry(builder.entryBuilder()
								.startTextDescription(Text.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
								.build())
						//cloud common density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.cloudDensity")
										, CONFIG.getCloudDensityPercent()
										,0
										,100)
								.setDefaultValue(25)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setCloudDensityPercent)
								.build())
						//rain density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.rainDensity")
										, CONFIG.getRainDensityPercent()
										,0
										,100)
								.setDefaultValue(60)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setRainDensityPercent)
								.build())
						//thunder density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.thunderDensity")
										, CONFIG.getThunderDensityPercent()
										,0
										,100)
								.setDefaultValue(90)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setThunderDensityPercent)
								.build())
						//night density
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.densityAtNight"),
										(int) (CONFIG.getDensityAtNight() * 10),
										0,
										10)
								.setDefaultValue(7)
								.setTextGetter(value -> Text.of(value * 10 + "%"))
								.setSaveConsumer(value -> CONFIG.setDensityAtNight(value / 10f))
								.build())
						//weather pre-detect time
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.weatherPreDetectTime")
										, CONFIG.getWeatherPreDetectTime()
										,0
										,30)
								.setDefaultValue(5)
								.setTextGetter(value -> value == 0 ?
										Text.translatable("text.sfcr.disabled") :
										Text.translatable("text.sfcr.second", value)
								)
								.setTooltip(Text.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
								.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
								.build())
						//cloud refresh speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Text.translatable("text.sfcr.option.cloudRefreshSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getNormalRefreshSpeed())
								.setDefaultValue(CloudRefreshSpeed.SLOW)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Text.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setNormalRefreshSpeed)
								.build())
						//weather refresh speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Text.translatable("text.sfcr.option.weatherRefreshSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getWeatherRefreshSpeed())
								.setDefaultValue(CloudRefreshSpeed.FAST)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Text.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
								.build())
						//density changing speed
						.addEntry(builder.entryBuilder()
								.startEnumSelector(Text.translatable("text.sfcr.option.densityChangingSpeed")
										, CloudRefreshSpeed.class
										, CONFIG.getDensityChangingSpeed())
								.setDefaultValue(CloudRefreshSpeed.SLOW)
								.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
								.setTooltip(Text.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
								.setSaveConsumer(CONFIG::setDensityChangingSpeed)
								.build())
						//smooth change
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableSmoothChange")
										, CONFIG.isEnableSmoothChange())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.enableSmoothChange.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableSmoothChange)
								.setRequirement(Requirement.isTrue(() -> false))
								.build())
						//precipitation info
						.addEntry(builder.entryBuilder()
								.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
								.build())
						//snow
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.snowDensity")
										, CONFIG.getSnowDensity()
										,0
										,100)
								.setDefaultValue(60)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setSnowDensity)
								.build())
						//rain
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
										, CONFIG.getRainDensity()
										,0
										,100)
								.setDefaultValue(90)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setRainDensity)
								.build())
						//none
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.noneDensity")
										, CONFIG.getNoneDensity()
										,0
										,100)
								.setDefaultValue(0)
								.setTextGetter(value -> Text.of(value + "%"))
								.setSaveConsumer(CONFIG::setNoneDensity)
								.build())
						//biome density affect by chunk
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityByChunk")
										, CONFIG.isBiomeDensityByChunk())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeDensityByChunk)
								.build())
						//biome density detect loaded chunk
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
										, CONFIG.isBiomeDensityUseLoadedChunk())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeDensityUseLoadedChunk)
								.build())
						//biome filter
						.addEntry(builder.entryBuilder()
								.startStrList(Text.translatable("text.sfcr.option.biomeFilter")
										, CONFIG.getBiomeFilterList())
								.setDefaultValue(Config.DEF_BIOME_FILTER_LIST)
								.setTooltip(Text.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
								.setSaveConsumer(CONFIG::setBiomeFilterList)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Text.translatable("text.sfcr.category.fog"))
						//fog
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.enableFog")
										, CONFIG.isEnableFog())
								.setDefaultValue(true)
								.setTooltip(Text.translatable("text.sfcr.option.enableFog.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableFog)
								.build())
						//auto fog
						.addEntry(autoFog)
						//min fog
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.fogMinDistance")
										, CONFIG.getFogMinDistance()
										,1
										,32)
								.setDefaultValue(2)
								.setTextGetter(value -> Text.of(value.toString()))
								.setTooltip(Text.translatable("text.sfcr.option.fogMinDistance.@Tooltip"))
								.setSaveConsumer(newValue -> fogMin = newValue)
								.setDisplayRequirement(Requirement.isFalse(autoFog))
								.build())
						//max fog
						.addEntry(builder.entryBuilder()
								.startIntSlider(Text.translatable("text.sfcr.option.fogMaxDistance")
										, CONFIG.getFogMaxDistance()
										,1
										,32)
								.setDefaultValue(4)
								.setTextGetter(value -> Text.of(value.toString()))
								.setTooltip(Text.translatable("text.sfcr.option.fogMaxDistance.@Tooltip"))
								.setSaveConsumer(newValue -> fogMax = newValue)
								.setDisplayRequirement(Requirement.isFalse(autoFog))
								.build())
						//NO CLOUD NO RAIN
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.isCloudRain")
										, CONFIG.isEnableCloudRain())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.isCloudRain.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableCloudRain)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Text.translatable("text.sfcr.category.compat"))
						//custom dimension
						.addEntry(builder.entryBuilder()
								.startTextDescription(Text.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
										(Client.isCustomDimensionConfig ? "§a" : "§c") + dimensionName
								))
								.setTooltip(Text.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
								.build())
						//distant horizons
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.dHCompat"),
										CONFIG.isEnableDHCompat())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.dHCompat.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableDHCompat)
								.setRequirement(Requirement.isTrue(() -> Client.isDistantHorizonsLoaded))
								.build())
						//particle rain
						.addEntry(builder.entryBuilder()
								.startBooleanToggle(Text.translatable("text.sfcr.option.particleRainCompat"),
										CONFIG.isEnableParticleRainCompat())
								.setDefaultValue(false)
								.setTooltip(Text.translatable("text.sfcr.option.particleRainCompat.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableParticleRainCompat)
								.setRequirement(Requirement.isTrue(() -> Client.isParticleRainLoaded))
								.build()
						)
				)
				.build();
	}
}
