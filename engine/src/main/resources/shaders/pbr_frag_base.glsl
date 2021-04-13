
// Based on:
// OpenGL - PBR: https://www.youtube.com/watch?v=5p0e7YNONr8
// SIGGRAPH University - Introduction to "Physically Based Shading in Theory and Practice": https://www.youtube.com/watch?v=j-A0mwsJRmk
// https://github.com/Nadrin/PBR
// http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf

#ifndef NUM_LIGHTS
#define NUM_LIGHTS 1
#endif

#include "pbr_base.glsl"

#ifdef ALBEDO_BINDING
layout(binding = ALBEDO_BINDING) uniform sampler2D albedo;
#endif
#ifdef NORMAL_BINDING
layout(binding = NORMAL_BINDING) uniform sampler2D normal;
#endif
#ifdef METALLIC_BINDING
layout(binding = METALLIC_BINDING) uniform sampler2D metalnessRoughness;
#endif

layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec2 fragUV;
layout(location = 2) in vec3 fragNormal;
layout(location = 3) in vec3 fragCam;
layout(location = 4) in vec3 fragPos;
layout(location = 5) in float fragAlphaCutoff;
layout(location = 6) in float fragMetallic;
layout(location = 7) in float fragRoughness;
layout(location = 8) in float fragNormalMul;
layout(location = 10) in vec3 fragLightDir[NUM_LIGHTS];
layout(location = 10 + NUM_LIGHTS) in vec3 fragLightCol[NUM_LIGHTS];
#define LAST_IN_LOCATION (10 + 2 * NUM_LIGHTS)

layout(location = 0) out vec4 outColor;

vec4 pbr_base() {

	vec4 albedoTex =
#ifdef ALBEDO_BINDING
		texture(albedo, fragUV);
#else
		vec4(1.0);
#endif

	float alpha = fragColor.a * albedoTex.a;

	if (alpha < fragAlphaCutoff) {
		discard;
	}

	vec3 albedo = albedoTex.rgb * fragColor.rgb;

	vec3 normalVec = vec3(0.0, 0.0, 1.0);

	mat3 tbn = getTBN(fragNormal, fragPos, fragUV);

#ifdef NORMAL_BINDING
	vec4 normalTex = texture(normal, fragUV);
	normalVec = (normalTex.rgb - vec3(0.5)) * 2.0;
	normalVec.xy *= fragNormalMul;
#endif

	normalVec = normalize(tbn * normalVec);

	vec3 viewVec = normalize(fragCam - fragPos);

	float roughness = fragRoughness;
	float metalness = fragMetallic;

#ifdef METALNESS_ROUGHNESS_BINDING
	vec4 metalnessRoughnessTex = texture(metalnessRoughness, fragUV);

	roughness *= metalnessRoughnessTex.g;
	metalness *= metalnessRoughnessTex.b;
#endif

	vec3 baseReflectivity = mix(vec3(0.04), albedo, metalness);

	vec3 accum = vec3(0.0);

	for (int i = 0; i < NUM_LIGHTS; i++) {
		accum += calcLightShading(
			baseReflectivity,
			viewVec,
			fragLightDir[i],
			normalVec,
			fragLightCol[i],
			albedo,
			roughness,
			metalness
		);
	}

	vec3 ambient = vec3(0.05) * albedo;

	vec3 color = ambient + accum;

	return vec4(color, alpha);
}
