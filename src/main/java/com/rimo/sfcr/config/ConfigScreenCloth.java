package com.rimo.sfcr.config;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.Common;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreenCloth {
	ConfigBuilder builder = ConfigBuilder.create();
	ConfigEntryBuilder entryBuilder = builder.entryBuilder();

	ConfigCategory general = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.general"));
	ConfigCategory density = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.density"));
	ConfigCategory compat = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.compat"));

	private final Config CONFIG = Common.CONFIG;
	private final boolean oldEnableMod = CONFIG.isEnableMod();
	private final boolean oldDHCompat = CONFIG.isEnableDHCompat();
	private final boolean oldBottomDim = CONFIG.isEnableBottomDim();

	public Screen buildScreen() {
		builder.setParentScreen(Minecraft.getInstance().screen)
				.setTitle(Client.isCustomDimensionConfig ?
						Component.translatable("text.sfcr.option.title.customDimensionMode") :
						Component.translatable("text.sfcr.option.title")
				);
		buildCategory();
		buildDensityCategory();
		buildCompatCategory();

		// Saving...
		builder.setSavingRunnable(() -> {
			if (Client.isCustomDimensionConfig)
				Config.save(CONFIG, Minecraft.getInstance().level.dimension().identifier().toString());
			else
				Config.save(CONFIG);
			Client.applyConfigChange(oldEnableMod, oldDHCompat);
			if (Minecraft.getInstance().level != null && (oldEnableMod != CONFIG.isEnableMod() || oldBottomDim != CONFIG.isEnableBottomDim()))  //notify vanilla cloudRenderer to update
				Minecraft.getInstance().levelRenderer.getCloudRenderer().markForRebuild();
		});

		return builder.build();
	}

	private void buildCategory() {
		//pre build
		IntegerSliderEntry cloudHeightOffset = entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudHeight")
						, CONFIG.getCloudHeightOffset()
						//due to vanilla cloudCell byteBuffer maximum only 128, any height higher than 128 must be blocked.
						,-128
						,128)
				.setDefaultValue(0)
				.setTextGetter(value -> {
					if (value == 0)
						return Component.translatable("text.sfcr.option.followVanilla");
					return Component.nullToEmpty(value.toString());
				})
				.setTooltip(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudHeightOffset)
				.build();
		BooleanListEntry isFitToView = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
						, CONFIG.isEnableRenderDistanceFitToView())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableRenderDistanceFitToView)
				.build();

		//custom warning
		if (Client.isCustomDimensionConfig)
			general.addEntry(entryBuilder
					.startTextDescription(Component.translatable("text.sfcr.option.customDimensionMode.@PrefixText",
							"§b" + Minecraft.getInstance().level.dimension().identifier()
					))
					.build()
			);
		//enable
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableMod"),
						CONFIG.isEnableMod())
				.setDefaultValue(true)
						.setTooltip(Component.translatable("text.sfcr.option.enableMod.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableMod)
				.build()
		);
		//cloud height offset
		general.addEntry(cloudHeightOffset);
		//cloud layer height
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudLayerThickness")
						, CONFIG.getCloudThickness()
						,3
						,66)
				.setDefaultValue(34)
				.setTextGetter(value -> {
					cloudHeightOffset.setMaximum(128 - value + 2);
					return Component.nullToEmpty(String.valueOf(value - 2));
				})
				.setTooltip(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudThickness)
				.build()
		);
		//cloud distance
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudRenderDistance")
						, CONFIG.getRenderDistance()
						,31
						,128)
				.setDefaultValue(31)
				.setTextGetter(value -> {
					if (value == 31)
						return Component.translatable("text.sfcr.option.followVanilla").append(": " + Minecraft.getInstance().options.cloudRange().get());
					return Component.nullToEmpty(value.toString());
				})
				.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setRenderDistance)
				.setRequirement(Requirement.isFalse(isFitToView))
				.build()
		);
		//cloud distance fit to view
		general.addEntry(isFitToView);
		//cloud sample steps
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.sampleSteps")
						, CONFIG.getSampleSteps()
						,1
						,3)
				.setDefaultValue(2)
				.setTextGetter(value -> Component.nullToEmpty(value.toString()))
				.setTooltip(Component.translatable("text.sfcr.option.sampleSteps.@Tooltip"))
				.setSaveConsumer(CONFIG::setSampleSteps)
				.build()
		);
		//terrain dodge
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableTerrainDodge")
						, CONFIG.isEnableTerrainDodge())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableTerrainDodge)
				.build()
		);
		//cloud color
		general.addEntry(entryBuilder
				.startColorField(Component.translatable("text.sfcr.option.cloudColor")
						, CONFIG.getCloudColor())
				.setDefaultValue(0xFFFFFF)
				.setSaveConsumer(CONFIG::setCloudColor)
				.build()
		);
		//dusk blush
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableDuskBlush")
						, CONFIG.isEnableDuskBlush())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableDuskBlush.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDuskBlush)
				.build()
		);
		//bottomDim
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableBottomDim")
						, CONFIG.isEnableBottomDim())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableBottomDim.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBottomDim)
				.build()
		);
		//debug
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableDebug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build()
		);
	}

	private void buildDensityCategory() {
		// threshold
		density.addEntry(entryBuilder
				.startFloatField(Component.translatable("text.sfcr.option.densityThreshold")
						, CONFIG.getDensityThreshold())
				.setDefaultValue(1.3f)
				.setMax(2f)
				.setMin(-1f)
				.setTooltip(Component.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityThreshold)
				.build()
		);
		// threshold multiplier
		density.addEntry(entryBuilder
				.startFloatField(Component.translatable("text.sfcr.option.thresholdMultiplier")
						, CONFIG.getThresholdMultiplier())
				.setDefaultValue(1.5f)
				.setMax(3f)
				.setMin(0f)
				.setTooltip(Component.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
				.setSaveConsumer(CONFIG::setThresholdMultiplier)
				.build()
		);
		//dynamic
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableDynamic")
						, CONFIG.isEnableDynamic())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableDynamic.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDynamic)
				.build()
		);
		//density
		density.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
				.build()
		);
		//cloud common density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudDensity")
						, CONFIG.getDensityPercent()
						,0
						,100)
				.setDefaultValue(25)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setDensityPercent)
				.build()
		);
		//weather
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableWeatherDensity")
						, CONFIG.isEnableWeatherDensity())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableWeatherDensity)
				.build()
		);
		//rain density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.rainDensity")
						, CONFIG.getRainDensityPercent()
						,0
						,100)
				.setDefaultValue(60)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setRainDensityPercent)
				.build()
		);
		//thunder density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.thunderDensity")
						, CONFIG.getThunderDensityPercent()
						,0
						,100)
				.setDefaultValue(90)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setThunderDensityPercent)
				.build()
		);
		//weather pre-detect time
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.weatherPreDetectTime")
						, CONFIG.getWeatherPreDetectTime()
						,0
						,30)
				.setDefaultValue(10)
				.setTextGetter(value -> {
					if (value == 0)
						return Component.translatable("text.sfcr.disabled");
					return Component.translatable("text.sfcr.second", value);
				})
				.setTooltip(Component.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
				.build()
		);
		//cloud refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.cloudRefreshSpeed")
						, RefreshSpeed.class
						, CONFIG.getRefreshSpeed())
				.setDefaultValue(RefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setRefreshSpeed)
				.build()
		);
		//weather refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.weatherRefreshSpeed")
						, RefreshSpeed.class
						, CONFIG.getWeatherRefreshSpeed())
				.setDefaultValue(RefreshSpeed.FAST)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
				.build()
		);
		//density changing speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.densityChangingSpeed")
						, RefreshSpeed.class
						, CONFIG.getDensityChangingSpeed())
				.setDefaultValue(RefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((RefreshSpeed)value).getStringKey())
				.setTooltip(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityChangingSpeed)
				.build()
		);
		//biome detect
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.biomeDensityMultiplier")
						, CONFIG.getBiomeDensityPercent()
						,0
						,100)
				.setDefaultValue(50)
				.setTextGetter(value -> {
					if (value == 0)
						return Component.translatable("text.sfcr.disabled");
					return Component.nullToEmpty(value + "%");
				})
				.setTooltip(Component.translatable("text.sfcr.option.biomeDensityMultiplier.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityPercent)
				.build()
		);
		//biome density affected by chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityByChunk")
						, CONFIG.isEnableBiomeDensityByChunk())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBiomeDensityByChunk)
				.build()
		);
		//biome density detect loaded chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
						, CONFIG.isEnableBiomeDensityUseLoadedChunk())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableBiomeDensityUseLoadedChunk)
				.build()
		);
		//biome filter
		density.addEntry(entryBuilder
				.startStrList(Component.translatable("text.sfcr.option.biomeFilter")
						, CONFIG.getBiomeBlackList())
				.setDefaultValue(Config.DEF_BIOME_BLACKLIST)
				.setTooltip(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeBlackList)
				.build()
		);
	}

	private void buildCompatCategory() {
		//Custom Dimension
		compat.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.sfcr.option.dimensionCompat.@PrefixText",
						Minecraft.getInstance().level != null ?
								(Client.isCustomDimensionConfig ? "§a" : "§c") + Minecraft.getInstance().level.dimension().identifier() :
								"§7null"
				))
				.setTooltip(Component.translatable("text.sfcr.option.dimensionCompat.@Tooltip"))
				.build()
		);
		//Distant Horizons
		BooleanListEntry dhCompatEntry = entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.DHCompat"),
						CONFIG.isEnableDHCompat())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.DHCompat.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDHCompat)
				.build();
		if (Client.isDistantHorizonsLoaded)
			compat.addEntry(dhCompatEntry);
		//dh render distance
		compat.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance"),
						CONFIG.getDhDistanceMultipler(),
						1,
						8)
				.setDefaultValue(1)
				.setTextGetter(value -> Component.nullToEmpty(value + "x"))
				.setTooltip(Component.translatable("text.sfcr.option.DHCompat.enhanceDistance.@Tooltip"))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.setSaveConsumer(CONFIG::setDhDistanceMultipler)
				.build()
		);
		//dh height enhance
		compat.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.DHCompat.enhanceHeight"),
						CONFIG.getDhHeightEnhance() / 16,
						0,
						32)
				.setDefaultValue(0)
				.setTextGetter(value -> Component.nullToEmpty(String.valueOf(value * 16)))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.setSaveConsumer(value -> CONFIG.setDhHeightEnhance(value * 16))
				.build()
		);
		compat.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHChunk",
//						CONFIG.isEnableBiomeDensityByChunk() ?
//							Component.translatable("text.cloth-config.boolean.value.true") :
							Component.translatable("text.cloth-config.boolean.value.false")
				))
				.setTooltip(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHChunk.@Tooltip"))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.build()
		);
		compat.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHLoadedChunk",
//						CONFIG.isEnableBiomeDensityUseLoadedChunk() ?
//							Component.translatable("text.cloth-config.boolean.value.true") :
							Component.translatable("text.cloth-config.boolean.value.false")
				))
				.setTooltip(Component.translatable("text.sfcr.option.DHCompat.detectBiomeByDHChunk.@Tooltip"))
				.setDisplayRequirement(Requirement.isTrue(dhCompatEntry))
				.build()
		);
	}

}
