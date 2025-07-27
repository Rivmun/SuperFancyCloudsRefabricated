package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {
	ConfigBuilder builder = ConfigBuilder.create();
	ConfigEntryBuilder entryBuilder = builder.entryBuilder();

	ConfigCategory general = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.general"));
	ConfigCategory density = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.density"));
	ConfigCategory compat = builder.getOrCreateCategory(Text.translatable("text.sfcr.category.compat"));

	private final Config CONFIG = Common.CONFIG;
	private final boolean oldEnableMod = CONFIG.isEnableMod();
	private final boolean oldDHCompat = CONFIG.isEnableDHCompat();
	private final boolean oldBottomDim = CONFIG.isEnableBottomDim();

	public Screen buildScreen() {
		builder.setParentScreen(MinecraftClient.getInstance().currentScreen)
				.setTitle(Client.isCustomDimensionConfig ?
						Text.translatable("text.sfcr.option.title.customDimensionMode") :
						Text.translatable("text.sfcr.option.title")
				);
		buildCategory();
		buildDensityCategory();
		buildCompatCategory();

		// Saving...
		builder.setSavingRunnable(() -> {
			if (Client.isCustomDimensionConfig)
				Config.save(CONFIG, MinecraftClient.getInstance().world.getRegistryKey().getValue().toString());
			else
				Config.save(CONFIG);
			Client.applyConfigChange(oldEnableMod, oldDHCompat);
			if (MinecraftClient.getInstance().world != null && (oldEnableMod != CONFIG.isEnableMod() || oldBottomDim != CONFIG.isEnableBottomDim()))  //notify vanilla cloudRenderer to update
				MinecraftClient.getInstance().worldRenderer.getCloudRenderer().scheduleTerrainUpdate();
		});

		return builder.build();
	}

	private void buildCategory() {
		if (Client.isCustomDimensionConfig)
			general.addEntry(entryBuilder
					.startTextDescription(Text.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
							"§b" + MinecraftClient.getInstance().world.getRegistryKey().getValue().toString()
					))
					.build()
			);
		//enable
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableMod"),
						CONFIG.isEnableMod())
				.setDefaultValue(true)
				.setSaveConsumer(CONFIG::setEnableMod)
				.build()
		);
		//cloud height offset
		general.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudHeight")
						, CONFIG.getCloudHeightOffset()
						,-128
						,128)
				.setDefaultValue(0)
				.setTextGetter(value -> {
					if (value == 0)
						return Text.translatable("text.sfcr.option.followVanilla");
					return Text.of(value.toString());
				})
				.setTooltip(Text.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudHeightOffset)
				.build()
		);
		//cloud layer height
		general.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudLayerThickness")
						, CONFIG.getCloudLayerHeight()
						,3
						,66)
				.setDefaultValue(10)
				.setTextGetter(value -> Text.of(String.valueOf(value - 2)))
				.setTooltip(Text.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudLayerHeight)
				.build()
		);
		BooleanListEntry isFitToView = entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
						, CONFIG.isEnableRenderDistanceFitToView())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableRenderDistanceFitToView)
				.build();
		//cloud distance
		general.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.cloudRenderDistance")
						, CONFIG.getRenderDistance()
						,31
						,128)
				.setDefaultValue(48)
				.setTextGetter(value -> {
					if (value == 31)
						return Text.translatable("text.sfcr.option.followVanilla").append(": " + MinecraftClient.getInstance().options.getCloudRenderDistance().getValue());
					return Text.of(value.toString());
				})
				.setTooltip(Text.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setRenderDistance)
				.setRequirement(Requirement.isFalse(isFitToView))
				.build()
		);
		//cloud distance fit to view
		general.addEntry(isFitToView);
		//cloud sample steps
		general.addEntry(entryBuilder
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
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableTerrainDodge")
						, CONFIG.isEnableTerrainDodge())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableTerrainDodge)
				.build()
		);
		//cloud color
		general.addEntry(entryBuilder
				.startColorField(Text.translatable("text.sfcr.option.cloudColor")
						, CONFIG.getCloudColor())
				.setDefaultValue(0xFFFFFF)
				.setSaveConsumer(CONFIG::setCloudColor)
				.build()
		);
		//dusk blush
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableDuskBlush")
						, CONFIG.isEnableDuskBlush())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableDuskBlush.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDuskBlush)
				.build()
		);
		//bottomDim
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableBottomDim")
						, CONFIG.isEnableBottomDim())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableBottomDim.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBottomDim)
				.build()
		);
		//debug
		general.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableDebug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build()
		);
	}

	private void buildDensityCategory() {
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
		//dynamic
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableDynamic")
						, CONFIG.isEnableDynamic())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableDynamic.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDynamic)
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
						, CONFIG.getDensityPercent()
						,0
						,100)
				.setDefaultValue(25)
				.setTextGetter(value -> Text.of(value + "%"))
				.setSaveConsumer(CONFIG::setDensityPercent)
				.build()
		);
		//weather
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.enableWeatherDensity")
						, CONFIG.isEnableWeatherDensity())
				.setDefaultValue(true)
				.setTooltip(Text.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableWeatherDensity)
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
				.setTextGetter(value -> {
					if (value == 0)
						return Text.translatable("text.sfcr.disabled");
					return Text.translatable("text.sfcr.second", value);
				})
				.setTooltip(Text.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
				.build()
		);
		//cloud refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.cloudRefreshSpeed")
						, RefreshSpeed.class
						, CONFIG.getRefreshSpeed())
				.setDefaultValue(RefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Text.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setRefreshSpeed)
				.build()
		);
		//weather refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.weatherRefreshSpeed")
						, RefreshSpeed.class
						, CONFIG.getWeatherRefreshSpeed())
				.setDefaultValue(RefreshSpeed.FAST)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Text.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
				.build()
		);
		//density changing speed
		density.addEntry(entryBuilder
				.startEnumSelector(Text.translatable("text.sfcr.option.densityChangingSpeed")
						, RefreshSpeed.class
						, CONFIG.getDensityChangingSpeed())
				.setDefaultValue(RefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Text.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityChangingSpeed)
				.build()
		);
		//biome detect
		density.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.biomeDensityMultiplier")
						, CONFIG.getBiomeDensityPercent()
						,0
						,100)
				.setDefaultValue(50)
				.setTextGetter(value -> {
					if (value == 0)
						return Text.translatable("text.sfcr.disabled");
					return Text.of(value + "%");
				})
				.setTooltip(Text.translatable("text.sfcr.option.biomeDensityMultiplier.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityPercent)
				.build()
		);
		//biome density affected by chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityByChunk")
						, CONFIG.isEnableBiomeDensityByChunk())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBiomeDensityByChunk)
				.build()
		);
		//biome density detect loaded chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
						, CONFIG.isEnableBiomeDensityUseLoadedChunk())
				.setDefaultValue(false)
				.setTooltip(Text.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBiomeDensityUseLoadedChunk)
				.build()
		);
		//biome filter
		density.addEntry(entryBuilder
				.startStrList(Text.translatable("text.sfcr.option.biomeFilter")
						, CONFIG.getBiomeBlackList())
				.setDefaultValue(Config.DEF_BIOME_BLACKLIST)
				.setTooltip(Text.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeBlackList)
				.build()
		);
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
		compat.addEntry(dhCompatEntry);
		//dh render distance
		compat.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.DHCompat.enhanceDistance"),
						CONFIG.getDhDistanceMultipler(),
						1,
						8)
				.setDefaultValue(1)
				.setTextGetter(value -> Text.of(value + "x"))
				.setTooltip(Text.translatable("text.sfcr.option.DHCompat.enhanceDistance.@Tooltip"))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.setSaveConsumer(CONFIG::setDhDistanceMultipler)
				.build()
		);
		//dh height enhance
		compat.addEntry(entryBuilder
				.startIntSlider(Text.translatable("text.sfcr.option.DHCompat.enhanceHeight"),
						CONFIG.getDhHeightEnhance() / 16,
						0,
						32)
				.setDefaultValue(0)
				.setTextGetter(value -> Text.of(String.valueOf(value * 16)))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.setSaveConsumer(value -> CONFIG.setDhHeightEnhance(value * 16))
				.build()
		);
		//Custom Dimension
		compat.addEntry(entryBuilder
				.startTextDescription(Text.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
						MinecraftClient.getInstance().world != null ?
								(Client.isCustomDimensionConfig ? "§a" : "§c") + MinecraftClient.getInstance().world.getRegistryKey().getValue().toString() :
								"§7null"
				))
				.setTooltip(Text.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
				.build()
		);
	}

}
