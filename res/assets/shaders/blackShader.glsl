#vertex
#version 110
attribute vec3 position;
attribute vec2 texCoord;
attribute float color;

uniform mat4 PVMatrix;
uniform mat4 ModelMatrix;

void main (void) {
    gl_PointSize = 10.0f;
    gl_Position = PVMatrix * ModelMatrix * vec4(position, 1);
}

#fragment
#version 110

void main (void) {
    const vec4 black = vec4(0, 0, 0, 1);
    gl_FragColor = black;
}

#vertexlayout
// 0 position
// 1 texCoord
// 2 color