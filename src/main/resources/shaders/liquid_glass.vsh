#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoord;

uniform vec2 iResolution;

out vec2 v_texCoord;
out vec2 v_pos;

void main() {
    v_texCoord = aTexCoord;
    v_pos = aPosition;
    vec2 ndc = (aPosition / iResolution) * 2.0 - 1.0;
    ndc.y = -ndc.y;
    gl_Position = vec4(ndc, 0.0, 1.0);
}
