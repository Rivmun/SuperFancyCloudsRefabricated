package com.rimo.sfcr.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.*;

public class CloudData {
	public static Sampler sampler = new Sampler();
	private final Type dataType;
	private float lifeTime;
	protected ArrayList<Integer> meshData = new ArrayList<>();
	protected boolean[][][] _cloudData;
	protected int width;
	protected int height;
	protected int gridCenterX;
	protected int gridCenterZ;
	// We want build inner faces earlier (no through culling equation to build useless faces), so this arg place here instead of Renderer.
	protected int gridYFromClouds;
	private boolean isOnBuild = false;
	private Thread buildThread;

	// Normal constructor
	public CloudData(int x, int y, int z, float densityByWeather, float densityByBiome) {
		dataType = Type.NORMAL;
		width = CONFIG.getCloudRenderDistance() * 2 + 1;
		height = CONFIG.getCloudLayerThickness();
		gridCenterX = x;
		gridCenterZ = z;
		gridYFromClouds = minusCloudGridHeight(y);
		_cloudData = new boolean[width][height][width];

		collectCloudData(x, z, densityByWeather, densityByBiome);
	}

	// for child
	public CloudData(Type type) {
		dataType = type;
		lifeTime = CONFIG.getNormalRefreshSpeed().getValue() / 5f;
	}

	public void tick() {
		lifeTime -= MinecraftClient.getInstance().getLastFrameDuration() * 0.25f * 0.25f;
	}

	private int minusCloudGridHeight(int y) {
		return y - (int)(RENDERER.getCloudHeight() / CONFIG.getCloudBlockSize() * 2);
	}

	// Access
	public Type getDataType() {return dataType;}
	public float getLifeTime() {return lifeTime;}

	boolean isCloudCovered(double x, double y, double z) {
		Vec3d camPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		int cbSize = CONFIG.getCloudBlockSize();
		int gx = (int) (width / 2F - (camPos.getX() - x) / cbSize);
		int gy = (int) (y / cbSize * 2);
		int gz = (int) (width / 2F - (camPos.getZ() - z) / cbSize);
		for (int i = height - 1; i >= 0; i --) {
			if (_cloudData[gx][i][gz]) {
				return minusCloudGridHeight(gy) <= i;
			}
		}
		return false;
	}

	private void collectCloudData(int x, int z, float densityByWeather, float densityByBiome) {
		World world = MinecraftClient.getInstance().world;
		if (world == null)
			return;

		final int sx = x - width / 2;  //sampling start pos
		final int sz = z - width / 2;

		for (int cx = 0; cx < width; cx++) {
			for (int cz = 0; cz < width; cz++) {
				for (int cy = 0; cy < height; cy++) {
					_cloudData[cx][cy][cz] = sampler.isGridHasCloud(sx + cx, cy, sz + cz, densityByWeather, densityByBiome);
				}
			}
		}
	}

	/* - - - - - Mesh Computing - - - - - */

	public CloudData buildMesh() {
		buildMesh(meshData);
		return this;
	}

	public void tryRebuildMesh(int y) {
		if (isOnBuild)
  			return;
		int newGridY = minusCloudGridHeight(y);
		if (newGridY == gridYFromClouds)
			return;
		isOnBuild = true;
		gridYFromClouds = newGridY;
		buildThread = new Thread(() -> {
			try {
				ArrayList<Integer> newMeshData = new ArrayList<>();
				buildMesh(newMeshData);
				meshData = newMeshData;
			} catch (Exception e) {
				exceptionCatcher(e);
			} finally {
				RENDERER.markForRebuild();
				isOnBuild = false;
			}
		});
		buildThread.start();
	}

	public void stop() {
		try {
			if (buildThread != null)
				buildThread.join();
		} catch (Exception e) {
			//
		}
	}

