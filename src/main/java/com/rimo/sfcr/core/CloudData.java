package com.rimo.sfcr.core;

import com.rimo.sfcr.Client;
import com.rimo.sfcr.VersionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;

import static com.rimo.sfcr.Client.RENDERER;
import static com.rimo.sfcr.Common.*;

public class CloudData {
	public static Sampler sampler = new Sampler();
	private final Type dataType;
	private float lifeTime;
	ArrayList<CompressedFace> meshData = new ArrayList<>();
	protected boolean[][][] _cloudData;
	protected int width;
	protected int height;
	protected int gridCenterX;
	protected int gridCenterZ;
	// We want build inner faces earlier (no through culling equation to build useless faces), so this arg place here instead of Renderer.
	int gridYFromClouds;
	private boolean isOnBuild = false;
	private Thread buildThread;

	// Normal constructor
	CloudData(int x, int y, int z, float densityByWeather, float densityByBiome) {
		dataType = Type.NORMAL;
		width = CONFIG.getCloudRenderDistance() * 2 + 1;
		height = CONFIG.getCloudLayerThickness();
		gridCenterX = x;
		gridCenterZ = z;
		gridYFromClouds = y;
		_cloudData = new boolean[width][height][width];

		collectCloudData(x, z, densityByWeather, densityByBiome);
	}

	// for child
	CloudData(Type type) {
		dataType = type;
		lifeTime = CONFIG.getNormalRefreshSpeed().getValue() / 5f;
	}

	void tick() {
		lifeTime -= VersionUtil.getLastFrameDuration() * 0.25f * 0.25f;
	}

	// Access
	Type getDataType() {return dataType;}
	float getLifeTime() {return lifeTime;}

	boolean isCloudCovered(double x, double y, double z) {
		Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		int cbSize = CONFIG.getCloudBlockSize();
		int gx = (int) (width / 2F - (camPos.x() - x) / cbSize);
		int gy = (int) (y / cbSize * 2);
		int gz = (int) (width / 2F - (camPos.z() - z) / cbSize);
		if (gx < 0 || gx >= width || gz < 0 || gz >= width)
			return false;
		for (int i = height - 1; i >= 0; i --) {
			if (_cloudData[gx][i][gz]) {
				return gy - (int) (Client.RENDERER.getCloudHeight() / cbSize * 2) <= i;
			}
		}
		return false;
	}

