#version 450
#extension GL_ARB_separate_shader_objects : enable

#include "pbr_frag_base.glsl"

layout(location = LAST_IN_LOCATION + 1) in vec4 inOverlay;

void main() {
	vec4 color = pbr_base();

	color.rgb = mix(color.rgb, inOverlay.rgb, inOverlay.a);

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
