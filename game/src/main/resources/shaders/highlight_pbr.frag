#version 450
#extension GL_ARB_separate_shader_objects : enable

#include "pbr_frag_base.glsl"

layout(location = LAST_IN_LOCATION + 1) in vec4 inOverlay;
layout(location = LAST_IN_LOCATION + 2) in float inMinDist;
layout(location = LAST_IN_LOCATION + 3) in float inMaxDist;
layout(location = LAST_IN_LOCATION + 4) in float inMinLerp;
layout(location = LAST_IN_LOCATION + 5) in float inDistPow;
layout(location = LAST_IN_LOCATION + 6) in float inAlphaMul;

void main() {
	vec4 color = pbr_base();

	float dist = max(length(fragUV - vec2(0.5)) - inMinDist, 0.0);
	float lerp = dist / (inMaxDist - inMinDist);
	float add = pow(lerp, inDistPow) + inMinLerp;
	vec4 col = inOverlay * add;

	col.a = col.a / (col.a + 1);
	color.rgb = color.rgb * max(1 - col.a, 0.0) + inOverlay.rgb * max(1.0, length(color.rgb)) * col.a;

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
