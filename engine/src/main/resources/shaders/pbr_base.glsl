
const float EPSILON = 1e-16;
const float PI = 3.14159265358979323846;

// Some magic, it scales the peak sphere light intensity down to roughly
// match how intensity behaves in blender.
// TODO: fix this utter hack. Perhaps finally try to understand some of
// those big brain math equations, you dummy.
const float MAGIC = pow(PI, 5) / sqrt(2);

vec3 fresnelSchlick(float hv, vec3 reflectivity) {
	return reflectivity + (vec3(1.0) - reflectivity) * pow(1.0 - hv, 5);
}

float distributionGGX(float nh, float roughness, float peakOffset) {
	float alpha = roughness * roughness;
	float alphaSqr = alpha * alpha;
	float div = nh * nh * (alphaSqr - 1.0 / peakOffset) + 1.0 / peakOffset;
	div *= div;
	return max(alphaSqr, EPSILON) / max(div, EPSILON);
}

// Modified Schlick's GGX formula. We incorporate peak offset for peak light brightness.
float schlickGGX(float nv, float roughness, float peakOffset) {
	return nv / (nv * (peakOffset + roughness) + roughness / peakOffset);
}

float geomSmith(float nv, float nl, float roughness, float peakOffset) {
	roughness += 1.0;
	roughness = roughness * roughness / 8.0;
	return schlickGGX(nv, roughness, peakOffset) * schlickGGX(nl, roughness, peakOffset);
}

// Find the most direct light vector to the reflection one
vec3 sampleLVec(vec3 viewVec, vec3 normalVec, vec3 lVec, float sinAng) {
	vec3 viewReflect = reflect(viewVec, normalVec);

	vec3 centerToRay = dot(lVec, viewReflect) * viewReflect - lVec;

	return normalize(lVec + centerToRay * min(sinAng / length(centerToRay), 1.0));
}

vec3 calcLightShading(vec3 baseReflectivity, vec3 viewVec, vec3 lightVec, vec3 normalVec, vec3 lightIntensity, vec3 albedo, float roughness, float metalness) {
	roughness = max(roughness, 0.01);
	vec3 lVec = -normalize(lightVec);

	// Controls the size of light circle
	float sinAng = 0.05;

	float area = sinAng * sinAng * MAGIC;
	float peakOffset = max(area, 1.0);

	lVec = sampleLVec(viewVec, normalVec, lVec, sinAng);

	vec3 hVec = normalize(viewVec + lVec);
	float distance = 1.0;
	float attenuation = 1.0 / (distance * distance);
	vec3 radiance = lightIntensity * attenuation;

	float nv = max(dot(normalVec, viewVec), 0.0);
	float nl = max(dot(normalVec, lVec), 0.0);
	float hv = max(dot(hVec, viewVec), 0.0);
	float nh = max(dot(normalVec, hVec), 0.0);

	vec3 fresnel = fresnelSchlick(hv, baseReflectivity);
	float distribution = distributionGGX(nh, roughness, peakOffset);
	float geometry = geomSmith(max(nv, EPSILON), max(nl, EPSILON), roughness, peakOffset);

	// Specular BRDF reflection
	vec3 spec = fresnel * distribution * geometry;
	spec /= max(4.0 * nv * nl, EPSILON);

	// Diffuse lighting based on conservation of energy
	vec3 diffuse = mix(vec3(1.0) - fresnel, vec3(0.0), metalness);
	diffuse *= albedo;

	return (diffuse + spec) * radiance * nl;
}

// Get the TBN matrix. Based on:
// http://www.thetenthplanet.de/archives/1180
mat3 getTBN(vec3 normal, vec3 worldVec, vec2 uv) {
	// Calculate edge vectors
	vec3 dpX = dFdx(worldVec);
	vec3 dpY = dFdy(worldVec);
	vec2 duvX = dFdx(uv);
	vec2 duvY = dFdy(uv);

	vec3 dpYperp = cross(dpY, normal);
	vec3 dpXperp = cross(normal, dpX);
	vec3 T = dpYperp * duvX.x + dpXperp * duvY.x;
	vec3 B = dpYperp * duvX.y + dpXperp * duvY.y;

	float inv = inversesqrt(max(dot(T, T), dot(B, B)));
	return mat3(T * inv, B * inv, normal);
}
