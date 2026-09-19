package com.rimo.sfcr.mixin.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Method;
import java.util.Map;

@Mixin(TransformPatcher.class)
public abstract class TransformPatcherMixin {
	/*
	 * Once transform succeed, the transformed vanilla cloud pipeline will never refresh and stored in TransformPatcher.cache.
	 * The iris will never transform again that leads our modified shader always active.
	 * We must nullify the cache.get / cache.constainsKey to force iris to re-compile the cloud pipeline, to apply our config change when reload().
	 */
	@WrapOperation(method = "transform", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
	private static boolean sfcr$forceCloudRecompile(Map instance, Object o, Operation<Boolean> original) {
		if (sfcr$isClouds(o))
			return false;
		return original.call(instance, o);
	}

	/*
	 * Parameters.class / VanillaParameters.class implements glsl_transformer that we cannot access directly.
	 * Here we use reflection to access the parameters member in CacheKey object, to check if it is cloud pipeline.
	 */
	@Unique
	private static boolean sfcr$isClouds(Object cacheKey) {
		try {
			var field = cacheKey.getClass().getDeclaredField("parameters");
			field.setAccessible(true);
			Object parameters = field.get(cacheKey);
			Method isClouds = parameters.getClass().getMethod("isClouds");
			return Boolean.TRUE.equals(isClouds.invoke(parameters));
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}
}
