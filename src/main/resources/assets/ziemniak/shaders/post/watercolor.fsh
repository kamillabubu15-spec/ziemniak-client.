#version 150

// Ziemniak Client - Akwarela: rozmyte plamy koloru, ciemniejsze brzegi pigmentu, faktura papieru
uniform sampler2D InSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

void main() {
    vec2 px = 1.0 / OutSize;
    vec2 wob = vec2(noise(texCoord * 40.0), noise(texCoord * 40.0 + 7.0)) - 0.5;
    vec2 uv = texCoord + wob * px * 4.0;
    vec3 c = texture(InSampler, uv).rgb;
    vec3 soft = texture(BlurSampler, uv).rgb;
    c = mix(c, soft, 0.55);

    float e = abs(luma(texture(BlurSampler, uv + vec2(px.x * 2.0, 0.0)).rgb) - luma(texture(BlurSampler, uv - vec2(px.x * 2.0, 0.0)).rgb))
            + abs(luma(texture(BlurSampler, uv + vec2(0.0, px.y * 2.0)).rgb) - luma(texture(BlurSampler, uv - vec2(0.0, px.y * 2.0)).rgb));
    c *= 1.0 - smoothstep(0.04, 0.25, e) * 0.35;

    if (texture(DepthSampler, texCoord).r >= 0.99999) {
        c = mix(vec3(0.98, 0.96, 0.92), vec3(0.55, 0.75, 0.95), smoothstep(0.3, 1.0, texCoord.y) * (0.7 + 0.3 * noise(texCoord * 6.0)));
    }

    float l = luma(c);
    c = mix(vec3(l), c, 1.2);
    c = mix(c, vec3(1.0, 0.98, 0.95), 0.16);
    float paper = noise(texCoord * OutSize / 3.0) * 0.6 + noise(texCoord * OutSize / 14.0) * 0.4;
    c *= 0.92 + paper * 0.1;
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
