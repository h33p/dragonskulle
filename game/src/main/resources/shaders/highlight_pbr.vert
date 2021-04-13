#version 450
#extension GL_ARB_separate_shader_objects : enable

#include "pbr_vert_base.glsl"

layout(location = LAST_IN_LOCATION + 1) in vec4 inOverlay;
layout(location = LAST_OUT_LOCATION + 1) out vec4 outOverlay;

void main() {
	pbr_base();
	outOverlay = inOverlay;
}
