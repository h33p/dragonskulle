#version 450
#extension GL_ARB_separate_shader_objects : enable

// Based on:
// OpenGL - PBR: https://www.youtube.com/watch?v=5p0e7YNONr8
// SIGGRAPH University - Introduction to "Physically Based Shading in Theory and Practice": https://www.youtube.com/watch?v=j-A0mwsJRmk
// https://github.com/Nadrin/PBR
// http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf

#ifndef NUM_LIGHTS
#define NUM_LIGHTS 1
#endif

const float EPSILON = 1e-16;
const float PI = 3.14159265358979323846;

// Some magic, it scales the peak sphere light intensity down to roughly
// match how intensity behaves in blender.
// TODO: fix this utter hack. Perhaps finally try to understand some of
// those big brain math equations, you dummy.
const float MAGIC = pow(PI, 5) / sqrt(2);

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

layout(location = 0) out vec4 outColor;

vec3 fresnelSchlick(float hv, vec3 reflectivity) {
	return reflectivity + (vec3(1.0) - reflectivity) * pow(1.0 - hv, 5);
}

float distributionGGX(float nh, float roughness, float peakOffset) {
	float alpha = roughness * roughness;
	float alphaSqr = alpha * alpha;
	float div = nh * nh * (alphaSqr - 1.0 / peakOffset) + 1.0 / peakOffset;
	div *= div;
	return max(alphaSqr, EPSILON) / max(div, EPSILON);
}

// Modified Schlick's GGX formula. We incorporate peak offset for peak light brightness.
float schlickGGX(float nv, float roughness, float peakOffset) {
	return nv / (nv * (peakOffset + roughness) + roughness / peakOffset);
}

float geomSmith(float nv, float nl, float roughness, float peakOffset) {
	roughness += 1.0;
	roughness = roughness * roughness / 8.0;
	return schlickGGX(nv, roughness, peakOffset) * schlickGGX(nl, roughness, peakOffset);
}

// Find the most direct light vector to the reflection one
vec3 sampleLVec(vec3 viewVec, vec3 normalVec, vec3 lVec, float sinAng) {
	vec3 viewReflect = reflect(viewVec, normalVec);

	vec3 centerToRay = dot(lVec, viewReflect) * viewReflect - lVec;

	return normalize(lVec + centerToRay * min(sinAng / length(centerToRay), 1.0));
}

vec3 calcLightShading(vec3 baseReflectivity, vec3 viewVec, vec3 lightVec, vec3 normalVec, vec3 lightIntensity, vec3 albedo, float roughness, float metalness) {
	roughness = max(roughness, 0.01);
	vec3 lVec = -normalize(lightVec);

	// Controls the size of light circle
	float sinAng = 0.05;

	float area = sinAng * sinAng * MAGIC;
	float peakOffset = max(area, 1.0);

	lVec = sampleLVec(viewVec, normalVec, lVec, sinAng);

	vec3 hVec = normalize(viewVec + lVec);
	float distance = 1.0;
	float attenuation = 1.0 / (distance * distance);
	vec3 radiance = lightIntensity * attenuation;

	float nv = max(dot(normalVec, viewVec), 0.0);
	float nl = max(dot(normalVec, lVec), 0.0);
	float hv = max(dot(hVec, viewVec), 0.0);
	float nh = max(dot(normalVec, hVec), 0.0);

	vec3 fresnel = fresnelSchlick(hv, baseReflectivity);
	float distribution = distributionGGX(nh, roughness, peakOffset);
	float geometry = geomSmith(max(nv, EPSILON), max(nl, EPSILON), roughness, peakOffset);

	// Specular BRDF reflection
	vec3 spec = fresnel * distribution * geometry;
	spec /= max(4.0 * nv * nl, EPSILON);

	// Diffuse lighting based on conservation of energy
	vec3 diffuse = mix(vec3(1.0) - fresnel, vec3(0.0), metalness);
	diffuse *= albedo;

	return (diffuse + spec) * radiance * nl;
}

// Get the TBN matrix. Based on:
// http://www.thetenthplanet.de/archives/1180
mat3 getTBN(vec3 normal, vec3 worldVec, vec2 uv) {
	// Calculate edge vectors
	vec3 dpX = dFdx(worldVec);
	vec3 dpY = dFdy(worldVec);
	vec2 duvX = dFdx(uv);
	vec2 duvY = dFdy(uv);

	vec3 dpYperp = cross(dpY, normal);
	vec3 dpXperp = cross(normal, dpX);
	vec3 T = dpYperp * duvX.x + dpXperp * duvY.x;
	vec3 B = dpYperp * duvX.y + dpXperp * duvY.y;

	float inv = inversesqrt(max(dot(T, T), dot(B, B)));
	return mat3(T * inv, B * inv, normal);
}

void main() {

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

	// Tonemap
	// TODO: Do this in post-processing effect
	color = color / (color + vec3(1.0));

	outColor =
#ifdef ALPHA_BLEND
		vec4(color * alpha, alpha);
#else
		vec4(color, 1.0);
#endif
}
