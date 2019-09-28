#vertex
#version 110
attribute vec3 position;
attribute vec2 texCoord;
attribute float lightIntensity;

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying float dbuff;

uniform mat4 PVMatrix;
uniform mat4 ModelMatrix;

void main (void) {
    gl_Position = PVMatrix * ModelMatrix * vec4(position, 1);
    frag_texCoord = texCoord;
    frag_lightIntensity = lightIntensity;

    dbuff = gl_Position.z;
}

#fragment
#version 110

varying float frag_lightIntensity;
varying vec2 frag_texCoord;
varying float dbuff;

uniform sampler2D texture0;

const float AMBIENT = 0.03;

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord);

    /*if (clr.a < 0.001)
        discard;*/

    gl_FragColor = vec4(clr.rgb * clamp(frag_lightIntensity + AMBIENT, 0.f, 1.f), clr.a);

    // Fog-like effect
    //gl_FragColor = vec4(clamp((1.f - dbuff/5.f) * (frag_lightIntensity + AMBIENT), 0.f, 1.f) * clr.rgb, clr.a);
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 lightIntensity