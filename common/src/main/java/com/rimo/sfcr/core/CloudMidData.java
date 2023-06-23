package com.rimo.sfcr.core;

import com.rimo.sfcr.util.CloudDataType;

public class CloudMidData extends CloudData {

	public CloudMidData(CloudData prevData, CloudData nextData, CloudDataType type) {
		super(prevData, nextData, type);

		width = Math.max(prevData.width, nextData.width);
		height = Math.max(prevData.height, nextData.height);
		_cloudData = new boolean[width][height][width];

		collectCloudData(prevData, nextData);
	}

	@Override
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		// Leave empty to prevent wrong invoke.
	}

	@Override
	public void collectCloudData(CloudData prevData, CloudData nextData) {

		// Get same block
		var startWidth = Math.abs(prevData.width - nextData.width) / 2;
		var startLength = prevData.startZ - nextData.startZ + Math.abs(prevData.width - nextData.width) / 2;
		var minWidth = Math.min(prevData.width, nextData.width);
		var minLength = Math.min(prevData.width, nextData.width) - Math.abs(startLength) * 2;
		var minHeight = Math.min(prevData.height, nextData.height);

		for (int cx = startWidth; cx < minWidth; cx++) {
			for (int cy = 0; cy < minHeight; cy++) {
				for (int cz = startLength; cz < minLength; cz++) {
					if (cz < 0) cz = 0;
					_cloudData[cx][cy][cz] = 
							prevData._cloudData[cx - startWidth][cy][cz - startLength] && 
							nextData._cloudData[cx][cy][cz];
				}
			}
		}

		// Use normal function
		computingCloudMesh();
	}

}