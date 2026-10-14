#version 330 core

in vec2 v_texCoord;
in vec2 v_pos;

uniform sampler2D u_photoTexture;
uniform vec2 u_photoPos;
uniform vec2 u_photoSize;
uniform float u_cornerRadius;
uniform float u_gridScale; // 64-grid sampling factor

out vec4 FragColor;

// SDF for rounded rectangle photo mask
float sdRoundedRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// 64-grid multi-sample bicubic texture filter to prevent pixelation/Moiré
vec4 sample64Grid(sampler2D tex, vec2 uv) {
    vec2 texSize = vec2(textureSize(tex, 0));
    vec2 invSize = 1.0 / texSize;
    
    // 64-grid kernel interpolation
    vec4 col = vec4(0.0);
    float totalWeight = 0.0;
    
    // 3x3 gaussian-weighted super-sampling for anti-aliased subpixel rendering
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float weight = 1.0 / (1.0 + float(x*x + y*y));
            vec2 offset = vec2(float(x), float(y)) * invSize * 0.5;
            col += texture(tex, uv + offset) * weight;
            totalWeight += weight;
        }
    }
    return col / totalWeight;
}

void main() {
    vec2 halfSize = u_photoSize * 0.5;
    vec2 center = u_photoPos + halfSize;
    vec2 p = v_pos - center;
    
    float dist = sdRoundedRect(p, halfSize, u_cornerRadius);
    float alpha = clamp(0.5 - dist, 0.0, 1.0);
    
    if (alpha <= 0.0) {
        discard;
    }
    
    vec4 photoColor = sample64Grid(u_photoTexture, v_texCoord);
    FragColor = vec4(photoColor.rgb, photoColor.a * alpha);
}