	private void collectCloudData(int x, int z, float densityByWeather, float densityByBiome) {
		Level level = Minecraft.getInstance().level;
		if (level == null)
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

	CloudData buildMesh() {
		buildMesh(meshData);
		return this;
	}

	void tryRebuildMesh(int y) {
		if (isOnBuild)
  			return;
		isOnBuild = true;
		gridYFromClouds = y;
		buildThread = new Thread(() -> {
			try {
				ArrayList<CompressedFace> newMeshData = new ArrayList<>();
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

	void stop() {
		try {
			if (buildThread != null)
				buildThread.join();
		} catch (Exception e) {
			//
		}
	}

	/*
	   Compress structure:
	   0 0 0 0 0 0 0 0  0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0   0 0 0 0 0 0 0 0
	   └─┬─────────┘ |  | └─────┬─────┘   └──────┬──────┘   └──────┬──────┘
	     |   sign of x, z.      y             unsign x          unsign z
	     └─ thickness
	   Y no have negative value, and yet don't need up to 200+, so just store max to 127 (bit 01111111) save 1 bit for thickness.
	   Thickness max base to y.
	 */
	static class CompressedFace {
		private static final EnumMap<Facing, int[][]> OFFSET_MAP = new EnumMap<>(Facing.class);
		private final Facing facing;
		private final int data;

		static {
			OFFSET_MAP.put(Facing.EAST,   new int[][]{{1, 0, 0},{1, 0, 1},{1, 1, 1},{1, 1, 0}});
			OFFSET_MAP.put(Facing.WEST,   new int[][]{{0, 0, 0},{0, 0, 1},{0, 1, 1},{0, 1, 0}});
			OFFSET_MAP.put(Facing.TOP,    new int[][]{{0, 1, 0},{1, 1, 0},{1, 1, 1},{0, 1, 1}});
			OFFSET_MAP.put(Facing.BOTTOM, new int[][]{{0, 0, 0},{1, 0, 0},{1, 0, 1},{0, 0, 1}});
			OFFSET_MAP.put(Facing.SOUTH,  new int[][]{{0, 0, 1},{1, 0, 1},{1, 1, 1},{0, 1, 1}});
			OFFSET_MAP.put(Facing.NORTH,  new int[][]{{0, 0, 0},{1, 0, 0},{1, 1, 0},{0, 1, 0}});
		}

		CompressedFace(Facing facing, int x, int y, int z, int thick) {
			int i = 0;
			i |= (255 & x) << 8;  // 00000000 00000000 00000000 11111111
			i |= (127 & y) << 16;    // 00000000 00000000 00000000 01111111
			i |= (255 & z);
			i |= (Integer.MIN_VALUE & x) >>> 7;  // 10000000 00000000 00000000 00000000
			i |= (Integer.MIN_VALUE & z) >>> 8;
			i |= thick << 25;
			this.facing = facing;
			data = i;
		}

		Facing getFacing() {
			return facing;
		}

		int getThickness() {
			return data >>> 25;
		}

		int[][] getVertexList() {
			int x = (data << 7 & Integer.MIN_VALUE) == 0 ?  //is positive?
					data >> 8 & 255 :
					(data >> 8 & 255) - 256;  // -256 equals to | 0xFFFFFF00;
			int y = data >> 16 & 127;
			int z = (data << 8 & Integer.MIN_VALUE) == 0 ?
					data & 255 :
					(data & 255) - 256;

			int[][] offset = OFFSET_MAP.get(facing);

			int[][] vertex = new int[4][3];
			for (int i = 0; i < 4; i++) {
				vertex[i][0] = x + offset[i][0];
				vertex[i][1] = y + offset[i][1];
				vertex[i][2] = z + offset[i][2];
			}
			return vertex;
		}
	}

	/*
	 * try port 1.21.6+ vanilla mesh build function here...
	 */
	private void buildMesh(ArrayList<CompressedFace> meshData) {
		int cy = gridYFromClouds;
		if (cy >= 0 && cy < height) {
			int cx = width / 2;
			if (_cloudData[cx][cy][cx]) {  //build inner faces then return
				encodeFace(meshData, -1, cy, 0, Facing.EAST, 0);
				encodeFace(meshData, 1, cy, 0, Facing.WEST, 0);
				encodeFace(meshData, 0, cy - 1, 0, Facing.TOP, 0);
				encodeFace(meshData, 0, cy + 1, 0, Facing.BOTTOM, 0);
				encodeFace(meshData, 0, cy, -1, Facing.SOUTH, 0);
				encodeFace(meshData, 0, cy, 1, Facing.NORTH, 0);
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

	private void encodeFace(ArrayList<CompressedFace> meshData, int x, int y, int z, Facing facing, int thickness) {
		meshData.add(new CompressedFace(facing, x, y, z, thickness));
	}

	/**
	 * @return true if a cell was built, false if not.
	 */
	private boolean tryBuildCell(ArrayList<CompressedFace> meshData, int xOffset, int y, int zOffset, int renderDistance, int thickness) {
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

	private void buildExtrudedCell(ArrayList<CompressedFace> meshData, int x, int y, int z, int cellState) {
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

	enum Type {
		NORMAL,
		TRANS_IN,
		TRANS_MID_BODY,
		TRANS_OUT
	}

	enum Facing {
		EAST  (new int[]{ 1,  0,  0}, new float[]{0.95f, 0.9f,  0.9f }),
		WEST  (new int[]{-1,  0,  0}, new float[]{0.75f, 0.75f, 0.75f}),
		TOP   (new int[]{ 0,  1,  0}, new float[]{1f,    1f,    1f   }),
		BOTTOM(new int[]{ 0, -1,  0}, new float[]{0.6f,  0.6f,  0.6f }),
		SOUTH (new int[]{ 0,  0,  1}, new float[]{0.92f, 0.85f, 0.85f}),
		NORTH (new int[]{ 0,  0, -1}, new float[]{0.8f,  0.8f,  0.8f });

		final int[] normal;
		final float[] color;

		Facing(int[] normal, float[] color) {
			this.normal = normal;
			this.color = color;
		}
	}

	/*
	 * TODO
	 *  class of abandon function 'smooth change'
	 */

	static class CloudFadeData extends CloudData {
		// Reverse input to get between fade-in and fade-out data
		CloudFadeData(CloudData prevData, CloudData nextData, Type type) {
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

	static class CloudMidData extends CloudData {
		CloudMidData(CloudData prevData, CloudData nextData, Type type) {
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
