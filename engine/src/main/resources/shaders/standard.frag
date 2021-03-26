#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform sampler2D diffuse;

layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec3 fragNormal;
layout(location = 2) in vec2 fragUV;

layout(location = 0) out vec4 outColor;

void main() {
	vec4 tex = texture(diffuse, fragUV);
	float texSum = tex.x + tex.y + tex.z - 3.0;

	// Dark magic to use normal colour only when untextured
	vec4 normalCol = vec4(vec3(1.0) - (-sign(texSum) - 1.0) * fragNormal, 1.0);

	outColor = tex * fragColor * vec4(vec3(fragColor.w), 1.0) * normalCol;
}
