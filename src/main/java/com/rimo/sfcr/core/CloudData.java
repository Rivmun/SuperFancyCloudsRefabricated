package com.rimo.sfcr.core;

import com.rimo.sfcr.SFCReMain;
import com.rimo.sfcr.config.SFCReConfig;
import com.rimo.sfcr.util.CloudDataType;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

public class CloudData implements CloudDataImplement {

	private static SimplexNoiseSampler cloudNoise;
	
	protected final SFCReRuntimeData runtimeData = SFCReMain.RUNTIME.getInstance();
	protected final SFCReConfig config = SFCReMain.CONFIGHOLDER.getConfig();
	
	private CloudDataType dataType;
	private float lifeTime;
	
	protected FloatArrayList vertexList = new FloatArrayList();
	protected ByteArrayList normalList = new ByteArrayList();
	protected boolean[][][] _cloudData;
	
	protected int width;
	protected int height;
	
	// Normal constructor
	public CloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		dataType = CloudDataType.NORMAL;
		width = config.getCloudRenderDistance();
		height = config.getCloudLayerThickness();
		_cloudData = new boolean[width][height][width];
		
		collectCloudData(scrollX, scrollZ, densityByWeather, densityByBiome);
	}
	
	// Overload
	public CloudData(CloudData prevData, CloudData nextData, CloudDataType type) {
		dataType = type;
		lifeTime = config.getNumFromSpeedEnum(config.getNormalRefreshSpeed()) / 5f;
	}
	
	public static void initSampler(long seed) {
		cloudNoise = new SimplexNoiseSampler(Random.create(seed));
	}
	
	public void tick() {
		lifeTime -= MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
	}
	
	// Access
	public FloatArrayList getVertexList() {
		return vertexList;
	}
	public ByteArrayList getNormalList() {
		return normalList;
	}
	public boolean[][][] getCloudData(){
		return _cloudData;
	}
	public CloudDataType getDataType() {
		return dataType;
	}
	public float getLifeTime() {
		return lifeTime;
	}
	
	protected double remappedValue(double noise) {
		return (Math.pow(Math.sin(Math.toRadians(((noise * 180) + 302) * 1.15)), 0.28) + noise - 0.5f) * 2;
	}

	protected void addVertex(float x, float y, float z) {
		vertexList.add(x - width / 2);
		vertexList.add(y);
		vertexList.add(z - width / 2);
	}
	
	@Override
	public void collectCloudData(double scrollX, double scrollZ, float densityByWeather, float densityByBiome) {
		try {
			double startX = scrollX / 16;
			double startZ = scrollZ / 16;

			double timeOffset = Math.floor(runtimeData.time / 6) * 6;

			runtimeData.checkFullOffset();
			
			float baseFreq = 0.05f;
			float baseTimeFactor = 0.01f;

			float l1Freq = 0.09f;
			float l1TimeFactor = 0.02f;

			float l2Freq = 0.001f;
			float l2TimeFactor = 0.1f;
			
			var f = 1.3 - densityByWeather * (1 - (1 - densityByBiome) * config.getBiomeDensityMultipler() / 100f * 1.5);
			if (config.isEnableDebug())
				SFCReMain.LOGGER.info("[SFCRe] density W: " + densityByWeather + ", B: " + densityByBiome + ", f: " + f);

			for (int cx = 0; cx < width; cx++) {
				for (int cy = 0; cy < height; cy++) {
					for (int cz = 0; cz < width; cz++) {
						double cloudVal = cloudNoise.sample(
								(startX + cx + (timeOffset * baseTimeFactor)) * baseFreq,
								(cy - (timeOffset * baseTimeFactor * 2)) * baseFreq,
								(startZ + cz - runtimeData.fullOffset) * baseFreq
						);
						if (config.getSampleSteps() > 1) {
							double cloudVal1 = cloudNoise.sample(
									(startX + cx + (timeOffset * l1TimeFactor)) * l1Freq,
									(cy - (timeOffset * l1TimeFactor)) * l1Freq,
									(startZ + cz - runtimeData.fullOffset) * l1Freq
							);
							double cloudVal2 = 1;
							if (config.getSampleSteps() > 2) {
								cloudVal2 = cloudNoise.sample(
										(startX + cx + (timeOffset * l2TimeFactor)) * l2Freq,
										0,
										(startZ + cz - runtimeData.fullOffset) * l2Freq
								);
								
								//Smooth floor function...
								cloudVal2 *= 3;
								cloudVal2 = (cloudVal2 - (Math.sin(Math.PI * 2 * cloudVal2) / (Math.PI * 2))) / 2.0f;
							}
	
							cloudVal = ((cloudVal + (cloudVal1 * 0.8f)) / 1.8f) * cloudVal2;
						}

						cloudVal = cloudVal * remappedValue(1 - ((double) (cy + 1) / 32));		//cloudVal ~ [-1, 2]

						_cloudData[cx][cy][cz] = cloudVal > f;		//Original is 0.5f.
					}
				}
			}
		} catch (Exception e) {
			// -- Ignore...
		}
		
		computingCloudMesh();
	}

	@Override
	public void collectCloudData(CloudData prevData, CloudData nextData) {
		// Leave empty here for child.
	}
	
	protected void computingCloudMesh() {
		for (int cx = 0; cx < width; cx++) {
			for (int cy = 0; cy < height; cy++) {
				for (int cz = 0; cz < width; cz++) {
					if (!_cloudData[cx][cy][cz])
						continue;

					//Right
					if (cx == width - 1 || !_cloudData[cx + 1][cy][cz]) {
						addVertex(cx + 1, cy, cz);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx + 1, cy + 1, cz);

						normalList.add((byte) 0);
					}

					//Left....
					if (cx == 0 || !_cloudData[cx - 1][cy][cz]) {
						addVertex(cx, cy, cz);
						addVertex(cx, cy, cz + 1);
						addVertex(cx, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz);

						normalList.add((byte) 1);
					}

					//Up....
					if (cy == height - 1 || !_cloudData[cx][cy + 1][cz]) {
						addVertex(cx, cy + 1, cz);
						addVertex(cx + 1, cy + 1, cz);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz + 1);

						normalList.add((byte) 2);
					}

					//Down
					if (cy == 0 || !_cloudData[cx][cy - 1][cz]) {
						addVertex(cx, cy, cz);
						addVertex(cx + 1, cy, cz);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx, cy, cz + 1);

						normalList.add((byte) 3);
					}


					//Forward....
					if (cz == width - 1 || !_cloudData[cx][cy][cz + 1]) {
						addVertex(cx, cy, cz + 1);
						addVertex(cx + 1, cy, cz + 1);
						addVertex(cx + 1, cy + 1, cz + 1);
						addVertex(cx, cy + 1, cz + 1);

						normalList.add((byte) 4);
					}

					//Backward
					if (cz == 0 || !_cloudData[cx][cy][cz - 1]) {
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
}
