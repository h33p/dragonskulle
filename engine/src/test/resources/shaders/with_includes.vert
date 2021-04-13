#version 450

#include "include_base.glsl"
#include <include_base2.glsl>

void main() {
	vec3 ab = muldot(vec3(1), vec3(1, 0, 3));
	ab += adddot(vec3(1), vec3(1, 0, 3));
}
