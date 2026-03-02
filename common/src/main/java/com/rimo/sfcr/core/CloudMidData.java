package com.rimo.sfcr.core;

public class CloudMidData extends CloudData {

	public CloudMidData(CloudData prevData, CloudData nextData, Type type) {
		super(type);

		width = Math.max(prevData.width, nextData.width);
		height = Math.max(prevData.height, nextData.height);
		_cloudData = new boolean[width][height][width];

		collectCloudData(prevData, nextData);
	}

	private void collectCloudData(CloudData prevData, CloudData nextData) {

		// Get same block
		int startWidth = Math.abs(prevData.width - nextData.width) / 2;
		int startLength = prevData.gridCenterZ - nextData.gridCenterZ + Math.abs(prevData.width - nextData.width) / 2;
		int minWidth = Math.min(prevData.width, nextData.width);
		int minLength = Math.min(prevData.width, nextData.width) - Math.abs(startLength) * 2;
		int minHeight = Math.min(prevData.height, nextData.height);

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
	}

}
