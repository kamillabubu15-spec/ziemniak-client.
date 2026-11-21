#version 150

// Ziemniak Client - Retro VHS: drgajaca tasma, rozjechane kolory, linie, szum
uniform sampler2D InSampler;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    float line = floor(uv.y * OutSize.y / 2.0);
    float jitter = (hash(vec2(line, floor(Time * 24.0))) - 0.5) * 0.002
            + sin(uv.y * 40.0 + Time * 3.0) * 0.0012;
    float roll = fract(uv.y * 0.6 - Time * 0.12);
    float band = smoothstep(0.0, 0.03, roll) * smoothstep(0.08, 0.03, roll);
    uv.x += jitter + band * 0.006;

    float r = texture(InSampler, uv + vec2(0.0035, 0.0)).r;
    float g = texture(InSampler, uv).g;
    float b = texture(InSampler, uv - vec2(0.0035, 0.0)).b;
    vec3 c = vec3(r, g, b);
    c = (c + texture(InSampler, uv + vec2(0.002, 0.0)).rgb + texture(InSampler, uv - vec2(0.002, 0.0)).rgb) / 3.0 * 0.5 + c * 0.5;

    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, 0.8);
    c = c * vec3(1.06, 1.0, 0.9) * 0.92 + 0.05;
    c *= 1.0 - 0.12 * (0.5 + 0.5 * sin(texCoord.y * OutSize.y * 3.14159));
    c += (hash(texCoord * OutSize + Time * 61.0) - 0.5) * 0.09;
    c += band * 0.12;

    vec2 d = abs(texCoord - 0.5) * 2.0;
    float corner = smoothstep(1.0, 0.82, max(d.x * d.x * 0.6 + d.y * d.y * 0.6, max(d.x, d.y) * 0.98));
    c *= mix(0.25, 1.0, corner);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
