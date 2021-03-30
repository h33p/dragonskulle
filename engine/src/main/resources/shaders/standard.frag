#version 450
#extension GL_ARB_separate_shader_objects : enable

// Based on:
// OpenGL - PBR: https://www.youtube.com/watch?v=5p0e7YNONr8
// SIGGRAPH University - Introduction to "Physically Based Shading in Theory and Practice": https://www.youtube.com/watch?v=j-A0mwsJRmk
// https://github.com/Nadrin/PBR
// http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf

const int NUM_LIGHTS = 4;

layout(binding = 0) uniform sampler2D albedo;
layout(binding = 1) uniform sampler2D normal;

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

const float PI = 3.14159265358979323846;
const float INV_PI = 1.0 / PI;
const float EPSILON = 0.000000000000001;

vec3 fresnelSchlick(float hv, vec3 reflectivity) {
	return reflectivity + (vec3(1.0) - reflectivity) * pow(1.0 - hv, 5);
}

float distributionGGX(float nh, float roughness) {
	float alpha = roughness * roughness;
	float alphaSqr = alpha * alpha;
	float div = nh * nh * (alphaSqr - 1.0) + 1.0;
	div *= div;
	return alphaSqr * INV_PI / max(div, EPSILON);
}

float schlickGGX(float nv, float roughness) {
	return nv / (nv * (1.0 - roughness) + roughness);
}

float geomSmith(float nv, float nl, float roughness) {
	roughness += 1.0;
	roughness = roughness * roughness / 8.0;
	return schlickGGX(nv, roughness) * schlickGGX(nl, roughness);
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
	vec4 albedoTex = texture(albedo, fragUV);

	float alpha = fragColor.a * albedoTex.a;

	if (alpha < fragAlphaCutoff) {
		discard;
	}

	vec3 albedo = albedoTex.rgb * fragColor.rgb;

	mat3 tbn = getTBN(fragNormal, fragPos, fragUV);

	vec3 defaultNormalVec = vec3(0.0, 0.0, 1.0);
	vec3 normalVec = defaultNormalVec;

	vec4 normalTex = texture(normal, fragUV);
	normalVec = (normalTex.rgb - vec3(0.5)) * 2.0;
	normalVec = mix(defaultNormalVec, normalVec, 0.1);

	normalVec = normalize(tbn * normalVec);
	vec3 viewVec = normalize(fragCam - fragPos);

	float roughness = fragRoughness;
	float metallic = fragMetallic;

	vec3 baseReflectivity = mix(vec3(0.08), albedo, metallic);

	vec3 accum = vec3(0.0);

	float nv = max(dot(normalVec, viewVec), 0.0);

	for (int i = 0; i < NUM_LIGHTS; i++) {
		vec3 lightVec = -normalize(fragLightDir[i]);
		vec3 hVec = normalize(viewVec + lightVec);
		float distance = 1.0;
		float attenuation = 1.0 / (distance * distance);
		vec3 radiance = fragLightCol[i] * attenuation;

		float nl = max(dot(normalVec, lightVec), 0.0);
		float hv = max(dot(hVec, viewVec), 0.0);
		float nh = max(dot(normalVec, hVec), 0.0);

		float distribution = distributionGGX(nh, roughness);
		float geometry = geomSmith(max(nv, EPSILON), max(nl, EPSILON), roughness);
		vec3 fresnel = fresnelSchlick(hv, baseReflectivity);

		// Specular reflection
		vec3 spec = fresnel * distribution * geometry;
		spec /= max(4.0 * nv * nl, EPSILON);

		// Diffuse lighting based on conservation of energy
		vec3 diffuse = mix(vec3(1.0) - fresnel, vec3(0.0), metallic);
		diffuse *= albedo;
		diffuse *= INV_PI;

		accum += (diffuse + spec) * radiance * nl;
	}

	vec3 ambient = vec3(0.05) * albedo;

	vec3 color = ambient + accum;
	//color = normalVec;

	// Tonemap
	color = color / (color + vec3(1.0));

	outColor = vec4(color * alpha, alpha);
}
