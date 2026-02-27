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

import static com.rimo.sfcr.Client.applyConfigChange;
import static com.rimo.sfcr.Common.CONFIG;
import static com.rimo.sfcr.Common.DATA;

public class ConfigScreen {

	ConfigBuilder builder = ConfigBuilder.create();
	ConfigEntryBuilder entryBuilder = builder.entryBuilder();

	ConfigCategory general = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.general"));
	ConfigCategory clouds = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.clouds"));
	ConfigCategory fog = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.fog"));
	ConfigCategory density = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.density"));
	ConfigCategory compat = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.compat"));

	private int fogMin, fogMax;
	private final boolean oldEnableMod = CONFIG.isEnableRender();
	private final boolean oldEnableDHCompat = CONFIG.isEnableDHCompat();
	private String dimensionName;

	public Screen buildScreen() {
		ClientWorld world = MinecraftClient.getInstance().world;
		dimensionName = world != null ? world.getRegistryKey().getValue().toString() : "null";

		builder.setParentScreen(MinecraftClient.getInstance().currentScreen)
				.setTitle(Client.isCustomDimensionConfig ?
						Text.translatable("text.sfcr.title.customDimensionMode", dimensionName) :
						Text.translatable("text.sfcr.title")
				);
		buildGeneralCategory();
		buildCloudsCategory();
		buildFogCategory();
		buildDensityCategory();
		buildCompatCategory();
		builder.setTransparentBackground(true);

		//Update when saving
		builder.setSavingRunnable(() -> {
			if (CONFIG.isCloudRenderDistanceFitToView())
				CONFIG.setCloudRenderDistance(MinecraftClient.getInstance().options.getClampedViewDistance() * 12);
			CONFIG.setFogDisance(fogMin, fogMax);

			//Update config
			if (Client.isCustomDimensionConfig) {
				CONFIG.save(dimensionName);
			} else {
				CONFIG.save();
			}
			Common.clearConfigCache(dimensionName);
			DATA.setConfig(CONFIG);
			applyConfigChange(oldEnableMod, oldEnableDHCompat);
		});

		return builder.build();
	}

