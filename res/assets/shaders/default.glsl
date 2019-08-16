#vertex
#version 110
attribute vec3 position;
attribute vec2 texCoord;
attribute vec4 lightColor;

varying vec4 frag_lightColor;
varying vec2 frag_texCoord;

uniform mat4 pvm;

void main (void) {
    gl_Position = pvm * vec4(position, 1);
    frag_texCoord = texCoord;
    frag_lightColor = lightColor;
}

#fragment
#version 110

varying vec4 frag_lightColor;
varying vec2 frag_texCoord;

uniform sampler2D texture0;

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord);
    gl_FragColor = vec4(mix(frag_lightColor.rgb, clr.rgb, frag_lightColor.a), clr.a);
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 lightColor