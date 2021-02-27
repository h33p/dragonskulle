#version 450
#extension GL_ARB_separate_shader_objects : enable

struct UBO {
	mat4 model;
};

layout(binding = 0) uniform UniformBufferObject {
	UBO ubo[2];
};

layout(push_constant) uniform PushConsts {
	mat4 view;
	mat4 proj;
} consts;

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inUV;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragUV;

void main() {
	gl_Position = consts.proj * consts.view * ubo[gl_InstanceIndex % 2].model * vec4(inPosition, 0.0, 1.0);
	fragColor = inColor;
	fragUV = inUV;
}
