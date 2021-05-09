#version 450
#extension GL_ARB_separate_shader_objects : enable

#include "pbr_vert_base.glsl"

layout(location = LAST_IN_LOCATION + 1) in vec4 inOverlay;
layout(location = LAST_IN_LOCATION + 2) in float inMinDist;
layout(location = LAST_IN_LOCATION + 3) in float inMaxDist;
layout(location = LAST_IN_LOCATION + 4) in float inMinLerp;
layout(location = LAST_IN_LOCATION + 5) in float inDistPow;
layout(location = LAST_IN_LOCATION + 6) in float inAlphaMul;

layout(location = LAST_OUT_LOCATION + 1) out vec4 outOverlay;
layout(location = LAST_OUT_LOCATION + 2) out float outMinDist;
layout(location = LAST_OUT_LOCATION + 3) out float outMaxDist;
layout(location = LAST_OUT_LOCATION + 4) out float outMinLerp;
layout(location = LAST_OUT_LOCATION + 5) out float outDistPow;
layout(location = LAST_OUT_LOCATION + 6) out float outAlphaMul;

void main() {
	pbr_base();
	outOverlay = inOverlay;
	outMinDist = inMinDist;
	outMaxDist = inMaxDist;
	outMinLerp = inMinLerp;
	outDistPow = inDistPow;
	outAlphaMul = inAlphaMul;
}
