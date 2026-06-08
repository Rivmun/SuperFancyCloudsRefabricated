package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
//? if < 1.19
//import net.minecraft.network.chat.TranslatableComponent;

import java.util.Arrays;
import java.util.Optional;

import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

//~ if < 1.19 'Component.translatable' -> 'new TranslatableComponent' {
public class ConfigScreen {
	final ConfigBuilder builder = ConfigBuilder.create();
	final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
	final boolean oldEnableDHCompat = CONFIG.isEnableDHCompat();
	final String dimensionName;
	final boolean isCustomDimension;
	int fogMin, fogMax;

	public ConfigScreen() {
//		builder.setGlobalized(true);
//		builder.setGlobalizedExpanded(false);
		ClientLevel level = Minecraft.getInstance().level;
		dimensionName = level != null ? level.dimension().location().toString() : Config.OVERWORLD;
		isCustomDimension = ! dimensionName.equals(Config.OVERWORLD);
	}

	public Screen build() {
		//cull mode
		BooleanListEntry cullMode = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.cullMode"),
						CONFIG.getEnableViewCulling())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.cullMode.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableViewCulling)
				.build();
		//auto fog
		BooleanListEntry autoFog = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.fogAutoDistance")
						, CONFIG.isFogAutoDistance())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.fogAutoDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setFogAutoDistance)
				.build();
		//debug
		BooleanListEntry debug = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.debug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.debug.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build();
		//dynamic
		BooleanListEntry enableDynamic = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableWeatherDensity")
						, CONFIG.isEnableDynamic())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDynamic)
				.build();
		//distanceFitToView
		BooleanListEntry distanceFitToView = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
						, CONFIG.isCloudRenderDistanceFitToView())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudRenderDistanceFitToView)
				.build();
		BooleanListEntry ncnr = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.isCloudRain")
						, CONFIG.isEnableCloudRain())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.isCloudRain.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableCloudRain)
				.build();
		BooleanListEntry enableServer = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableServer")
						, CONFIG.isEnableServer())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableServer.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableServer)
				.build();
		BooleanListEntry deleteAfterQuit = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.deleteDimensionAfterQuit",
								Component.translatable("text.cloth-config.save_and_done")),
						false)
				.setDefaultValue(false)
				.setSaveConsumer(value -> {})
				.setDisplayRequirement(Requirement.isTrue(() -> Client.isCustomDimensionConfig))
				.build();
		DropdownBoxEntry<Integer> cloudBlockSize = entryBuilder
				.startDropdownMenu(Component.translatable("text.sfcr.option.cloudBlockSize")
						, DropdownMenuBuilder.TopCellElementBuilder.of(CONFIG.getCloudBlockSize(), Integer::parseInt))
				.setDefaultValue(12)
				.setSuggestionMode(false)
				.setSelections(Arrays.asList(2, 4, 8, 12, 16))
				.setTooltip(Component.translatable("text.sfcr.option.cloudBlockSize.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudBlockSize)
				.build();
		IntegerSliderEntry cloudHeight = entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudHeight")
						, CONFIG.getCloudHeight() < 0 ? -1 : CONFIG.getCloudHeight() / cloudBlockSize.getValue() * 2
						, - 1
						, 384 / cloudBlockSize.getValue() * 2)
				.setDefaultValue(- 1)
				.setTextGetter(value -> value < 0 ?
						Component.translatable("text.sfcr.option.cloudHeight.followVanilla") :
						Component.nullToEmpty(String.valueOf(value * cloudBlockSize.getValue() / 2))
				)
				.setTooltip(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
				.setSaveConsumer(value -> CONFIG.setCloudHeight(value < 0 ? value : value * cloudBlockSize.getValue() / 2))
				.build();
		BooleanListEntry dHCompat = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.dHCompat"),
						CONFIG.isEnableDHCompat())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.dHCompat.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDHCompat)
				.setRequirement(Requirement.isTrue(() -> Client.isDistantHorizonsLoaded))
				.build();
		// (i love it...
		return builder.setParentScreen(Minecraft.getInstance().screen)
				.setTransparentBackground(true)
				.setTitle(isCustomDimension ?
						Component.translatable("text.sfcr.title.customDimensionMode", dimensionName) :
						Component.translatable("text.sfcr.title")
				)
				.setSavingRunnable(() -> {
					if (deleteAfterQuit.getValue()) {
						Config.delete(dimensionName);
						Common.setDimensionConfigJson(dimensionName, "");
						CONFIG.load();
						Client.isCustomDimensionConfig = false;
					} else {
						if (CONFIG.isCloudRenderDistanceFitToView())
							//~ if ! 1.16.5 '.renderDistance' -> '.getEffectiveRenderDistance()'
							CONFIG.setCloudRenderDistance(Minecraft.getInstance().options.getEffectiveRenderDistance() * 12);
						CONFIG.setFogDistance(fogMin, fogMax);
						Common.setDimensionConfigJson(dimensionName, CONFIG.toString());
						CONFIG.save(dimensionName);
						if (isCustomDimension)
							Client.isCustomDimensionConfig = true;
					}
					DATA.setConfig(CONFIG);
					Client.applyConfigChange(oldEnableDHCompat);
				})
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.general"))
						// Custom Dimension Warning
						.addEntry(entryBuilder
								.startTextDescription(Component.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
										"§b" + dimensionName
								))
								.setTooltip(Component.translatable("text.sfcr.option.customDimensionMode.@Tooltip"))
								.setDisplayRequirement(Requirement.isTrue(() -> isCustomDimension))
								.build())
						// Config Override Warning
						.addEntry(entryBuilder
								.startTextDescription(Component.translatable("text.sfcr.option.configHasBeenOverride.@PrefixText"))
								.setDisplayRequirement(Requirement.isTrue(() -> Client.isConfigHasBeenOverride))
								.build())
						// enable cloud
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableMod")
										, CONFIG.isEnableRender())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableMod.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableRender)
								.build())
						//enable server
						.addEntry(enableServer)
						//cull mode
						.addEntry(cullMode)
						//cull radian multiplier
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.cullRadianMultiplier")
										,(int) (CONFIG.getCullRadianMultiplier() * 10)
										,5
										,15)
								.setDefaultValue(11)
								.setTextGetter(value -> Component.nullToEmpty(value / 10f + "x"))
								.setTooltip(Component.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip"))
								.setDisplayRequirement(Requirement.isTrue(cullMode))
								.setSaveConsumer(value -> CONFIG.setCullRadianMultiplier(value / 10f))
								.build())
						//remesh interval
						.addEntry(entryBuilder
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
						.addEntry(cloudHeight)
						//force rendering
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.forceRendering"),
										CONFIG.isForceRendering())
								.setDefaultValue(false)
								.setSaveConsumer(CONFIG::setForceRendering)
								.setTooltip(Component.translatable("text.sfcr.option.forceRendering.@Tooltip"))
								.setRequirement(Requirement.isTrue(() -> cloudHeight.getValue() >= 0))
								.build())
						//cloud block size
						.addEntry(cloudBlockSize)
						//cloud thickness
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.cloudLayerThickness")
										, CONFIG.getCloudLayerThickness()
										,2
										,65)
								.setDefaultValue(9)
								.setTextGetter(value -> Component.nullToEmpty(String.valueOf(value - 1)))
								.setTooltip(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudLayerThickness)
								.build())
						//cloud distance
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.cloudRenderDistance")
										, CONFIG.getCloudRenderDistance()
										,32
										,192)
								.setDefaultValue(64)
								.setTextGetter(value -> Component.nullToEmpty(value.toString()))
								.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRenderDistance)
								.setRequirement(Requirement.isFalse(distanceFitToView))
								.build())
						//cloud distance fit to view
						.addEntry(distanceFitToView)
						//cloud sample steps
						.addEntry(entryBuilder
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
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableTerrainDodge")
										, CONFIG.isEnableTerrainDodge())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableTerrainDodge)
								.build())
						//cloud color
						.addEntry(entryBuilder
								.startAlphaColorField(Component.translatable("text.sfcr.option.cloudColor")
										, CONFIG.getCloudColor())
								.setDefaultValue(0xFFFFFFFF)
								.setErrorSupplier(value -> {
									if (value >>> 24 <= 0x20)
										return Optional.of(Component.translatable("text.sfcr.colorAlphaTooLow"));
									return Optional.empty();
								})
								.setSaveConsumer(CONFIG::setCloudColor)
								.build())
						//cloud bright multiplier
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.cloudBright")
										, (int) (CONFIG.getCloudBrightMultiplier() * 10)
										, 0
										, 10)
								.setDefaultValue(1)
								.setTextGetter(value -> Component.nullToEmpty(value * 10 + "%"))
								.setSaveConsumer(value -> CONFIG.setCloudBrightMultiplier(value / 10f))
								.build())
						//dusk blush
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableDuskBlush")
										, CONFIG.isEnableDuskBlush())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableDuskBlush.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableDuskBlush)
								.build())
						//bottomDim
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableBottomDim")
										, CONFIG.isEnableBottomDim())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableBottomDim.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableBottomDim)
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.density"))
						// threshold
						.addEntry(entryBuilder
								.startFloatField(Component.translatable("text.sfcr.option.densityThreshold")
										, CONFIG.getDensityThreshold())
								.setDefaultValue(1.3f)
								.setMax(2f)
								.setMin(-1f)
								.setTooltip(Component.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
								.setSaveConsumer(CONFIG::setDensityThreshold)
								.build())
						// threshold multiplier
						.addEntry(entryBuilder
								.startFloatField(Component.translatable("text.sfcr.option.thresholdMultiplier")
										, CONFIG.getThresholdMaxReduction())
								.setDefaultValue(1.5f)
								.setMax(3f)
								.setMin(0f)
								.setTooltip(Component.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
								.setSaveConsumer(CONFIG::setThresholdMaxReduction)
								.build())
						//Dynamic
						.addEntry(enableDynamic)
						//weather group
						.addEntry(entryBuilder
								.startSubCategory(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"), Arrays.asList(
										//cloud common density
										entryBuilder
												.startIntSlider(Component.translatable("text.sfcr.option.cloudDensity")
														, CONFIG.getCloudDensityPercent()
														,0
														,100)
												.setDefaultValue(25)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setCloudDensityPercent)
												.build(),
										//rain density
										entryBuilder
												.startIntSlider(Component.translatable("text.sfcr.option.rainDensity")
														, CONFIG.getRainDensityPercent()
														,0
														,100)
												.setDefaultValue(60)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setRainDensityPercent)
												.build(),
										//thunder density
										entryBuilder
												.startIntSlider(Component.translatable("text.sfcr.option.thunderDensity")
														, CONFIG.getThunderDensityPercent()
														,0
														,100)
												.setDefaultValue(90)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setThunderDensityPercent)
												.build(),
										//night density
										entryBuilder
												.startIntSlider(Component.translatable("text.sfcr.option.densityAtNight"),
														(int) (CONFIG.getDensityAtNight() * 10),
														0,
														10)
												.setDefaultValue(7)
												.setTextGetter(value -> Component.nullToEmpty(value * 10 + "%"))
												.setSaveConsumer(value -> CONFIG.setDensityAtNight(value / 10f))
												.build(),
										//weather pre-detect time
										entryBuilder
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
												.build(),
										//cloud refresh speed
										entryBuilder
												.startEnumSelector(Component.translatable("text.sfcr.option.cloudRefreshSpeed")
														, CloudRefreshSpeed.class
														, CONFIG.getNormalRefreshSpeed())
												.setDefaultValue(CloudRefreshSpeed.SLOW)
												.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
												.setTooltip(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
												.setSaveConsumer(CONFIG::setNormalRefreshSpeed)
												.build(),
										//weather refresh speed
										entryBuilder
												.startEnumSelector(Component.translatable("text.sfcr.option.weatherRefreshSpeed")
														, CloudRefreshSpeed.class
														, CONFIG.getWeatherRefreshSpeed())
												.setDefaultValue(CloudRefreshSpeed.FAST)
												.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
												.setTooltip(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
												.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
												.build(),
										//density changing speed
										entryBuilder
												.startEnumSelector(Component.translatable("text.sfcr.option.densityChangingSpeed")
														, CloudRefreshSpeed.class
														, CONFIG.getDensityChangingSpeed())
												.setDefaultValue(CloudRefreshSpeed.SLOW)
												.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
												.setTooltip(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
												.setSaveConsumer(CONFIG::setDensityChangingSpeed)
												.build(),
										//smooth change
										entryBuilder
												.startBooleanToggle(Component.translatable("text.sfcr.option.enableSmoothChange")
														, CONFIG.isEnableSmoothChange())
												.setDefaultValue(false)
												.setTooltip(Component.translatable("text.sfcr.option.enableSmoothChange.@Tooltip"))
												.setSaveConsumer(CONFIG::setEnableSmoothChange)
												.setRequirement(Requirement.isTrue(debug))
												.build()
								))
								.setExpanded(true)
								.setDisplayRequirement(Requirement.isTrue(enableDynamic))
								.build())
						//biome group
						.addEntry(entryBuilder
								.startSubCategory(Component.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"), Arrays.asList(
										//? if > 1.20 {
										//snow
										entryBuilder
												.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.snowDensity")
														, CONFIG.getSnowDensity()
														,0
														,100)
												.setDefaultValue(60)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setSnowDensity)
												.build(),
										//rain
										entryBuilder
												.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
														, CONFIG.getRainDensity()
														,0
														,100)
												.setDefaultValue(90)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setRainDensity)
												.build(),
										//none
										entryBuilder
												.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.noneDensity")
														, CONFIG.getNoneDensity()
														,0
														,100)
												.setDefaultValue(0)
												.setTextGetter(value -> Component.nullToEmpty(value + "%"))
												.setSaveConsumer(CONFIG::setNoneDensity)
												.build(),
										//? }
										//biome density affect by chunk
										entryBuilder
												.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityByChunk")
														, CONFIG.isBiomeDensityByChunk())
												.setDefaultValue(false)
												.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
												.setSaveConsumer(CONFIG::setBiomeDensityByChunk)
												.build(),
										//biome filter
										entryBuilder
												.startStrList(Component.translatable("text.sfcr.option.biomeFilter")
														, CONFIG.getBiomeFilterList())
												.setDefaultValue(Config.DEF_BIOME_FILTER_LIST)
												.setTooltip(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
												.setSaveConsumer(CONFIG::setBiomeFilterList)
												.build()
								))
								.setExpanded(true)
								.setDisplayRequirement(Requirement.isTrue(enableDynamic))
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.fog"))
						//fog
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.enableFog")
										, CONFIG.isEnableFog())
								.setDefaultValue(true)
								.setTooltip(Component.translatable("text.sfcr.option.enableFog.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableFog)
								.build())
						//auto fog
						.addEntry(autoFog)
						//min fog
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.fogMinDistance")
										, CONFIG.getFogMinDistance()
										,1
										,32)
								.setDefaultValue(2)
								.setTextGetter(value -> Component.nullToEmpty(value.toString()))
								.setTooltip(Component.translatable("text.sfcr.option.fogMinDistance.@Tooltip"))
								.setSaveConsumer(newValue -> fogMin = newValue)
								.setDisplayRequirement(Requirement.isFalse(autoFog))
								.build())
						//max fog
						.addEntry(entryBuilder
								.startIntSlider(Component.translatable("text.sfcr.option.fogMaxDistance")
										, CONFIG.getFogMaxDistance()
										,1
										,32)
								.setDefaultValue(4)
								.setTextGetter(value -> Component.nullToEmpty(value.toString()))
								.setTooltip(Component.translatable("text.sfcr.option.fogMaxDistance.@Tooltip"))
								.setSaveConsumer(newValue -> fogMax = newValue)
								.setDisplayRequirement(Requirement.isFalse(autoFog))
								.build())
				)
				.setFallbackCategory(builder.getOrCreateCategory(Component.translatable("text.sfcr.category.compat"))
						//NO CLOUD NO RAIN
						.addEntry(ncnr)
						//NO CLOUD NO RAIN logically
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.cloudRainLogically"),
										CONFIG.isCloudRainLogically())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.cloudRainLogically.@Tooltip"))
								.setSaveConsumer(CONFIG::setCloudRainLogically)
								.setDisplayRequirement(Requirement.isTrue(ncnr))
								.setRequirement(Requirement.isTrue(enableServer))
								.build())
						//particle rain
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.particleRainCompat"),
										CONFIG.isEnableParticleRainCompat())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.particleRainCompat.@Tooltip"))
								.setSaveConsumer(CONFIG::setEnableParticleRainCompat)
								.setDisplayRequirement(Requirement.isTrue(ncnr))
								.setRequirement(Requirement.isTrue(() -> Client.isParticleRainLoaded))
								.build()
						)
						//custom dimension
						.addEntry(entryBuilder
								.startTextDescription(Component.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
										(Client.isCustomDimensionConfig ? "§a" : "§c") + dimensionName
								))
								.setTooltip(Component.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
								.build())
						//delete config after quit
						.addEntry(deleteAfterQuit)
						//distant horizons
						.addEntry(dHCompat)
						//threadify remeshing
						.addEntry(entryBuilder
								.startBooleanToggle(Component.translatable("text.sfcr.option.isThreadifyDHRemesh"),
										CONFIG.isThreadifyDHRemesh())
								.setDefaultValue(false)
								.setTooltip(Component.translatable("text.sfcr.option.isThreadifyDHRemesh.@Tooltip"))
								.setSaveConsumer(CONFIG::setThreadifyDHRemesh)
								.setDisplayRequirement(Requirement.isTrue(dHCompat))
								.build())
						//seasons
						.addEntry(entryBuilder
								.startStrList(Component.translatable("text.sfcr.option.seasonCompat", Common.seasonHandler != null ?
												Common.seasonHandler.getClass().getSimpleName() :
												"§4null"
										),
										CONFIG.getSeasonDensityPercentMap())
								.setDefaultValue(SharedConfig.DEF_SEASON_DENSITY_MAP)
								.setInsertButtonEnabled(false)
								.setDeleteButtonEnabled(false)
								.setTooltip(Component.translatable("text.sfcr.option.seasonCompat.@Tooltip"))
								.setSaveConsumer(CONFIG::setSeasonDensityPercentMap)
								.setErrorSupplier(str -> {
									try {
										Common.seasonHandler.castStringToDensityMap(str.get(0));
									} catch (IllegalArgumentException e) {
										return Optional.of(Component.nullToEmpty(e.getLocalizedMessage()));
									} catch (Exception ignored) {}
									return Optional.empty();
								})
								.setRequirement(Requirement.isTrue(() -> Common.seasonHandler != null))
								.build())
				)
				.build();
	}
}
//~ }
