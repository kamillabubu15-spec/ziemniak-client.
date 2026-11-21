#version 150

// Ziemniak Client - Sny Sakury: spadajace platki wisni na ekranie
uniform sampler2D InSampler;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec4 petalLayer(vec2 uv, float scale, float speed, float seed) {
    vec2 p = uv * vec2(OutSize.x / OutSize.y, 1.0) * scale;
    p.y += Time * speed;
    p.x += Time * speed * 0.35 + sin(Time * 0.7 + p.y * 0.8 + seed) * 0.35;
    vec2 cell = floor(p);
    vec2 f = fract(p) - 0.5;
    float h = hash(cell + seed);
    if (h > 0.5) return vec4(0.0);
    vec2 q = f - (vec2(hash(cell + seed + 1.3), hash(cell + seed + 7.1)) - 0.5) * 0.5;
    float a = Time * (1.0 + h * 2.0) + h * 6.2831;
    q = mat2(cos(a), -sin(a), sin(a), cos(a)) * q;
    q.x *= 1.0 + 0.7 * abs(sin(Time * 2.0 + h * 10.0));
    float d = length(q * vec2(1.0, 1.7));
    float m = smoothstep(0.13, 0.1, d);
    vec3 col = mix(vec3(1.0, 0.62, 0.8), vec3(1.0, 0.92, 0.96), clamp(0.5 + q.y * 4.0, 0.0, 1.0));
    return vec4(col, m * 0.9);
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    vec4 a = petalLayer(texCoord, 4.0, 0.35, 0.0);
    vec4 b = petalLayer(texCoord, 7.0, 0.22, 17.0);
    c = mix(c, b.rgb, b.a * 0.7);
    c = mix(c, a.rgb, a.a);
    fragColor = vec4(c, 1.0);
}
