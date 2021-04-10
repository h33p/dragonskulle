
const float PI = 3.14159265358979323846;
const float INV_PI = 1.0 / PI;

#ifndef NUM_LIGHTS
#define NUM_LIGHTS 1
#endif

#define DNUM_LIGHTS (2 * NUM_LIGHTS)

layout(push_constant) uniform PushConsts {
	mat4 view;
	mat4 proj;
} consts;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inColor;
layout(location = 3) in vec2 inUV;

layout(location = 4) in mat4 model;

layout(location = 8) in vec3 inLightDir[NUM_LIGHTS];
layout(location = 8 + NUM_LIGHTS) in vec3 inLightCol[NUM_LIGHTS];

layout(location = 8 + DNUM_LIGHTS) in vec4 instColor;
layout(location = 9 + DNUM_LIGHTS) in vec3 inCam;

layout(location = 10 + DNUM_LIGHTS) in float alphaCutoff;
layout(location = 11 + DNUM_LIGHTS) in float metallic;
layout(location = 12 + DNUM_LIGHTS) in float roughness;
layout(location = 13 + DNUM_LIGHTS) in float normalMul;
#define LAST_IN_LOCATION (13 + DNUM_LIGHTS)

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec2 fragUV;
layout(location = 2) out vec3 fragNormal;
layout(location = 3) out vec3 fragCam;
layout(location = 4) out vec3 fragPos;

layout(location = 5) out float fragAlphaCutoff;
layout(location = 6) out float fragMetallic;
layout(location = 7) out float fragRoughness;
layout(location = 8) out float fragNormalMul;

layout(location = 10) out vec3 fragLightDir[NUM_LIGHTS];
layout(location = 10 + NUM_LIGHTS) out vec3 fragLightCol[NUM_LIGHTS];
#define LAST_OUT_LOCATION (10 + DNUM_LIGHTS)

void pbr_base() {
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
	fragNormalMul = normalMul;

	for (int i = 0; i < NUM_LIGHTS; i++) {
		fragLightDir[i] = inLightDir[i];
		fragLightCol[i] = inLightCol[i];
	}
}
