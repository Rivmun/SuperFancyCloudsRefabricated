package com.rimo.sfcr.core;

public class CloudFadeData extends CloudData {

	// Reverse input to get between fade-in and fade-out data
	public CloudFadeData(CloudData prevData, CloudData nextData, CloudDataType type) {
		super(type);

		width = nextData.width;
		height = nextData.height;
		_cloudData = new boolean[nextData.width][nextData.height][nextData.width];

		collectCloudData(prevData, nextData);
	}

	private void collectCloudData(CloudData prevData, CloudData nextData) {

		int startWidth = prevData.gridCenterX - nextData.gridCenterX;
		int startLength = prevData.gridCenterZ - nextData.gridCenterZ;
		int minWidth = Math.min(prevData.width, nextData.width) - Math.abs(startWidth) * 2;
		int minLength = Math.min(prevData.width, nextData.width) - Math.abs(startLength) * 2;
		int minHeight = Math.min(prevData.height, nextData.height);

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
	}
}
