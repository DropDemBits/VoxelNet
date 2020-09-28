#vertex
#version 120
attribute vec3 position;
attribute vec2 texCoord;
attribute vec3 lightValues;

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying vec4 viewCoord;

uniform mat4 ProjectMatrix;
uniform mat4 ViewMatrix;
uniform mat4 ModelMatrix;

uniform float iTime;

const float[8*4] faceIntensities = float[8*4](
    //   S       N       E       W       U       D
    //0.750f, 0.750f, 0.825f, 0.825f, 0.950f, 0.600f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f,
    0.825f, 0.825f, 0.950f, 0.950f, 1.000f, 0.750f, 0.0f, 0.0f
);

// Light from the sky (percent, 0-1)
float sunBright = 1.f;
const float interval = (2 * 3.14159265f);

void main (void) {
    const float test = 0.1f;
    float nee = mod(test, 1.0f);

    gl_Position = ProjectMatrix * ViewMatrix * ModelMatrix * vec4(position, 1);
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
    finalLight = exp2((1.f - (finalLight)) * log2(0.2f)) * faceIntensities[faceIndex];

    frag_lightIntensity = finalLight;

    viewCoord = ViewMatrix * ModelMatrix * vec4(position, 1);
}

#fragment
#version 110

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying vec4 viewCoord;

uniform sampler2D texture0;
uniform bool inWater;

const float AMBIENT_LIGHTING = 0.00;
const vec4 WATER_COLOR = vec4(0.44, 0.4, 0.6, 1.0);
const float FOG_DENSITY = 0.125;
const float INV_LOG2E = 1.442695;

const float gamma = 2.2f;
const float inv_gamma = 1.f / 2.2f;

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord);
    clr.rgb = pow(clr.rgb, vec3(2));

    if (clr.a < 0.001)
        discard;

    float computedLight = frag_lightIntensity + AMBIENT_LIGHTING;
    clr.rgb = vec3(clr.rgb * clamp(computedLight, 0.f, 1.f));

    clr.rgb = sqrt(clr.rgb);

    if (inWater) {
        // Water fog
        // fogFactor = e^(-(density * gl_FragCoord.z)^2.0)
        float z = (viewCoord.z / viewCoord.w);

        float fogFactor = exp2(-FOG_DENSITY * FOG_DENSITY * z * z * INV_LOG2E);
        fogFactor = clamp(fogFactor, 0.0, 1.0);

        gl_FragColor = vec4(mix(WATER_COLOR.rgb, clr.rgb, fogFactor), clr.a);
    } else {
        gl_FragColor = clr;
    }
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 lightIntensity