package com.rimo.sfcr;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class SFCReConfigScreen {
    
    ConfigBuilder builder = ConfigBuilder.create()
    		.setParentScreen(MinecraftClient.getInstance().currentScreen)
    		.setTitle(Text.translatable("text.autoconfig.sfcr.title"));
    ConfigEntryBuilder entryBuilder = builder.entryBuilder();
    ConfigCategory general = builder.getOrCreateCategory(Text.translatable("text.autoconfig.sfcr.title"));
    
    SFCReConfig config = SFCReMod.CONFIG.getConfig();
    
    public Screen buildScreen() {
    	//enabled
    	general.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableMod")
    					,config.isEnableMod())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableMod.@Tooltip"))
                .setSaveConsumer(config::setEnableMod)
                .build());
    	//weather
    	general.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableWeatherDensity")
    					,config.isEnableWeatherDensity())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableWeatherDensity.@Tooltip"))
                .setSaveConsumer(config::setEnableWeatherDensity)
                .build());
    	//cloud banner
    	general.addEntry(entryBuilder
    			.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.cloudHeight.@PrefixText"))
    			.build());
    	//cloud height
    	general.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudHeight")
    					,config.getCloudHeight()
    					,96
    					,384)
                .setDefaultValue(192)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudHeight.@Tooltip"))
                .setSaveConsumer(config::setCloudHeight)
                .build());
    	//cloud thickness
    	general.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudLayerThickness")
    					,config.getCloudLayerThickness()
    					,8
    					,80)
                .setDefaultValue(32)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudLayerThickness.@Tooltip"))
                .setSaveConsumer(config::setCloudLayerThickness)
                .build());
    	//cloud distance
    	general.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistance")
    					,config.getCloudRenderDistance()
    					,64
    					,384)
                .setDefaultValue(96)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistance.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistance)
                .build());
    	//cloud distance fit to view
    	general.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView")
    					,config.isCloudRenderDistanceFitToView())
                .setDefaultValue(false)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
                .setSaveConsumer(config::setCloudRenderDistanceFitToView)
                .build());
    	//fog banner
    	general.addEntry(entryBuilder
    			.startTextDescription(Text.translatable("text.autoconfig.sfcr.option.enableFog.@PrefixText"))
    			.build());
    	//fog enable
    	general.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.enableFog")
    					,config.isEnableFog())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.enableFog.@Tooltip"))
                .setSaveConsumer(config::setEnableFog)
                .build());
    	//fog auto distance
    	general.addEntry(entryBuilder
    			.startBooleanToggle(Text.translatable("text.autoconfig.sfcr.option.fogAutoDistance")
    					,config.isFogAutoDistance())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogAutoDistance.@Tooltip"))
                .setSaveConsumer(config::setFogAutoDistance)
                .build());
    	//fog min dist.
    	general.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.fogMinDistance")
    					,config.getFogMinDistance()
    					,1
    					,16)
                .setDefaultValue(2)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogMinDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(newValue, config.getFogMaxDistance()))
                .build());
    	//fog max dist.
    	general.addEntry(entryBuilder
    			.startIntSlider(Text.translatable("text.autoconfig.sfcr.option.fogMaxDistance")
    					,config.getFogMaxDistance()
    					,2
    					,18)
                .setDefaultValue(4)
                .setTooltip(Text.translatable("text.autoconfig.sfcr.option.fogMaxDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.setFogDisance(config.getFogMinDistance(), newValue))
                .build());

		//Update when saving
    	builder.setSavingRunnable(() -> {
    		SFCReMod.CONFIG.setConfig(config);
    		SFCReMod.CONFIG.save();
    		SFCReMod.RENDERER.UpdateRenderData(config);
    	});
    	
    	return builder.build();
    }
}
