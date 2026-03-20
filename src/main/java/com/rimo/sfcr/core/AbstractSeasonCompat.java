package com.rimo.sfcr.core;

import com.rimo.sfcr.config.Config;
import com.rimo.sfcr.config.ConfigScreen;
//~ if ! 1.16.5 'me.shedaniel.' -> 'dev.'
import dev.architectury.platform.Platform;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.TreeMap;

import static com.rimo.sfcr.Common.LOGGER;
import static com.rimo.sfcr.Common.MOD_ID;

/**
 * An abstract class to handle current season mod listener and season density map.<br>
 * Each season mod has its own implementation invoke by reflection to prevent api changed.<br>
 * SubClass name use to show compat status in {@link ConfigScreen}, make it readable.<br>
 * Static call {@link #getInstance(Config)} to get an instance of impl.
 * @since 1.9
 */
public abstract class AbstractSeasonCompat {
	@SuppressWarnings("rawtypes")
	protected Class<? extends Enum> season;
	private TreeMap<Enum<?>, Integer> densityMap;

	/**
	 * @param config use to initialize density map.
	 * @return an instance if implemented mod exist, or null if not.
	 */
	public static @Nullable AbstractSeasonCompat getInstance(Config config) {
		try {
			if (Platform.isModLoaded("sereneseasons"))
				return new SereneSeasons(config);
			if (Platform.isFabric() && Platform.isModLoaded("seasons"))
				return new FabricSeasons(config);
		} catch (RuntimeException e) {
			LOGGER.error("{} Failed to initialize season listener, is season mod api changed? Please report.", MOD_ID, e);
		}
		return null;
	}

	/**
	 * @see #castStringToDensityMap(String)
	 */
	public void setDensityMapFromString(String str) {
		try {
			densityMap = castStringToDensityMap(str);
		} catch (IllegalArgumentException e) {
			if (season == null)
				LOGGER.error("{} Failed to apply seasonDensityMap because seasonEnum hasn't initialized.", MOD_ID);
			else {
				LOGGER.error("{} Failed to apply seasonDensityMap from '{}', please check config:\n{}", MOD_ID, str, e.getMessage());
			}
			if (densityMap == null) {
				densityMap = new TreeMap<>();
			}
		}
	}

	/**
	 * @param str without brackets, case-insensitive and can be unsorted, i.e. "a=1,f=7,c=3..."
	 * @throws IllegalArgumentException with reason message when cast failed
	 */
	public TreeMap<Enum<?>, Integer> castStringToDensityMap(String str) throws IllegalArgumentException {
		TreeMap<Enum<?>, Integer> map = new TreeMap<>();
		if (! str.isEmpty()) {
			String[] pairs = str.split(",");
			for (String pair : pairs) {
				pair = pair.trim();
				if (pair.isEmpty())
					continue;

				String[] kv = pair.split("=");
				if (kv.length != 2)
					throw new IllegalArgumentException("Invalid key-value pair format: " + pair);

				try {
					Enum<?> key = Enum.valueOf(season.asSubclass(Enum.class), kv[0].trim().toUpperCase());
					int value = Integer.parseInt(kv[1].trim());
					if (value < 0)
						throw new IllegalArgumentException("Negative value in pair: " + pair);
					map.put(key, value);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Non-integer value in pair: " + pair, e);
				} catch (NullPointerException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid key name in pair: " + pair, e);
				}
			}
		}
		return map;
	}

