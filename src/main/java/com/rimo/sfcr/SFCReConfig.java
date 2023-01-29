package com.rimo.sfcr;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "sfcr")
public class SFCReConfig implements ConfigData {

	//@ConfigEntry.Gui.Tooltip
	//public boolean enableMod = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean enableFog = true;

	/*
	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 8, min = 1)
	public int fogStart = 2;

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 16, min = 4)
	public int fogEnd = 4;
	*/

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 8, min = 1)
	public int fogDistance = 2;

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 384, min = 128)
	public int cloudHeight = 192;
	
	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 8)
	public int cloudLayerThickness = 32;

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 180, min = 60)
	public int cloudRenderDistance = 96;

	@ConfigEntry.Gui.Tooltip
	public boolean cloudRenderDistanceFitToView = false;
	
	public int getMaxFogDistance() {
		return 8;
	}
}
