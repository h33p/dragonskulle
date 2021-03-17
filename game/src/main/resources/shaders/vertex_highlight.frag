#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform sampler2D diffuse;

layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec2 fragUV;
layout(location = 2) in float distPow;
layout(location = 3) in float maxDist;

layout(location = 0) out vec4 outColor;

void main() {
	outColor = texture(diffuse, fragUV) * fragColor * vec4(vec3(fragColor.w), 1.0);
	float dist = length(fragUV);
	float lerp = min(dist / maxDist, 1.0);
	float add = pow(lerp, distPow);
	vec4 col = fragColor * vec4(vec3(fragColor.w), 1.0) * add;

	outColor = texture(diffuse, fragUV) + col;
}
