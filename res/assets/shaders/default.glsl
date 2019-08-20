#vertex
#version 110
attribute vec3 position;
attribute vec2 texCoord;
attribute float lightIntensity;

varying float frag_lightIntensity;
varying vec2 frag_texCoord;

uniform mat4 PVMatrix;
uniform mat4 ModelMatrix;

void main (void) {
    gl_Position = PVMatrix * ModelMatrix * vec4(position, 1);
    frag_texCoord = texCoord;
    frag_lightIntensity = lightIntensity;
}

#fragment
#version 110

varying float frag_lightIntensity;
varying vec2 frag_texCoord;

uniform sampler2D texture0;

const vec3 BLACK = vec3 (0);

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord);
    gl_FragColor = vec4(mix(BLACK, clr.rgb, frag_lightIntensity), clr.a);
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 lightIntensity