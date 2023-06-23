package com.rimo.sfcr.core;

public interface CloudDataImplement {
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome);
	public void collectCloudData(CloudData newData, CloudData oldData);
}
