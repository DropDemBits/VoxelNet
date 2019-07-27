#vertex
#version 110
attribute vec3 position;
attribute vec2 texCoord;

varying vec2 frag_texCoord;

uniform mat4 pvm;

void main (void) {
    gl_Position = pvm * vec4(position, 1);
    frag_texCoord = texCoord;
}

#fragment
#version 110

varying vec2 frag_texCoord;

uniform sampler2D texture0;

void main (void) {
    gl_FragColor = texture2D(texture0, frag_texCoord);
}

#vertexlayout
// 0 position
// 1 color