#version 450
#extension GL_ARB_separate_shader_objects : enable

#ifndef NUM_LIGHTS
#define NUM_LIGHTS 1
#endif

layout(push_constant) uniform PushConsts {
	mat4 view;
	mat4 proj;
} consts;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inColor;
layout(location = 3) in vec2 inUV;

layout(location = 4) in mat4 model;
layout(location = 8) in vec4 instColor;
layout(location = 9) in vec3 inCam;
layout(location = 10) in float alphaCutoff;
layout(location = 11) in float metallic;
layout(location = 12) in float roughness;
layout(location = 13) in float normal;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec2 fragUV;
layout(location = 2) out vec3 fragNormal;
layout(location = 3) out vec3 fragCam;
layout(location = 4) out vec3 fragPos;

layout(location = 5) out float fragAlphaCutoff;
layout(location = 6) out float fragMetallic;
layout(location = 7) out float fragRoughness;
layout(location = 8) out float fragNormalMul;

layout(location = 10) out vec3 lightDir[NUM_LIGHTS];
layout(location = 10 + NUM_LIGHTS) out vec3 lightCol[NUM_LIGHTS];

const float PI = 3.14159265358979323846;
const float INV_PI = 1.0 / PI;

void main() {
	fragCam = inCam;
	vec4 pos = model * vec4(inPosition, 1.0);
	fragPos = pos.xyz;
	gl_Position = consts.proj * consts.view * pos;
	fragColor = vec4(inColor, 1.0) * instColor;
	fragNormal = normalize(transpose(inverse(mat3(model))) * inNormal);
	fragUV = inUV;

	fragAlphaCutoff = alphaCutoff;
	fragMetallic = metallic;
	fragRoughness = roughness;
	fragNormalMul = normal;

	vec3 lDir = vec3(-0.5, 0.7, -1.0);
	float lIntensity = 10.0;
	vec3 lColor = vec3(1.0);

	lightDir[0] = normalize(lDir);
	lightCol[0] = lIntensity * lColor * INV_PI;

	for (int i = 1; i < NUM_LIGHTS; i++) {
		lightDir[i] = vec3(1, 0, 0);
		lightCol[i] = vec3(1) * 0;
	}
}
