package com.rimo.sfcr.config;

import java.util.ArrayList;
import java.util.List;

import com.rimo.sfcr.SFCReClient;
import com.rimo.sfcr.SFCReMain;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.client.MinecraftClient;

public class SFCReConfigScreen {
    
    ConfigBuilder builder = ConfigBuilder.create()
    		.setParentScreen(MinecraftClient.getInstance().currentScreen)
    		.setTitle(new TranslatableText("text.autoconfig.sfcr.title"));
    ConfigEntryBuilder entryBuilder = builder.entryBuilder();
    
    ConfigCategory clouds = builder.getOrCreateCategory(new TranslatableText("text.autoconfig.sfcr.category.clouds"));
    ConfigCategory fog = builder.getOrCreateCategory(new TranslatableText("text.autoconfig.sfcr.category.fog"));
    ConfigCategory density = builder.getOrCreateCategory(new TranslatableText("text.autoconfig.sfcr.category.density"));
    
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
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.enableMod")
    					,config.isEnableMod())
                .setDefaultValue(true)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.enableMod.@Tooltip"))
                .setSaveConsumer(config::setEnableMod)
                .build());
    	//server control
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.enableServer")
    					,config.isEnableServerConfig())
    			.setDefaultValue(false)
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.enableServer.@Tooltip"))
    			.setSaveConsumer(config::setEnableServerConfig)
    			.build());
    	//cloud height
    	clouds.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.cloudHeight")
    					,config.getCloudHeight()
    					,96
    					,384)
                .setDefaultValue(192)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudHeight.@Tooltip"))
                .setSaveConsumer(config::setCloudHeight)
                .build());
    	//cloud thickness
    	clouds.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.cloudLayerThickness")
    					,config.getCloudLayerThickness()
    					,8
    					,64)
                .setDefaultValue(32)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudLayerThickness.@Tooltip"))
                .setSaveConsumer(config::setCloudLayerThickness)
                .build());
    	//cloud distance
    	clouds.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.cloudRenderDistance")
    					,config.getCloudRenderDistance()
    					,64
    					,384)
                .setDefaultValue(96)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudRenderDistance.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistance)
                .build());
    	//cloud distance fit to view
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView")
    					,config.isCloudRenderDistanceFitToView())
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistanceFitToView)
                .build());
    	//cloud refresh speed
    	clouds.addEntry(entryBuilder
    			.startEnumSelector(new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed")
    					,CloudRefreshSpeed.class
    					,config.getNormalRefreshSpeed())
    			.setDefaultValue(CloudRefreshSpeed.SLOW)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.@Tooltip"))
    			.setSaveConsumer(config::setNormalRefreshSpeed)
    			.build());
    	//cloud sample steps
    	clouds.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.sampleSteps")
    					,config.getSampleSteps()
    					,1
    					,3)
    			.setDefaultValue(3)
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.sampleSteps.@Tooltip"))
    			.setSaveConsumer(config::setSampleSteps)
    			.build());
    	//DEBUG
    	/*
    	clouds.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.debug")
    					,config.isEnableDebug())
    			.setDefaultValue(false)
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.debug.@Tooltip"))
    			.setSaveConsumer(config::setEnableDebug)
    			.build());
    	 */
    }
    
    private void buildFogCategory() {
    	//fog enable
    	fog.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.enableFog")
    					,config.isEnableFog())
                .setDefaultValue(true)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.enableFog.@Tooltip"))
                .setSaveConsumer(config::setEnableFog)
                .build());
    	//fog auto distance
    	fog.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.fogAutoDistance")
    					,config.isFogAutoDistance())
                .setDefaultValue(true)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.fogAutoDistance.@Tooltip"))
                .setSaveConsumer(config::setFogAutoDistance)
                .build());
    	//fog min dist.
    	fog.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.fogMinDistance")
    					,config.getFogMinDistance()
    					,1
    					,32)
                .setDefaultValue(2)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.fogMinDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(newValue, config.getFogMaxDistance()))
                .build());
    	//fog max dist.
    	fog.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.fogMaxDistance")
    					,config.getFogMaxDistance()
    					,1
    					,32)
                .setDefaultValue(4)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.fogMaxDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(config.getFogMinDistance(), newValue))
                .build());    	
    }
    
    private void buildDensityCategory() {
    	//weather
    	density.addEntry(entryBuilder
    			.startBooleanToggle(new TranslatableText("text.autoconfig.sfcr.option.enableWeatherDensity")
    					,config.isEnableWeatherDensity())
                .setDefaultValue(true)
                .setTooltip(new TranslatableText("text.autoconfig.sfcr.option.enableWeatherDensity.@Tooltip"))
                .setSaveConsumer(config::setEnableWeatherDensity)
                .build());
    	//weather pre-detect time
    	density.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.weatherPreDetectTime")
    					,config.getWeatherPreDetectTime()
    					,0
    					,30)
    			.setDefaultValue(10)
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.weatherPreDetectTime.@Tooltip"))
    			.setSaveConsumer(config::setWeatherPreDetectTime)
    			.build());
    	//density info
    	density.addEntry(entryBuilder
    			.startTextDescription(new TranslatableText("text.autoconfig.sfcr.option.cloudDensity.@PrefixText"))
    			.build());
    	//cloud common density
    	density.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.cloudDensity")
    					,config.getCloudDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(25)
    			//.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.cloudDensity.@Tooltip"))
    			.setSaveConsumer(config::setCloudDensityPercent)
    			.build());
    	//rain density
    	density.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.rainDensity")
    					,config.getRainDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(60)
    			//.setTooltip(new TranslatableText("text.autoconfig.sfcr.optioon.rainDensity.@Tooltip"))
    			.setSaveConsumer(config::setRainDensityPercent)
    			.build());
    	//thunder density
    	density.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.thunderDensity")
    					,config.getThunderDensityPercent()
    					,0
    					,100)
    			.setDefaultValue(90)
    			//.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.thunderDensity.@Tooltip"))
    			.setSaveConsumer(config::setThunderDensityPercent)
    			.build());
    	//weather refresh speed 
    	density.addEntry(entryBuilder
    			.startEnumSelector(new TranslatableText("text.autoconfig.sfcr.option.weatherRefreshSpeed")
    					,CloudRefreshSpeed.class
    					,config.getWeatherRefreshSpeed())
    			.setDefaultValue(CloudRefreshSpeed.FAST)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.weatherRefreshSpeed.@Tooltip"))
    			.setSaveConsumer(config::setWeatherRefreshSpeed)
    			.build());
    	//density changing speed
    	density.addEntry(entryBuilder
    			.startEnumSelector(new TranslatableText("text.autoconfig.sfcr.option.densityChangingSpeed")
    					,CloudRefreshSpeed.class
    					,config.getDensityChangingSpeed())
    			.setDefaultValue(CloudRefreshSpeed.NORMAL)
    			.setEnumNameProvider((value) -> getNameFromSpeedEnum((CloudRefreshSpeed) value))
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.densityChangingSpeed.@Tooltip"))
    			.setSaveConsumer(config::setDensityChangingSpeed)
    			.build());
    	//biome detect
    	density.addEntry(entryBuilder
    			.startIntSlider(new TranslatableText("text.autoconfig.sfcr.option.biomeDensityMultipler")
    					,config.getBiomeDensityMultipler()
    					,0
    					,100)
    			.setDefaultValue(50)
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.biomeDensityMultipler.@Tooltip"))
    			.setSaveConsumer(config::setBiomeDensityMultipler)
    			.build());
    	//biome filter
    	density.addEntry(entryBuilder
    			.startStrList(new TranslatableText("text.autoconfig.sfcr.option.biomeFilter")
    					,config.getBiomeFilterList())
    			.setDefaultValue(() -> {
    				List<String> list = new ArrayList<>();
    				list.add("minecraft:river");
    				list.add("minecraft:frozen_river");
    				return list;
    			})
    			.setTooltip(new TranslatableText("text.autoconfig.sfcr.option.biomeFilter.@Tooltip"))
    			.setSaveConsumer(config::setBiomeFilterList)
    			.build());
    }
    
    private Text getNameFromSpeedEnum(CloudRefreshSpeed value) {
		if (value.equals(CloudRefreshSpeed.VERY_FAST)) {
			return new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.VERY_FAST");
		} else if (value.equals(CloudRefreshSpeed.FAST)){
			return new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.FAST");
		} else if (value.equals(CloudRefreshSpeed.NORMAL)) {
			return new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.NORMAL");
		} else if (value.equals(CloudRefreshSpeed.SLOW)) {
			return new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.SLOW");
		} else if (value.equals(CloudRefreshSpeed.VERY_SLOW)) {
			return new TranslatableText("text.autoconfig.sfcr.option.cloudRefreshSpeed.VERY_SLOW");
		} else {
			return Text.of("");
		}
    }
}
