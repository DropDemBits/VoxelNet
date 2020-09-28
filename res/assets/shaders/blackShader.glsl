#vertex
#version 110
attribute vec3 position;

uniform mat4 ProjectMatrix;
uniform mat4 ViewMatrix;
uniform mat4 ModelMatrix;

void main (void) {
    gl_PointSize = 10.0f;
    gl_Position = ProjectMatrix * ViewMatrix * ModelMatrix * vec4(position, 1);
}

#fragment
#version 110

void main (void) {
    const vec4 black = vec4(0, 0, 0, 1);
    gl_FragColor = black;
}

#vertexlayout
// 0 position