	/*
	 * try port 1.21.6+ vanilla mesh build function here...
	 */
	private void buildMesh(ArrayList<Integer> meshData) {
		int cy = gridYFromClouds;
		if (cy >= 0 && cy < height) {
			int cx = width / 2;
			if (_cloudData[cx][cy][cx]) {  //build inner faces then return
				encodeFace(meshData, -1, cy, 0, Facing.EAST, 0);
				encodeFace(meshData, 1, cy, 0, Facing.WEST, 0);
				encodeFace(meshData, 0, cy - 1, 0, Facing.TOP, 0);
				encodeFace(meshData, 0, cy + 1, 0, Facing.BOTTOM, 0);
				encodeFace(meshData, 0, cy, 1, Facing.NORTH, 0);
				encodeFace(meshData, 0, cy, -1, Facing.SOUTH, 0);
				return;
			}
		}

		int renderDistance = (width - 1) / 2;
		for(int l = 0; l <= 2 * renderDistance; ++l) {
			for (int xOffset = - l; xOffset <= l; ++ xOffset) {
				int zOffset = l - Math.abs(xOffset);
				// circular-like culling...
				if (zOffset >= 0 && zOffset <= renderDistance && xOffset * xOffset + zOffset * zOffset <= renderDistance * renderDistance) {
					if (zOffset != 0) {
						int thickness = 0;  //these 2 y loop may run in same time, so thickness must sum separately
						for (int y = height - 1; y >= 0; -- y) {
							if (tryBuildCell(meshData, xOffset, y, - zOffset, renderDistance, thickness)) {
								thickness++;
							} else {
								if (thickness > 0)
									thickness --;
							}
						}
					}
					int thickness = 0;
					for (int y = height - 1; y >= 0; -- y) {
						if (tryBuildCell(meshData, xOffset, y, zOffset, renderDistance, thickness)) {
							thickness ++;
						} else {
							if (thickness > 0)
								thickness --;
						}
					}
				}
			}
		}
	}

