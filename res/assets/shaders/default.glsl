#vertex
#version 110
attribute vec3 position;
attribute vec3 color;

varying vec3 frag_color;

void main (void) {
    gl_Position = vec4(position, 1);
    frag_color = color;
}

#fragment
#version 110

varying vec3 frag_color;

void main (void) {
    gl_FragColor = vec4(frag_color, 1);
}

#vertexlayout
// 0 position
// 1 color