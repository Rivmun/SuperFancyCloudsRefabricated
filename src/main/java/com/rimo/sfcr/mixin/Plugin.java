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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Plugin implements IMixinConfigPlugin {
	// Manually debug whether mixin inject success or failure
	public static Set<String> MIXINS = ConcurrentHashMap.newKeySet();
	private static boolean isIrisLoaded;
	private static boolean isParticleRainLoaded;

	@Override
	public void onLoad(String mixinPackage) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("sfcr.mixins.json")) {
			if (is != null) {
				JsonObject config = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

				JsonArray mixins = config.getAsJsonArray("mixins");
				if (mixins != null)
					mixins.forEach(e -> MIXINS.add(mixinPackage + "." + e.getAsString()));

				JsonArray client = config.getAsJsonArray("client");
				if (client != null)
					client.forEach(e -> MIXINS.add(mixinPackage + "." + e.getAsString()));
			}
		} catch (Exception ignored) {}

		isIrisLoaded = getClass().getClassLoader().getResource("mixins.iris.json") != null;
		isParticleRainLoaded = getClass().getClassLoader().getResource("particlerain.mixins.json") != null;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		// excluded compatibility mixin when target mod doesn't exist to prevent meaningless warn log out.
		if (! isIrisLoaded && mixinClassName.contains("iris.")) return false;
		if (! isParticleRainLoaded && mixinClassName.contains("particlerain.")) return false;
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
		//
	}

	@Override
	public List<String> getMixins() {
		return null;
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
