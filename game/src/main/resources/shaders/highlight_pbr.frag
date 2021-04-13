#version 450
#extension GL_ARB_separate_shader_objects : enable

#include "pbr_frag_base.glsl"

layout(location = LAST_IN_LOCATION + 1) in vec4 inOverlay;
layout(location = LAST_IN_LOCATION + 2) in float inMinDist;
layout(location = LAST_IN_LOCATION + 3) in float inDistPow;
layout(location = LAST_IN_LOCATION + 4) in float inAlphaMul;

void main() {
	vec4 color = pbr_base();

	float dist = length(fragUV - vec2(0.5));
	float lerp = min(dist / inMinDist, 1.0);
	float add = pow(lerp, inDistPow);
	vec4 col = inOverlay + ((vec4(inOverlay.rgb, 1.0) * vec4(vec3(fragColor.a), min(1.0, inOverlay.a * inAlphaMul)) * add));

	color.rgb = mix(color.rgb, inOverlay.rgb, col.a);

	// Tonemap
	// TODO: Do this in post-processing effect
	color.rgb = color.rgb / (color.rgb + vec3(1.0));

	outColor =
#ifdef ALPHA_BLEND
		vec4(color.rgb * color.a, color.a);
#else
		vec4(color.rgb, 1.0);
#endif
}
