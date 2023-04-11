package com.rimo.sfcr.core;

import com.rimo.sfcr.util.CloudDataType;

public class CloudFadeData extends CloudData {

	// Reverse input to get between fade-in and fade-out data
	public CloudFadeData(CloudData prevData, CloudData nextData, CloudDataType type) {
		super(prevData, nextData, type);

		width = nextData.width;
		height = nextData.height;
		startX = nextData.startX;
		startZ = nextData.startZ;
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
					_cloudData[cx - startWidth][cy][cz - startLength] = !prevData._cloudData[cx][cy][cz] && nextData._cloudData[cx - startWidth][cy][cz - startLength];
				}
			}
		}

/*		if (prevData.width > nextData.width) {
			for (int cx = startWidth; cx < minWidth; cx++) {
				for (int cy = 0; cy < minHeight; cy++) {
					for (int cz = startWidth; cz < minWidth; cz++) {
						_cloudData[cx - startWidth][cy][cz - startWidth] = !prevData._cloudData[cx][cy][cz] && nextData._cloudData[cx - startWidth][cy][cz - startWidth];
					}
				}
			}

			for (int cx = 0; cx < width; cx++) {
				for (int cy = 0; cy < height; cy++) {
					for (int cz = 0; cz < width; cz++) {
						if (!_cloudData[cx][cy][cz])
							continue;

						//Right
						if (cx == width - 1 || !_cloudData[cx + 1][cy][cz]) {
							if (cy < minHeight) {
								if (prevData._cloudData[cx + 1 + startWidth][cy][cz + startWidth])		// Remove contiguous mesh
									continue;
							}
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx + 1, cy + 1, cz);

							normalList.add((byte) 0);
						}

						//Left....
						if (cx == 0 || !_cloudData[cx - 1][cy][cz]) {
							if (cy < minHeight) {
								if (prevData._cloudData[cx - 1 + startWidth][cy][cz + startWidth])
									continue;
							}
							addVertex(cx, cy, cz);
							addVertex(cx, cy, cz + 1);
							addVertex(cx, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz);

							normalList.add((byte) 1);
						}

						//Up....
						if (cy == height - 1 || !_cloudData[cx][cy + 1][cz]) {
							if (cy < minHeight - 1) {
								if (prevData._cloudData[cx + startWidth][cy + 1][cz + startWidth])
									continue;
							}
							addVertex(cx, cy + 1, cz);
							addVertex(cx + 1, cy + 1, cz);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz + 1);

							normalList.add((byte) 2);
						}

						//Down
						if (cy == 0 || !_cloudData[cx][cy - 1][cz]) {
							if (cy < minHeight) {
								if (prevData._cloudData[cx + startWidth][cy - 1][cz + startWidth])
									continue;
							}
							addVertex(cx, cy, cz);
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx, cy, cz + 1);

							normalList.add((byte) 3);
						}


						//Forward....
						if (cz == width - 1 || !_cloudData[cx][cy][cz + 1]) {
							if (cy < minHeight) {
								if (prevData._cloudData[cx + startWidth][cy][cz + 1 + startWidth])
									continue;
							}
							addVertex(cx, cy, cz + 1);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz + 1);

							normalList.add((byte) 4);
						}

						//Backward
						if (cz == 0 || !_cloudData[cx][cy][cz - 1]) {
							if (cy < minHeight) {
								if (prevData._cloudData[cx + startWidth][cy][cz - 1 + startWidth])
									continue;
							}
							addVertex(cx, cy, cz);
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy + 1, cz);
							addVertex(cx, cy + 1, cz);

							normalList.add((byte) 5);
						}
					}
				}
			}

		} else {
			for (int cx = startWidth; cx < minWidth; cx++) {
				for (int cy = 0; cy < minHeight; cy++) {
					for (int cz = startWidth; cz < minWidth; cz++) {
						_cloudData[cx][cy][cz] = !prevData._cloudData[cx - startWidth][cy][cz - startWidth] && nextData._cloudData[cx][cy][cz];
					}
				}
			}

			for (int cx = startWidth; cx < width; cx++) {
				for (int cy = 0; cy < height; cy++) {
					for (int cz = startWidth; cz < width; cz++) {
						if (!_cloudData[cx][cy][cz])
							continue;

						//Right
						if (cx == width - 1 || !_cloudData[cx + 1][cy][cz]) {
							if (cy < minHeight) {
								if (cx - startWidth == prevData.width - 1) {
									if (prevData._cloudData[cx][cy][cz - startWidth])
										continue;
								} else {
									if (prevData._cloudData[cx + 1 - startWidth][cy][cz - startWidth])		// Remove contiguous mesh
										continue;
								}
							}
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx + 1, cy + 1, cz);

							normalList.add((byte) 0);
						}

						//Left....
						if (cx == 0 || !_cloudData[cx - 1][cy][cz]) {
							if (cy < minHeight) {
								if (cx - startWidth == 0) {
									if (prevData._cloudData[0][cy][cz - startWidth])
										continue;
								} else {
									if (prevData._cloudData[cx - 1 - startWidth][cy][cz - startWidth])
										continue;
								}
							}
							addVertex(cx, cy, cz);
							addVertex(cx, cy, cz + 1);
							addVertex(cx, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz);

							normalList.add((byte) 1);
						}

						//Up....
						if (cy == height - 1 || !_cloudData[cx][cy + 1][cz]) {
							if (cy < minHeight) {
								if (cy == prevData.height - 1) {
									if (prevData._cloudData[cx - startWidth][cy][cz - startWidth])
										continue;
								} else {
									if (prevData._cloudData[cx - startWidth][cy + 1][cz - startWidth])
										continue;
								}
							}
							addVertex(cx, cy + 1, cz);
							addVertex(cx + 1, cy + 1, cz);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz + 1);

							normalList.add((byte) 2);
						}

						//Down
						if (cy == 0 || !_cloudData[cx][cy - 1][cz]) {
							if (cy < minHeight) {
								if (cy == 0) {
									if (prevData._cloudData[cx - startWidth][0][cz - startWidth])
										continue;
								} else {
									if (prevData._cloudData[cx - startWidth][cy - 1][cz - startWidth])
										continue;
								}
							}
							addVertex(cx, cy, cz);
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx, cy, cz + 1);

							normalList.add((byte) 3);
						}


						//Forward....
						if (cz == width - 1 || !_cloudData[cx][cy][cz + 1]) {
							if (cy < minHeight) {
								if (cz == prevData.width - 1) {
									if (prevData._cloudData[cx - startWidth][cy][cz])
										continue;
								} else {
									if (prevData._cloudData[cx - startWidth][cy][cz + 1 - startWidth])
										continue;
								}
							}
							addVertex(cx, cy, cz + 1);
							addVertex(cx + 1, cy, cz + 1);
							addVertex(cx + 1, cy + 1, cz + 1);
							addVertex(cx, cy + 1, cz + 1);

							normalList.add((byte) 4);
						}

						//Backward
						if (cz == 0 || !_cloudData[cx][cy][cz - 1]) {
							if (cy < minHeight) {
								if (cz == 0) {
									if (prevData._cloudData[cx - startWidth][cy][0])
										continue;
								} else {
									if (prevData._cloudData[cx - startWidth][cy][cz - 1 - startWidth])
										continue;
								}
							}
							addVertex(cx, cy, cz);
							addVertex(cx + 1, cy, cz);
							addVertex(cx + 1, cy + 1, cz);
							addVertex(cx, cy + 1, cz);

							normalList.add((byte) 5);
						}
					}
				}
			}
		}
*/
		computingCloudMesh();
	}
}