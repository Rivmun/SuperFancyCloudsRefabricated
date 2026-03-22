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

	@Override
	public void onLoad(String mixinPackage) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("sfcr.mixins.json")) {
			if (is != null) {
				JsonObject config = new JsonParser().parse(new InputStreamReader(is)).getAsJsonObject();
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
			Common.LOGGER.error("{} was failed to apply mixin(s):\n{}Some function may no work.", Common.MOD_ID, str);
		}
		MIXINS = null;
	}
}
