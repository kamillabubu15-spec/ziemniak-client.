#version 150

// Ziemniak Client - Neon Noc: turkus w cieniach, roz w swiatlach, rozszczepienie kolorow, nocne niebo z gwiazdami
uniform sampler2D InSampler;
uniform sampler2D BloomSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 dir = texCoord - 0.5;
    vec2 off = dir * 0.012;
    vec3 c = vec3(texture(InSampler, texCoord + off).r, texture(InSampler, texCoord).g, texture(InSampler, texCoord - off).b);
    vec3 bloom = texture(BloomSampler, texCoord).rgb;

    if (texture(DepthSampler, texCoord).r >= 0.99999) {
        vec3 sky = mix(vec3(0.85, 0.2, 0.65), vec3(0.06, 0.02, 0.16), smoothstep(0.25, 0.9, texCoord.y));
        vec2 g = floor(texCoord * OutSize / 3.0);
        float s = step(0.996, hash(g));
        sky += s * (0.6 + 0.4 * sin(Time * 3.0 + hash(g + 3.0) * 20.0));
        c = mix(sky, vec3(1.0), smoothstep(0.85, 0.98, luma(c)));
    }

    float l = luma(c);
    vec3 tone = mix(vec3(0.0, 0.55, 0.7), vec3(1.0, 0.25, 0.85), smoothstep(0.15, 0.75, l));
    c = mix(c, c * 0.45 + tone * l * 1.25, 0.65);
    c += bloom * vec3(1.0, 0.45, 1.0) * 1.3;
    c *= 1.0 - 0.07 * (0.5 + 0.5 * sin(texCoord.y * OutSize.y * 3.14159));
    c *= mix(0.55, 1.0, smoothstep(0.85, 0.25, length(dir * vec2(1.15, 1.0))));
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