	/**
	 * Get a percentage value of specific season key from {@link #densityMap}<br>
	 * If key doesn't exist, an interpolation will present base on season key order
	 *
	 * @param key Seasons Enum Key
	 * @return percentage value, or 100 if map is empty or key class not match the map.
	 * @see #castStringToDensityMap(String)
	 * @author <a href="https://chat.deepseek.com/">Deepseek.ai</a>
	 */
	private Integer getDensityValue(Enum<?> key) {
		final int DEFAULT = 100;
		TreeMap<Enum<?>, Integer> map = this.densityMap;
		if (map.isEmpty() || key == null) {
			return DEFAULT;
		} else {
			Class<?> mapEnumClass = map.firstKey().getDeclaringClass();
			if (! mapEnumClass.equals(key.getDeclaringClass())) {
				LOGGER.error("{} SeasonMap class mismatch: map expects {}, but key {} is of type {}. Map keys: {}. It shouldn't be happened, please report.",
						MOD_ID, mapEnumClass.getName(), key.name(), key.getDeclaringClass().getName(), map.keySet());
				return DEFAULT;
			}
		}
		int totalCount = key.getDeclaringClass().getEnumConstants().length;

		// 精确匹配
		Integer exact = map.get(key);
		if (exact != null)
			return exact;

		// 只有一个点的情况：所有 key 均返回该点值
		if (map.size() == 1)
			return map.firstEntry().getValue();

		// 查找循环顺序中的前一个点和后一个点
		Enum<?> floor   = map.floorKey(key);   // ≤ key 的最大键
		Enum<?> ceiling = map.ceilingKey(key); // ≥ key 的最小键

		Enum<?> x1, x2; // 前后两个点的键
		if (floor != null && ceiling != null) {
			// key 在内部，前后点就是 floor 和 ceiling
			x1 = floor;
			x2 = ceiling;
		} else if (floor != null) {
			// key 大于所有键，前一个点为最大键，后一个点为最小键（循环）
			x1 = floor;          // 即最大键
			x2 = map.firstKey(); // 最小键
		} else {
			// key 小于所有键，前一个点为最大键（循环），后一个点为最小键
			x1 = map.lastKey();  // 最大键
			x2 = ceiling;        // 最小键
		}

		int y1 = map.get(x1);
		int y2 = map.get(x2);

		// 计算循环距离（从 x1 到 x2 沿循环方向经过的步数）
		int dist = (x2.ordinal() - x1.ordinal() + totalCount) % totalCount;

		// 将 key 映射到线性坐标：若 key < x1，则加上 totalCount 以落入 [x1, x2Linear] 区间
		int keyLinear = key.ordinal() < x1.ordinal() ? key.ordinal() + totalCount : key.ordinal();

		// 线性插值
		double t = (double) (keyLinear - x1.ordinal()) / dist;
		return (int) Math.round(y1 + t * (y2 - y1));
	}

	protected abstract Enum<?> getSeason(Level level);

	public int getSeasonDensityPercent(Level level) {
		return getDensityValue(getSeason(level));
	}

	/*
	 * Implementation...
	 */

	// I want to register an event handle of SeasonChangedEvent.Standard in glitchcore.event.EventManager at first.
	// But the registration under reflection is SO COMPLEX, I've surrendered.
	private static class SereneSeasons extends AbstractSeasonCompat {
		private final Method getSeasonState;
		private final Method getSubSeason;
		private SereneSeasons(Config config) throws RuntimeException {
			try {
				season = Class.forName("sereneseasons.api.season.Season$SubSeason").asSubclass(Enum.class);
				setDensityMapFromString(config.getSeasonDensityPercentMap().get(0));  //should call after seasonClass get
				getSeasonState = Class.forName("sereneseasons.api.season.SeasonHelper")
						.getDeclaredMethod("getSeasonState", Level.class);
				getSubSeason = Class.forName("sereneseasons.api.season.ISeasonState")
						.getDeclaredMethod("getSubSeason");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected Enum<?> getSeason(Level level) {
			try {
				Object objSeasonState = getSeasonState.invoke(null, level);
				Object objSubSeason = getSubSeason.invoke(objSeasonState);
				return (Enum<?>) objSubSeason;
			} catch (Exception e) {
				return null;
			}
		}
	}

	private static class FabricSeasons extends AbstractSeasonCompat {
		private final Method getCurrentSeason;
		private FabricSeasons(Config config) throws RuntimeException {
			try {
				season = Class.forName("io.github.lucaargolo.seasons.utils.Season").asSubclass(Enum.class);
				setDensityMapFromString(config.getSeasonDensityPercentMap().get(0));
				getCurrentSeason = Class.forName("io.github.lucaargolo.seasons.FabricSeasons")
						.getMethod("getCurrentSeason", Level.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected Enum<?> getSeason(Level level) {
			try {
				Object objSeason = getCurrentSeason.invoke(null, level);
				return (Enum<?>) objSeason;
			} catch (Exception e) {
				return null;
			}
		}
	}
}