	private void buildCompatCategory() {
		//Distant Horizons
		BooleanListEntry dhCompatEntry = entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.DHCompat"),
						CONFIG.isEnableDHCompat())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.DHCompat.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDHCompat)
				.build();
		//Custom Dimension
		compat.addEntry(entryBuilder
				.startTextDescription(Text.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
						(Client.isCustomDimensionConfig ? "§a" : "§c") + dimensionName
				))
				.setTooltip(Text.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
				.build()
		);
		if (Client.isDistantHorizonsLoaded)
			compat.addEntry(dhCompatEntry);
	}

	private void buildGeneralCategory() {
		//custom warning
		if (Client.isCustomDimensionConfig)
			general.addEntry(entryBuilder
					.startTextDescription(Text.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
							"§b" + dimensionName
					))
					.build()
			);
		//override warning
		if (Client.isConfigHasBeenOverride)
			general.addEntry(entryBuilder
					.startTextDescription(Text.translatable("text.sfcr.option.configHasBeenOverride.@PrefixText"))
					.build()
			);
		//enabled render
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableMod")
						, CONFIG.isEnableRender())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableMod.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableRender)
				.build()
		);
		//enable server
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableServer")
						, CONFIG.isEnableServer())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.enableServer.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableServer)
				.build()
		);
		//cull mode
		EnumListEntry<CullMode> cullMode = entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.cullMode")
						, CullMode.class
						, CONFIG.getCullMode())
				.setDefaultValue(CullMode.RECTANGULAR)
				.setEnumNameProvider(value -> ((CullMode) value).getName())
				.setTooltip(Text.translatable("text.sfcr.option.cullMode.@Tooltip"))
				.setSaveConsumer(CONFIG::setCullMode)
				.build();
		general.addEntry(cullMode);
		//cull radian multiplier
		general.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cullRadianMultiplier")
						,(int) (CONFIG.getCullRadianMultiplier() * 10)
						,5
						,15)
				.setDefaultValue(10)
				.setTextGetter(value -> Text.of(value / 10f + "x"))
				.setTooltip(Text.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip"))
				.setDisplayRequirement(Requirement.isValue(cullMode, CullMode.CIRCULAR, CullMode.RECTANGULAR))
				.setSaveConsumer(value -> CONFIG.setCullRadianMultiplier(value / 10f))
				.build()
		);
		//remesh interval
		general.addEntry(entryBuilder
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
				.build()
		);
		//DEBUG
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.debug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.debug.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build()
		);
	}

	private void buildCloudsCategory() {
		//cloud height
		clouds.addEntry(entryBuilder
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
				.build()
		);
		//cloud block size
		clouds.addEntry(entryBuilder
				.startDropdownMenu(Text.translatable("text.sfcr.option.cloudBlockSize")
						,TopCellElementBuilder.of(CONFIG.getCloudBlockSize(), Integer::parseInt))
				.setDefaultValue(12)
				.setSuggestionMode(false)
				.setSelections(List.of(2, 4, 8, 12, 16))
				.setTooltip(Text.translatable("text.sfcr.option.cloudBlockSize.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudBlockSize)
				.build()
		);
		//cloud thickness
		clouds.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudLayerThickness")
						, CONFIG.getCloudLayerThickness()
						,3
						,66)
				.setDefaultValue(10)
				.setTextGetter(value -> Text.of(String.valueOf(value - 2)))
				.setTooltip(Text.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudLayerThickness)
				.build()
		);
		//cloud distance
		clouds.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudRenderDistance")
						, CONFIG.getCloudRenderDistance()
						,32
						,256)
				.setDefaultValue(64)
				.setTextGetter(value -> Text.of(value.toString()))
				.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudRenderDistance)
				.build()
		);
		//cloud distance fit to view
		clouds.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
						, CONFIG.isCloudRenderDistanceFitToView())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudRenderDistanceFitToView)
				.build()
		);
		//cloud sample steps
		clouds.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.sampleSteps")
						, CONFIG.getSampleSteps()
						,1
						,3)
				.setDefaultValue(2)
				.setTextGetter(value -> Text.of(value.toString()))
				.setTooltip(Text.translatable("text.sfcr.option.sampleSteps.@Tooltip"))
				.setSaveConsumer(CONFIG::setSampleSteps)
				.build()
		);
		//terrain dodge
		clouds.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableTerrainDodge")
						, CONFIG.isEnableTerrainDodge())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableTerrainDodge)
				.build()
		);
		//cloud color
		clouds.addEntry(entryBuilder
				.startColorField(Text.translatable("text.sfcr.option.cloudColor")
						, CONFIG.getCloudColor())
				.setDefaultValue(0xFFFFFF)
				.setSaveConsumer(CONFIG::setCloudColor)
				.build()
		);
		//cloud bright multiplier
		clouds.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudBright")
						, (int) (CONFIG.getCloudBrightMultiplier() * 10)
						, 0
						, 10)
				.setDefaultValue(1)
				.setTextGetter(value -> Text.of(value * 10 + "%"))
				.setSaveConsumer(value -> CONFIG.setCloudBrightMultiplier(value / 10f))
				.build()
		);
	}

	private void buildFogCategory() {
		BooleanListEntry autoFog = entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.fogAutoDistance")
						, CONFIG.isFogAutoDistance())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.fogAutoDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setFogAutoDistance)
				.build();
		//fog enable
		fog.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableFog")
						, CONFIG.isEnableFog())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableFog.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableFog)
				.build()
		);
		//fog auto distance
		fog.addEntry(autoFog);
		//fog min dist.
		fog.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.fogMinDistance")
						, CONFIG.getFogMinDistance()
						,1
						,32)
				.setDefaultValue(2)
				.setTextGetter(value -> Text.of(value.toString()))
				.setTooltip(Text.translatable("text.sfcr.option.fogMinDistance.@Tooltip"))
				.setSaveConsumer(newValue -> fogMin = newValue)
				.setDisplayRequirement(Requirement.isFalse(autoFog))
				.build()
		);
		//fog max dist.
		fog.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.fogMaxDistance")
						, CONFIG.getFogMaxDistance()
						,1
						,32)
				.setDefaultValue(4)
				.setTextGetter(value -> Text.of(value.toString()))
				.setTooltip(Text.translatable("text.sfcr.option.fogMaxDistance.@Tooltip"))
				.setSaveConsumer(newValue -> fogMax = newValue)
				.setDisplayRequirement(Requirement.isFalse(autoFog))
				.build()
		);
	}

	private void buildDensityCategory() {
		//weather
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableWeatherDensity")
						, CONFIG.isEnableWeatherDensity())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableWeatherDensity)
				.build()
		);
		// threshold
		density.addEntry(entryBuilder
				.startFloatField(Text.translatable("text.sfcr.option.densityThreshold")
						, CONFIG.getDensityThreshold())
				.setDefaultValue(1.3f)
				.setMax(2f)
				.setMin(-1f)
				.setTooltip(Text.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityThreshold)
				.build()
		);
		// threshold multiplier
		density.addEntry(entryBuilder
				.startFloatField(Text.translatable("text.sfcr.option.thresholdMultiplier")
						, CONFIG.getThresholdMultiplier())
				.setDefaultValue(1.5f)
				.setMax(3f)
				.setMin(0f)
				.setTooltip(Text.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
				.setSaveConsumer(CONFIG::setThresholdMultiplier)
				.build()
		);
		//density
		density.addEntry(entryBuilder
				.startTextDescription(Text.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
				.build()
		);
		//cloud common density
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudDensity")
						, CONFIG.getCloudDensityPercent()
						,0
						,100)
				.setDefaultValue(25)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setCloudDensityPercent)
				.build()
		);
		//rain density
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.rainDensity")
						, CONFIG.getRainDensityPercent()
						,0
						,100)
				.setDefaultValue(60)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setRainDensityPercent)
				.build()
		);
		//thunder density
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.thunderDensity")
						, CONFIG.getThunderDensityPercent()
						,0
						,100)
				.setDefaultValue(90)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setThunderDensityPercent)
				.build()
		);
		//weather pre-detect time
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.weatherPreDetectTime")
						, CONFIG.getWeatherPreDetectTime()
						,0
						,30)
				.setDefaultValue(10)
				.setTextGetter(value -> value == 0 ?
						Text.translatable("text.sfcr.disabled") :
						Text.translatable("text.sfcr.second", value)
				)
				.setTooltip(Text.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
				.build()
		);
		//cloud refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.cloudRefreshSpeed")
						, CloudRefreshSpeed.class
						, CONFIG.getNormalRefreshSpeed())
				.setDefaultValue(CloudRefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Text.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setNormalRefreshSpeed)
				.build()
		);
		//weather refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.weatherRefreshSpeed")
						, CloudRefreshSpeed.class
						, CONFIG.getWeatherRefreshSpeed())
				.setDefaultValue(CloudRefreshSpeed.FAST)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Text.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
				.build()
		);
		//density changing speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.densityChangingSpeed")
						, CloudRefreshSpeed.class
						, CONFIG.getDensityChangingSpeed())
				.setDefaultValue(CloudRefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Text.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityChangingSpeed)
				.build()
		);
		//smooth change
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableSmoothChange")
						, CONFIG.isEnableSmoothChange())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.enableSmoothChange.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableSmoothChange)
				.setRequirement(Requirement.isTrue(() -> false))
				.build()
		);
		//precipitation info
		density.addEntry(entryBuilder
				.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
				.build()
		);
		//snow
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.snowDensity")
						, CONFIG.getSnowDensity()
						,0
						,100)
				.setDefaultValue(60)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setSnowDensity)
				.build()
		);
		//rain
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
						, CONFIG.getRainDensity()
						,0
						,100)
				.setDefaultValue(90)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setRainDensity)
				.build()
		);
		//none
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.noneDensity")
						, CONFIG.getNoneDensity()
						,0
						,100)
				.setDefaultValue(0)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setNoneDensity)
				.build()
		);
		//biome density affect by chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityByChunk")
						, CONFIG.isBiomeDensityByChunk())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityByChunk)
				.build()
		);
		//biome density detect loaded chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
						, CONFIG.isBiomeDensityUseLoadedChunk())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityUseLoadedChunk)
				.build()
		);
		//biome filter
		density.addEntry(entryBuilder
				.startStrList(Text.translatable("text.sfcr.option.biomeFilter")
						, CONFIG.getBiomeFilterList())
				.setDefaultValue(Config.DEF_BIOME_FILTER_LIST)
				.setTooltip(Text.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeFilterList)
				.build()
		);
	}

}
