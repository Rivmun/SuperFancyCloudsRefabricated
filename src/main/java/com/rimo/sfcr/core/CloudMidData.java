package com.rimo.sfcr.core;

import com.rimo.sfcr.util.CloudDataType;

public class CloudMidData extends CloudData {

	public CloudMidData(CloudData prevData, CloudData nextData, CloudDataType type) {
		super(prevData, nextData, type);

		width = Math.max(prevData.width, nextData.width);
		height = Math.min(prevData.height, nextData.height);
		
		collectCloudData(prevData, nextData);
	}

	@Override
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		// Leave empty to prevent wrong invoke.
	}

	@Override
	public void collectCloudData(CloudData prevData, CloudData nextData) {

		// Get same block
		var startWidth = (prevData.width - nextData.width) / 2;
		var minWidth = Math.min(prevData.width, nextData.width);
		var minHeight = Math.min(prevData.height, nextData.height);

		if (prevData.width > nextData.width) {
			for (int cx = startWidth; cx < minWidth; cx++) {
				for (int cy = 0; cy < minHeight; cy++) {
					for (int cz = startWidth; cz < minWidth; cz++) {
						_cloudData[cx][cy][cz] = prevData._cloudData[cx][cy][cz] == nextData._cloudData[cx - minWidth][cy][cz - minWidth];
					}
				}
			}
		} else {
			for (int cx = startWidth; cx < minWidth; cx++) {
				for (int cy = 0; cy < minHeight; cy++) {
					for (int cz = startWidth; cz < minWidth; cz++) {
						_cloudData[cx - minWidth][cy][cz - minWidth] = prevData._cloudData[cx - minWidth][cy][cz - minWidth] == nextData._cloudData[cx][cy][cz];
					}
				}
			}
		}

		// Use normal function
		computingCloudMesh();
	}

}
