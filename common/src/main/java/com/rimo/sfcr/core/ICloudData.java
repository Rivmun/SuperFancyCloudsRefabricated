package com.rimo.sfcr.core;

public interface ICloudData {
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome);
	public void collectCloudData(CloudData newData, CloudData oldData);
}
