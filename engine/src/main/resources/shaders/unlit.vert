#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(push_constant) uniform PushConsts {
	mat4 view;
	mat4 proj;
} consts;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inUV;

layout(location = 3) in mat4 model;
layout(location = 7) in vec3 instColor;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragUV;

void main() {
	gl_Position = consts.proj * consts.view * model * vec4(inPosition, 1.0);
	fragColor = inColor * instColor;
	fragUV = inUV;
}
