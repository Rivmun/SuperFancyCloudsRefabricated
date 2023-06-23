package com.rimo.sfcr.core;

import com.rimo.sfcr.util.CloudDataType;

public class CloudFadeData extends CloudData {

	// Reverse input to get between fade-in and fade-out data
	public CloudFadeData(CloudData prevData, CloudData nextData, CloudDataType type) {
		super(prevData, nextData, type);

		width = nextData.width;
		height = nextData.height;
		_cloudData = new boolean[nextData.width][nextData.height][nextData.width];

		collectCloudData(prevData, nextData);
	}

	@Override
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		// Leave empty to prevent wrong invoke.
	}

	@Override
	public void collectCloudData(CloudData prevData, CloudData nextData) {

		var startWidth = prevData.startX - nextData.startX;
		var startLength = prevData.startZ - nextData.startZ;
		var minWidth = Math.min(prevData.width, nextData.width) - Math.abs(startWidth) * 2;
		var minLength = Math.min(prevData.width, nextData.width) - Math.abs(startLength) * 2;
		var minHeight = Math.min(prevData.height, nextData.height);

		// Remove same block
		for (int cx = startWidth; cx < minWidth; cx++) {
			if (cx < 0) cx = 0;
			for (int cy = 0; cy < minHeight; cy++) {
				for (int cz = startLength; cz < minLength; cz++) {
					if (cz < 0) cz = 0;
					_cloudData[cx][cy][cz] = 
							!prevData._cloudData[cx - startWidth][cy][cz - startLength] && 
							nextData._cloudData[cx][cy][cz];
				}
			}
		}
		computingCloudMesh();
	}
}