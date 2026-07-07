package com.rimo.sfcr.mixin.iris;

import com.rimo.sfcr.Common;
import net.irisshaders.iris.pipeline.transform.transformer.VanillaTransformer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Arrays;

@Mixin(VanillaTransformer.class)
public abstract class VanillaTransformerMixin {
	@ModifyArg(
			method = "transform",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/irisshaders/iris/pipeline/transform/parameter/VanillaParameters;isClouds()Z",
							ordinal = 3
					),
					to = @At(
							value = "INVOKE",
							target = "Lio/github/douira/glsl_transformer/ast/node/TranslationUnit;prependMainFunctionBody(Lio/github/douira/glsl_transformer/ast/transform/ASTParser;Ljava/lang/String;)V"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lio/github/douira/glsl_transformer/ast/node/TranslationUnit;parseAndInjectNodes(Lio/github/douira/glsl_transformer/ast/transform/ASTParser;Lio/github/douira/glsl_transformer/ast/transform/ASTInjectionPoint;[Ljava/lang/String;)V"
			),
			index = 2
	)
	private static String[] sfcr$modifyIrisCloudsPipeline(String[] original) {
		if (! Common.CONFIG.isEnableRender() || original.length == 0 || ! original[0].contains("layout(std140) uniform iris_CloudInfo"))
			return original;
		int len = original.length;
		String[] newArray = Arrays.copyOf(original, len + 1);
		newArray[3] = """
						const vec4[] iris_faceColors = vec4[](
						    // Bottom face
						    vec4(0.7, 0.7, 0.7, 1.0),
						    // Top face
						    vec4(1.0, 1.0, 1.0, 1.0),
						    // North face
						    vec4(0.8, 0.8, 0.8, 1.0),
						    // South face
						    vec4(0.8, 0.8, 0.8, 1.0),
						    // West face
						    vec4(0.9, 0.9, 0.9, 1.0),
						    // East face
						    vec4(0.9, 0.9, 0.9, 1.0)
						);
						""";
		newArray[len - 1] = "const int FLAG_EXTRA_H = 1 << 3;";  //add height mask bit
		if (Common.CONFIG.isEnableBottomDim()) {
			newArray[len] = """
					void iris_cloudsMain() {
					    int quadVertex = gl_VertexID % 4;
					    int index = (gl_VertexID / 4) * 5;

					    int cellX = texelFetch(CloudFaces, index).r;
					    int cellZ = texelFetch(CloudFaces, index + 1).r;
					    int cellH = texelFetch(CloudFaces, index + 2).r;
					    int dirAndFlags = texelFetch(CloudFaces, index + 3).r;
					    int thickness = texelFetch(CloudFaces, index + 4).r;
					    int direction = dirAndFlags & FLAG_MASK_DIR;
					    bool isInsideFace = (dirAndFlags & FLAG_INSIDE_FACE) == FLAG_INSIDE_FACE;
					    bool useTopColor = (dirAndFlags & FLAG_USE_TOP_COLOR) == FLAG_USE_TOP_COLOR;
					    cellX = (cellX << 1) | ((dirAndFlags & FLAG_EXTRA_X) >> 7);
					    cellZ = (cellZ << 1) | ((dirAndFlags & FLAG_EXTRA_Z) >> 6);
					    cellH = (cellH << 1) | ((dirAndFlags & FLAG_EXTRA_H) >> 3);
					    float thicknessColor = clamp((255 - thickness * 8) / 255.0f, 0.0, 1.0);
					    vec4 dimColor = vec4(thicknessColor, thicknessColor, thicknessColor, 1.0f);
					    vec3 faceVertex = iris_cloudVertices[(direction * 4) + (isInsideFace ? 3 - quadVertex : quadVertex)];
					    iris_cloudPos = (faceVertex * iris_Clouds.CellSize) + (vec3(cellX, cellH, cellZ) * iris_Clouds.CellSize) + iris_Clouds.CloudOffset;
					    iris_cloudNormal = iris_cloudNormals[direction];
					    iris_cloudCol = (useTopColor ? iris_faceColors[1] : iris_faceColors[direction]) * iris_Clouds.CloudColor * dimColor;
					    }
					""";
		} else {  // no thickness...
			newArray[len] = """
					void iris_cloudsMain() {
					    int quadVertex = gl_VertexID % 4;
					    int index = (gl_VertexID / 4) * 4;

					    int cellX = texelFetch(CloudFaces, index).r;
					    int cellZ = texelFetch(CloudFaces, index + 1).r;
					    int cellH = texelFetch(CloudFaces, index + 2).r;
					    int dirAndFlags = texelFetch(CloudFaces, index + 3).r;
					    int direction = dirAndFlags & FLAG_MASK_DIR;
					    bool isInsideFace = (dirAndFlags & FLAG_INSIDE_FACE) == FLAG_INSIDE_FACE;
					    bool useTopColor = (dirAndFlags & FLAG_USE_TOP_COLOR) == FLAG_USE_TOP_COLOR;
					    cellX = (cellX << 1) | ((dirAndFlags & FLAG_EXTRA_X) >> 7);
					    cellZ = (cellZ << 1) | ((dirAndFlags & FLAG_EXTRA_Z) >> 6);
					    cellH = (cellH << 1) | ((dirAndFlags & FLAG_EXTRA_H) >> 3);
					    vec3 faceVertex = iris_cloudVertices[(direction * 4) + (isInsideFace ? 3 - quadVertex : quadVertex)];
					    iris_cloudPos = (faceVertex * iris_Clouds.CellSize) + (vec3(cellX, cellH, cellZ) * iris_Clouds.CellSize) + iris_Clouds.CloudOffset;
					    iris_cloudNormal = iris_cloudNormals[direction];
					    iris_cloudCol = (useTopColor ? iris_faceColors[1] : iris_faceColors[direction]) * iris_Clouds.CloudColor;
					    }
					""";
		}
		return newArray;
	}
}