	/*
	   Compress structure:
	   0 0 0 0 0 0 0 0  0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0
	                 |  | └─────┬─────┘   └──────┬──────┘   └──────┬──────┘
	         sign of x, z.      y             unsign x          unsign z
	   That's 1 vertex, we need 4 for 1 face.
	   Y no have negative value, and yet don't need up to 200+, so just store max to 127 (bit 01111111) save 1 bit for thickness
	   FACING information needs 3 bit (max ordinal 5 = bit 00000101), we compress at 1st vertex.
	   Thickness max base to y, we compress it in 2nd vertex int head.
	 */
	private void encodeFace(ArrayList<Integer> meshData, int x, int y, int z, Facing facing, int thickness) {
		switch (facing) {
			case EAST -> {
				meshData.add(compressToHead(compressVertex(x + 1, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z + 1), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x + 1, y + 1, z));
			}
			case WEST -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x, y, z + 1), thickness));
				meshData.add(compressVertex(x, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z));
			}
			case TOP -> {
				meshData.add(compressToHead(compressVertex(x, y + 1, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y + 1, z), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z + 1));
			}
			case BOTTOM -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z), thickness));
				meshData.add(compressVertex(x + 1, y, z + 1));
				meshData.add(compressVertex(x, y, z + 1));
			}
			case SOUTH -> {
				meshData.add(compressToHead(compressVertex(x, y, z + 1), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z + 1), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z + 1));
				meshData.add(compressVertex(x, y + 1, z + 1));
			}
			case NORTH -> {
				meshData.add(compressToHead(compressVertex(x, y, z), facing.ordinal()));
				meshData.add(compressToHead(compressVertex(x + 1, y, z), thickness));
				meshData.add(compressVertex(x + 1, y + 1, z));
				meshData.add(compressVertex(x, y + 1, z));
			}
		}
	}
	private static int compressVertex(int x, int y, int z) {
		int i = 0;
		i |= (255 & x) << 8;  // 00000000 00000000 00000000 11111111
		i |= (127 & y) << 16;    // 00000000 00000000 00000000 01111111
		i |= (255 & z);
		i |= (Integer.MIN_VALUE & x) >>> 7;  // 10000000 00000000 00000000 00000000
		i |= (Integer.MIN_VALUE & z) >>> 8;
		return i;
	}
	private static int compressToHead(int i, int j) {
		return i | j << 25;
	}
	static int[] depressVertex(int i) {
		int x, y, z;
		x = (i << 7 & Integer.MIN_VALUE) == 0 ?  //is positive?
				 i >> 8 & 255 :
				(i >> 8 & 255) - 256;  // -256 equals to | 0xFFFFFF00;
		y = i >> 16 & 127;
		z = (i << 8 & Integer.MIN_VALUE) == 0 ?
				 i & 255 :
				(i & 255) - 256;
		return new int[]{x, y, z};
	}
	static int depressFromHead(int i) {
		return i >> 25;
	}

	/**
	 * @return true if a cell was built, false if not.
	 */
	private boolean tryBuildCell(ArrayList<Integer> meshData, int xOffset, int y, int zOffset, int renderDistance, int thickness) {
		int x = xOffset + renderDistance;  //trans to list index
		int z = zOffset + renderDistance;
		if (!_cloudData[x][y][z])
			return false;  //ignore empty...
		boolean borderTop    = !(y + 1 <  height            && _cloudData[x][y + 1][z]);  // outOfBound check || isNotNeighbor
		boolean borderBottom = !(y - 1 >= 0                 && _cloudData[x][y - 1][z]);
		boolean borderSouth  = !(z + 1 <  _cloudData.length && _cloudData[x][y][z + 1]);
		boolean borderNorth  = !(z - 1 >= 0                 && _cloudData[x][y][z - 1]);
		boolean borderEast   = !(x + 1 <  _cloudData.length && _cloudData[x + 1][y][z]);
		boolean borderWest   = !(x - 1 >= 0                 && _cloudData[x -1 ][y][z]);
		int cellState = ((borderTop?1:0)<<5) | ((borderBottom?1:0)<<4) | ((borderEast?1:0)<<3) | ((borderWest?1:0)<<2) | ((borderSouth?1:0)<<1) | ((borderNorth?1:0)<<0) |
				(thickness << 6);
		buildExtrudedCell(meshData, xOffset, y, zOffset, cellState);
		return true;
	}

	private void buildExtrudedCell(ArrayList<Integer> meshData, int x, int y, int z, int cellState) {
		int thickness = cellState >> 6;
		if (hasBorderTop(cellState) && y < gridYFromClouds)  //facing (normals) culling...
			encodeFace(meshData, x, y, z, Facing.TOP, thickness);
		if (hasBorderBottom(cellState) && y > gridYFromClouds)
			encodeFace(meshData, x, y, z, Facing.BOTTOM, thickness);
		if (hasBorderSouth(cellState) && z < 0)
			encodeFace(meshData, x, y, z, Facing.SOUTH, thickness);
		if (hasBorderNorth(cellState) && z > 0)
			encodeFace(meshData, x, y, z, Facing.NORTH, thickness);
		if (hasBorderEast(cellState) && x < 0)
			encodeFace(meshData, x, y, z, Facing.EAST, thickness);
		if (hasBorderWest(cellState) && x > 0)
			encodeFace(meshData, x, y, z, Facing.WEST, thickness);
	}
	private static boolean hasBorderTop(int packed) {
		return (packed >> 5 & 1) != 0;
	}
	private static boolean hasBorderBottom(int packed) {
		return (packed >> 4 & 1) != 0;
	}
	private static boolean hasBorderEast(int packed) {
		return (packed >> 3 & 1) != 0;
	}
	private static boolean hasBorderWest(int packed) {
		return (packed >> 2 & 1) != 0;
	}
	private static boolean hasBorderSouth(int packed) {
		return (packed >> 1 & 1) != 0;
	}
	private static boolean hasBorderNorth(int packed) {
		return (packed >> 0 & 1) != 0;
	}

	public enum Type {
		NORMAL,
		TRANS_IN,
		TRANS_MID_BODY,
		TRANS_OUT
	}

	enum Facing {
		EAST  (new Vec3i(1, 0, 0),  new Vec3d(0.95f, 0.9f,  0.9f)),
		WEST  (new Vec3i(-1, 0, 0), new Vec3d(0.75f, 0.75f, 0.75f)),
		TOP   (new Vec3i(0, 1, 0),  new Vec3d(1f,    1f,    1f)),
		BOTTOM(new Vec3i(0, -1, 0), new Vec3d(0.6f,  0.6f,  0.6f)),
		SOUTH (new Vec3i(0, 0, 1),  new Vec3d(0.92f, 0.85f, 0.85f)),
		NORTH (new Vec3i(0, 0, -1), new Vec3d(0.8f,  0.8f,  0.8f));

		final Vec3i normal;
		final Vec3d color;

		Facing(Vec3i normal, Vec3d color) {
			this.normal = normal;
			this.color = color;
		}

		static Facing get(int i) {
			return Facing.values()[i];
		}
	}

	/*
	 * TODO
	 *  class of abandon function 'smooth change'
	 */

	public static class CloudFadeData extends CloudData {
		// Reverse input to get between fade-in and fade-out data
		public CloudFadeData(CloudData prevData, CloudData nextData, Type type) {
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

	public static class CloudMidData extends CloudData {
		public CloudMidData(CloudData prevData, CloudData nextData, Type type) {
			super(type);
			width = Math.max(prevData.width, nextData.width);
			height = Math.max(prevData.height, nextData.height);
			_cloudData = new boolean[width][height][width];
			collectCloudData(prevData, nextData);
		}

		private void collectCloudData(CloudData prevData, CloudData nextData) {
			int startWidth = Math.abs(prevData.width - nextData.width) / 2;
			int startLength = prevData.gridCenterZ - nextData.gridCenterZ + Math.abs(prevData.width - nextData.width) / 2;
			int minWidth = Math.min(prevData.width, nextData.width);
			int minLength = Math.min(prevData.width, nextData.width) - Math.abs(startLength) * 2;
			int minHeight = Math.min(prevData.height, nextData.height);
			// Get same block
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

}
