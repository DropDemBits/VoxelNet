#vertex
#version 120
attribute vec3 position;
attribute vec2 texCoord;
attribute vec3 lightValues;

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying float dbuff;
varying float frag_faceIntensity;

uniform mat4 PVMatrix;
uniform mat4 ModelMatrix;

uniform float iTime;

const float[8*4] faceIntensities = float[8*4](
    //   S       N       E       W       U       D
    0.750f, 0.750f, 0.825f, 0.825f, 0.950f, 0.600f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f
);

// Light from the sky (percent, 0-1)
float sunBright = 1.f;
const float interval = (2 * 3.14159265f);

void main (void) {
    gl_Position = PVMatrix * ModelMatrix * vec4(position, 1);
    frag_texCoord = texCoord;

    // Compute light value
    // skyLight/shadow, blockLight, face & aoLight
    sunBright = (0.5f) * (sin((iTime / 45.f) * interval) + 1.f);

    int faceIndex = int(lightValues.z);

    float blockLight = lightValues.y / 4.0f;
    float skyLight = lightValues.x / 4.0f;
    float maxLight = max(skyLight, blockLight);
    float effectiveLight = blockLight * (1 - sunBright) + maxLight * sunBright;

    float finalLight = effectiveLight / 15.0f;

    frag_faceIntensity = faceIntensities[faceIndex];
    frag_lightIntensity = finalLight;

    dbuff = gl_Position.z;
}

#fragment
#version 110

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying float dbuff;
varying float frag_faceIntensity;

uniform sampler2D texture0;
uniform bool inWater;

const float AMBIENT = 0.03;
const vec4 WATER_COLOR = vec4(0.44, 0.4, 0.6, 1.0);

const float gamma = 2.2f;
const float inv_gamma = 1.f / 2.2f;

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord);

    if (clr.a < 0.001)
        discard;

    if (inWater)
        clr *= WATER_COLOR;

    float finalLight = exp2((1.f - (frag_lightIntensity)) * log2(0.2f));
    float computedLight = finalLight * frag_faceIntensity + AMBIENT;
    clr.rgb = vec3(clr.rgb * clamp(computedLight, 0.f, 1.f));

    gl_FragColor = clr;
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), clr.a);

    // Fog-like effect
    //gl_FragColor = vec4(clamp((1.f - dbuff/5.f) * (frag_lightIntensity + AMBIENT), 0.f, 1.f) * clr.rgb, clr.a);
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 lightIntensity