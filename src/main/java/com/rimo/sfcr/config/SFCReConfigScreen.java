package com.rimo.sfcr.config;

import java.util.List;

import com.rimo.sfcr.SFCReClient;
import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.util.CloudRefreshSpeed;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class SFCReConfigScreen {
    
    ConfigBuilder builder = ConfigBuilder.create()
    		.setParentScreen(MinecraftClient.getInstance().currentScreen)
    		.setTitle(Text.translatable("text.autoconfig.sfcr.title"));
    ConfigEntryBuilder entryBuilder = builder.entryBuilder();
    
    ConfigCategory clouds = builder.getOrCreateCategory(Text.translatable("text.autoconfig.sfcr.category.clouds"));
    ConfigCategory fog = builder.getOrCreateCategory(Text.translatable("text.autoconfig.sfcr.category.fog"));
    ConfigCategory density = builder.getOrCreateCategory(Text.translatable("text.autoconfig.sfcr.category.density"));
    
    SFCReConfig config = SFCReMain.CONFIGHOLDER.getConfig();
    
    public Screen buildScreen() {
    	buildCloudsCategory();
    	buildFogCategory();
    	buildDensityCategory();
    	builder.setTransparentBackground(true);
    	
		//Update when saving
    	builder.setSavingRunnable(() -> {
    		SFCReMain.CONFIGHOLDER.save();
    		SFCReClient.RENDERER.updateRenderData(config);
    		if (config.isEnableServerConfig() && MinecraftClient.getInstance().player != null)
    			SFCReClient.sendSyncRequest(true);
    	});
    	
    	return builder.build();
    }
    
    private void buildCloudsCategory() {
    	//enabled
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableMod")
    					,config.isEnableMod())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableMod.@Tooltip"))
                .setSaveConsumer(config::setEnableMod)
                .build());
    	//server control
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableServer")
    					,config.isEnableServerConfig())
    			.setDefaultValue(false)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableServer.@Tooltip"))
    			.setSaveConsumer(config::setEnableServerConfig)
    			.build());
    	//cloud height
    	clouds.addEntry(entryBuilder
    			.startIntField(Text.translatable("text.autoconfig.sfcr.option.cloudHeight")
    					,config.getCloudHeight())
                .setDefaultValue(192)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudHeight.@Tooltip"))
                .setSaveConsumer(config::setCloudHeight)
                .build());
    	//cloud block size
    	clouds.addEntry(entryBuilder
    			.startDropdownMenu(Text.translatable("text.autoconfig.sfcr.option.cloudBlockSize")
    					,TopCellElementBuilder.of(config.getCloudBlockSize(), value -> {return Integer.parseInt(value);}))
    			.setDefaultValue(16)
    			.setSuggestionMode(false)
    			.setSelections(List.of(2, 4, 8, 16))
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudBlockSize.@Tooltip"))
    			.setSaveConsumer(config::setCloudBlockSize)
    			.build());
    	//cloud thickness
    	clouds.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudLayerThickness")
    					,config.getCloudLayerThickness()
    					,3
    					,64)
                .setDefaultValue(32)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudLayerThickness.@Tooltip"))
                .setSaveConsumer(config::setCloudLayerThickness)
                .build());
    	//cloud distance
    	clouds.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistance")
    					,config.getCloudRenderDistance()
    					,64
    					,384)
                .setDefaultValue(96)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistance.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistance)
                .build());
    	//cloud distance fit to view
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView")
    					,config.isCloudRenderDistanceFitToView())
                .setDefaultValue(false)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistanceFitToView)
                .build());
    	//cloud refresh speed
    	clouds.addEntry(entryBuilder
    			.startEnumSelector(Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed")
    					,CloudRefreshSpeed.class
    					,config.getNormalRefreshSpeed())
    			.setDefaultValue(CloudRefreshSpeed.SLOW)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.@Tooltip"))
    			.setSaveConsumer(config::setNormalRefreshSpeed)
    			.build());
    	//cloud sample steps
    	clouds.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.sampleSteps")
    					,config.getSampleSteps()
    					,1
    					,3)
    			.setDefaultValue(3)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.sampleSteps.@Tooltip"))
    			.setSaveConsumer(config::setSampleSteps)
    			.build());
    	//terrain dodge
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableTerrainDodge")
    					,config.isEnableTerrainDodge())
    			.setDefaultValue(true)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableTerrainDodge.@Tooltip"))
    			.setSaveConsumer(config::setEnableTerrainDodge)
    			.build());
    	//DEBUG
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.debug")
    					,config.isEnableDebug())
    			.setDefaultValue(false)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.debug.@Tooltip"))
    			.setSaveConsumer(config::setEnableDebug)
    			.build());
    }
    
    private void buildFogCategory() {
    	//fog enable
    	fog.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableFog")
    					,config.isEnableFog())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableFog.@Tooltip"))
                .setSaveConsumer(config::setEnableFog)
                .build());
    	//fog auto distance
    	fog.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.fogAutoDistance")
    					,config.isFogAutoDistance())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogAutoDistance.@Tooltip"))
                .setSaveConsumer(config::setFogAutoDistance)
                .build());
    	//fog min dist.
    	fog.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.fogMinDistance")
    					,config.getFogMinDistance()
    					,1
    					,32)
                .setDefaultValue(2)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogMinDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(newValue, config.getFogMaxDistance()))
                .build());
    	//fog max dist.
    	fog.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.fogMaxDistance")
    					,config.getFogMaxDistance()
    					,1
    					,32)
                .setDefaultValue(4)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogMaxDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(config.getFogMinDistance(), newValue))
                .build());    	
    }
    
    private void buildDensityCategory() {
    	// threshold
    	density.addEntry(entryBuilder
    			.startFloatField(Text.translatable("text.autoconfig.sfcr.option.densityThreshold")
    					, config.getDensityThreshold())
    			.setDefaultValue(1.3f)
    			.setMax(2f)
    			.setMin(-1f)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.densityThreshold.@Tooltip"))
    			.setSaveConsumer(config::setDensityThreshold)
    			.build());
    	// threshold multiplier
    	density.addEntry(entryBuilder
    			.startFloatField(Text.translatable("text.autoconfig.sfcr.option.thresholdMultiplier")
    					, config.getThresholdMultiplier())
    			.setDefaultValue(1.5f)
    			.setMax(3f)
    			.setMin(0f)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.thresholdMultiplier.@Tooltip"))
    			.setSaveConsumer(config::setThresholdMultiplier)
    			.build());
    	//weather
    	density.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableWeatherDensity")
    					,config.isEnableWeatherDensity())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableWeatherDensity.@Tooltip"))
                .setSaveConsumer(config::setEnableWeatherDensity)
                .build());
    	//weather pre-detect time
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.weatherPreDetectTime")
    					,config.getWeatherPreDetectTime()
    					,0
    					,30)
    			.setDefaultValue(10)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.weatherPreDetectTime.@Tooltip"))
    			.setSaveConsumer(config::setWeatherPreDetectTime)
    			.build());
    	//density info
    	density.addEntry(entryBuilder
    			.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.cloudDensity.@PrefixText"))
    			.build());
    	//cloud common density
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudDensity")
    					,config.getCloudDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(25)
    			.setSaveConsumer(config::setCloudDensityPercent)
    			.build());
    	//rain density
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.rainDensity")
    					,config.getRainDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(60)
    			.setSaveConsumer(config::setRainDensityPercent)
    			.build());
    	//thunder density
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.thunderDensity")
    					,config.getThunderDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(90)
    			.setSaveConsumer(config::setThunderDensityPercent)
    			.build());
    	//weather refresh speed 
    	density.addEntry(entryBuilder
    			.startEnumSelector(Text.translatable("text.autoconfig.sfcr.option.weatherRefreshSpeed")
    					,CloudRefreshSpeed.class
    					,config.getWeatherRefreshSpeed())
    			.setDefaultValue(CloudRefreshSpeed.FAST)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.weatherRefreshSpeed.@Tooltip"))
    			.setSaveConsumer(config::setWeatherRefreshSpeed)
    			.build());
    	//density changing speed
    	density.addEntry(entryBuilder
    			.startEnumSelector(Text.translatable("text.autoconfig.sfcr.option.densityChangingSpeed")
    					,CloudRefreshSpeed.class
    					,config.getDensityChangingSpeed())
    			.setDefaultValue(CloudRefreshSpeed.SLOW)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.densityChangingSpeed.@Tooltip"))
    			.setSaveConsumer(config::setDensityChangingSpeed)
    			.build());
    	//smooth change
    	density.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableSmoothChange")
    					,config.isEnableSmoothChange())
    			.setDefaultValue(false)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableSmoothChange.@Tooltip"))
    			.setSaveConsumer(config::setEnableSmoothChange)
    			.build());
    	//precipitation info
    	density.addEntry(entryBuilder
    			.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
    			.build());
    	//snow
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.snowDensity")
    					,config.getSnowDensity()
    					,0
    					,100)
    			.setDefaultValue(60)
    			.setSaveConsumer(config::setSnowDensity)
    			.build());
    	//rain
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
    					,config.getRainDensity()
    					,0
    					,100)
    			.setDefaultValue(90)
    			.setSaveConsumer(config::setRainDensity)
    			.build());
    	//none
    	density.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.noneDensity")
    					,config.getNoneDensity()
    					,0
    					,100)
    			.setDefaultValue(0)
    			.setSaveConsumer(config::setNoneDensity)
    			.build());
    	//biome density affect by chunk
    	density.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.isBiomeDensityByChunk")
    					,config.isBiomeDensityByChunk())
    			.setDefaultValue(false)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
    			.setSaveConsumer(config::setBiomeDensityByChunk)
    			.build());
    	//biome density detect loaded chunk
    	density.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.isBiomeDensityUseLoadedChunk")
    					,config.isBiomeDensityUseLoadedChunk())
    			.setDefaultValue(false)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
    			.setSaveConsumer(config::setBiomeDensityUseLoadedChunk)
    			.build());
    	//biome filter
    	density.addEntry(entryBuilder
    			.startStrList(Text.translatable("text.autoconfig.sfcr.option.biomeFilter")
    					,config.getBiomeFilterList())
    			.setDefaultValue(SFCReConfig.DEF_BIOME_FILTER_LIST)
    			.setTooltip(Text.translatable("text.autoconfig.sfcr.option.biomeFilter.@Tooltip"))
    			.setSaveConsumer(config::setBiomeFilterList)
    			.build());
    }
    
    private Text getNameFromSpeedEnum(CloudRefreshSpeed value) {
		if (value.equals(CloudRefreshSpeed.VERY_FAST)) {
			return Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.VERY_FAST");
		} else if (value.equals(CloudRefreshSpeed.FAST)){
			return Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.FAST");
		} else if (value.equals(CloudRefreshSpeed.NORMAL)) {
			return Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.NORMAL");
		} else if (value.equals(CloudRefreshSpeed.SLOW)) {
			return Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.SLOW");
		} else if (value.equals(CloudRefreshSpeed.VERY_SLOW)) {
			return Text.translatable("text.autoconfig.sfcr.option.cloudRefreshSpeed.VERY_SLOW");
		} else {
			return Text.of("");
		}
    }
}
