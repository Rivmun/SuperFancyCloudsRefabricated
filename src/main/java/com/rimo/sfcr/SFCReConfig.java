package com.rimo.sfcr;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "sfcr")
public class SFCReConfig implements ConfigData {

	@ConfigEntry.Gui.Tooltip
	public boolean enableMod = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean enableFog = true;

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 8, min = 1)
	public int fogDistance = 2;

	@ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 384, min = 128)
	public int cloudHeight = 192;
	
	@ConfigEntry.Gui.PrefixText
    @ConfigEntry.BoundedDiscrete(max = 128, min = 8)
	public int cloudLayerThickness = 32;

    @ConfigEntry.BoundedDiscrete(max = 384, min = 64)
	public int cloudRenderDistance = 96;

	@ConfigEntry.Gui.Tooltip
	public boolean cloudRenderDistanceFitToView = false;
	
	//When nofog, need this to extend frustum.
	public int getMaxFogDistance() {
		return 8;
	}
}
