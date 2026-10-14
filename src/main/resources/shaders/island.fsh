#version 330 core

in vec2 v_texCoord;
in vec2 v_pos;

uniform vec2 u_resolution;
uniform vec2 u_capsulePos;
uniform vec2 u_capsuleSize;
uniform float u_radius;
uniform vec4 u_bgColor;       // Solid black #000000
uniform vec4 u_rimColor;      // Subtle ambient glass rim
uniform float u_rimWidth;

out vec4 FragColor;

// Analytical Signed Distance Field (SDF) of rounded box / capsule
float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 halfSize = u_capsuleSize * 0.5;
    vec2 center = u_capsulePos + halfSize;
    vec2 p = v_pos - center;
    
    float dist = sdRoundedBox(p, halfSize, u_radius);
    
    // Pixel-perfect anti-aliased edge smoothing using screen derivative
    float edgeSmooth = 1.0;
    float alpha = clamp(0.5 - dist / edgeSmooth, 0.0, 1.0);
    
    if (alpha <= 0.0) {
        discard;
    }
    
    // Ambient rim highlight along the capsule boundary
    float rimDist = abs(dist + u_rimWidth * 0.5) - u_rimWidth * 0.5;
    float rimFactor = clamp(0.5 - rimDist / edgeSmooth, 0.0, 1.0);
    
    vec4 finalColor = mix(u_bgColor, u_rimColor, rimFactor * u_rimColor.a);
    FragColor = vec4(finalColor.rgb, finalColor.a * alpha);
}
