package com.rimo.sfcr.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rimo.sfcr.Common;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Plugin implements IMixinConfigPlugin {
	// Manually debug whether mixin inject success or failure
	public static Set<String> MIXINS = ConcurrentHashMap.newKeySet();

	@Override
	public void onLoad(String mixinPackage) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("sfcr.mixins.json")) {
			if (is != null) {
				JsonObject config = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
				String head = config.get("package").getAsString() + ".";

				JsonArray mixins = config.getAsJsonArray("mixins");
				if (mixins != null)
					mixins.forEach(e -> MIXINS.add(head + e.getAsString()));

				JsonArray client = config.getAsJsonArray("client");
				if (client != null)
					client.forEach(e -> MIXINS.add(head + e.getAsString()));
			}
		} catch (Exception ignored) {}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
		//
	}

	/**
	 * Third-party mod's mixin applied when the target mod loaded,
	 * Otherwise that mixin will pollute the logs during game loading.
	 */
	@Override
	public List<String> getMixins() {
		List<String> conditional = new ArrayList<>();
		// Must NOT use Class.forName here: getMixins runs during mixin prepare, loading the
		// @Mixin target class too early triggers MixinTargetAlreadyLoadedException.
		// Query the loader mod registry instead (no class loading, no cross-loader compile dep).
		if (isModLoaded("particlerain")) {
			conditional.add("particlerain.ParticleSpawnerMixin");
		} else {
			MIXINS.add("~ParticleRainMixin");
		}
		if (isModLoaded("iris")) {
			conditional.add("iris.IrisConfigMixin");
			conditional.add("iris.IrisPipelinesMixin");
			conditional.add("iris.TransformPatcherMixin");
			conditional.add("iris.VanillaTransformerMixin");
		} else {
			MIXINS.add("~IrisMixins");
		}
		return conditional.isEmpty() ? null : conditional;
	}

	/**
	 * Detect a mod by registry query via reflection, trying Fabric first then NeoForge/Forge,
	 * so this shared plugin class keeps no compile-time dependency on any single loader API.
	 */
	private static boolean isModLoaded(String modId) {
		// Fabric: FabricLoader.getInstance().isModLoaded(id)
		try {
			Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader")
					.getMethod("getInstance").invoke(null);
			return (boolean) loader.getClass().getMethod("isModLoaded", String.class)
					.invoke(loader, modId);
		} catch (Throwable ignored) {
		}
		// NeoForge/Forge: ModList.get().isLoaded(id)
		try {
			Object modList = Class.forName("net.neoforged.fml.ModList")
					.getMethod("get").invoke(null);
			return (boolean) modList.getClass().getMethod("isLoaded", String.class)
					.invoke(modList, modId);
		} catch (Throwable ignored) {
		}
		return false;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		//
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (MIXINS == null)
			return;
		MIXINS.remove(mixinClassName);
	}

	// manually mixin debug
	public static void checkMixinApplied() {
		if (MIXINS == null)
			return;
		if (MIXINS.isEmpty()) {
			Common.LOGGER.info("{} mixins was loaded entirely, enjoy!", Common.MOD_ID);
		} else {
			StringBuilder str = new StringBuilder();
			for (String s : MIXINS)
				str.append("  ").append(s).append("\n");
			Common.LOGGER.warn("{} was failed to apply mixin(s):\n{}Some function may no work.", Common.MOD_ID, str);
		}
		MIXINS = null;
	}
}
