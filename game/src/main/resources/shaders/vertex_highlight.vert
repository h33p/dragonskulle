#version 450
#extension GL_ARB_separate_shader_objects : enable

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
layout(location = 9) in vec4 instTexColor;
layout(location = 10) in float distPow;
layout(location = 11) in float maxDist;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragTexColor;
layout(location = 2) out vec2 fragUV;
layout(location = 3) out float outDistPow;
layout(location = 4) out float outMaxDist;

void main() {
	gl_Position = consts.proj * consts.view * model * vec4(inPosition, 1.0);
	fragColor = vec4(inColor, 1.0) * instColor;
	fragTexColor = instTexColor;
	fragUV = inUV;

	outDistPow = distPow;
	outMaxDist = maxDist;
}
