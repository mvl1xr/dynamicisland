#version 330 core

#define width 0.33  // % by X
#define height 0.33 // % by X
#define radius 0.1
#define lens_refraction 0.1
#define sharp 0.1

uniform vec2 iResolution;
uniform vec2 iMouse;
uniform sampler2D iChannel0;

in vec2 v_texCoord;
in vec2 v_pos;

out vec4 fragColor;

float _clamp(float a){
    return clamp(a,0.,1.);
}

float box( in vec2 p, in vec2 b, in vec4 r )
{
    r.xy = (p.x>0.0)?r.xy : r.zw;
    r.x  = (p.y>0.0)?r.x  : r.y;
    vec2 q = abs(p)-b+r.x;
    return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    fragColor = vec4(0);
    
    vec2 ir = iResolution.xy;
    vec2 wh = vec2(width,height)/2.0*ir.x/ir.y;
    vec4 vr = vec4(radius)/2.0*ir.x/ir.y;
    
    vec2 uv = fragCoord/ir;
    vec2 mouse = iMouse.xy;
    if (length(mouse)<1.0) {
        mouse = ir/2.0;
    } vec2 m2 = (uv-mouse/ir);

    // box
    float rb1 =  _clamp( -box(vec2(m2.x*ir.x/ir.y,m2.y), wh, vr)/sharp*32.0);
    // borders
    float rb2 =  _clamp(-box(vec2(m2.x*ir.x/ir.y,m2.y), wh+1.0/ir.y, vr)/sharp*16.0) - _clamp(-box(vec2(m2.x*ir.x/ir.y,m2.y), wh, vr)/sharp*16.0);
    // gradient
    float rb3 = _clamp(-box(vec2(m2.x*ir.x/ir.y,m2.y), wh+4.0/ir.y, vr)/sharp*4.0) - _clamp(-box(vec2(m2.x*ir.x/ir.y,m2.y), wh-4.0/ir.y, vr)/sharp*4.0);
    
    float transition = smoothstep(0.0, 1.0, rb1); 
    
    if (transition>0.0) {
    
        // Refraction
        vec2 lens = (uv-0.5)*sin(pow(
            _clamp(-box(vec2(m2.x*ir.x/ir.y,m2.y), wh, vr)/lens_refraction),
        0.25)*1.57)+0.5;
        
        // Blur
        float total = 0.0;
        for (float x = -4.0; x <= 4.0; x++) {
            for (float y = -4.0; y <= 4.0; y++) {
                vec2 blur = vec2(x, y) * 0.5 / ir;
                fragColor += texture(iChannel0, lens+blur);
                total += 1.0;
            }
        } fragColor/=total;
        
        // Lighting
        float gradient = _clamp(clamp(m2.y,0.0,0.2)+0.1)/2.0 + _clamp(clamp(-m2.y,-1.0,0.2)*rb3+0.1)/2.0;
        vec4 lighting = fragColor+1.0*vec4(rb2)+gradient*1.0;
        
        // Antialiasing
        fragColor = mix(texture(iChannel0, uv), lighting, transition);
        
    } else { 
        fragColor = texture(iChannel0, uv);
    }
}

void main() {
    mainImage(fragColor, gl_FragCoord.xy);
}
