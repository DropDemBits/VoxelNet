#vertex
#version 110
attribute vec2 position;
attribute vec2 texCoord;
attribute vec4 color;

varying vec2 frag_texCoord;
varying vec4 frag_color;

uniform mat4 PVMatrix;

void main (void) {
    gl_Position = PVMatrix * vec4(position, 0, 1);
    frag_texCoord = texCoord;
    frag_color = color;
}

#fragment
#version 110

varying vec2 frag_texCoord;
varying vec4 frag_color;

uniform sampler2D texture0;

void main (void) {
    vec4 clr = texture2D(texture0, frag_texCoord) * frag_color;

    //if (clr.a < 0.001)
    //    discard;

    gl_FragColor = clr;
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 color