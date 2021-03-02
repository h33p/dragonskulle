#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform sampler samplers[4];
layout(binding = 1) uniform texture2D textures[1];

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 fragUV;

layout(location = 0) out vec4 outColor;

void main() {
	outColor = texture(sampler2D(textures[0], samplers[0]), fragUV);
}
