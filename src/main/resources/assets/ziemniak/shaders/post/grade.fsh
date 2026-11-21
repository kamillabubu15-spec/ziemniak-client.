#version 150

// Ziemniak Client - korekcja kolorow: bloom, split-toning, kontrast, nasycenie, winieta
uniform sampler2D InSampler;
uniform sampler2D BloomSampler;
uniform sampler2D DepthSampler;

uniform float BloomStrength;
uniform vec3 ShadowTint;
uniform vec3 HighlightTint;
uniform float ToneStrength;
uniform float Saturation;
uniform float Contrast;
uniform float Exposure;
uniform float Vignette;
uniform vec3 VignetteTint;
uniform float Haze;
uniform vec3 SkyTop;
uniform vec3 SkyMid;
uniform vec3 SkyBottom;
uniform float SkyMix;
uniform vec3 Balance;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;

    // wlasne niebo: piksele bez geometrii (glebia = 1) dostaja gradient, slonce/ksiezyc zostaja jasne
    float depth = texture(DepthSampler, texCoord).r;
    float skyMask = smoothstep(0.9993, 1.0, depth) * SkyMix;
    if (skyMask > 0.0) {
        float y = texCoord.y;
        vec3 grad = mix(SkyBottom, SkyMid, smoothstep(0.2, 0.55, y));
        grad = mix(grad, SkyTop, smoothstep(0.55, 1.0, y));
        float bright = smoothstep(0.82, 0.98, luma(c));
        vec3 sky = mix(grad * (0.85 + 0.3 * luma(c)), vec3(1.0, 0.97, 0.9), bright);
        c = mix(c, sky, skyMask);
    }

    // poswiata + delikatna mgielka
    c += bloom * BloomStrength;
    c = mix(c, c + bloom * 0.6 + vec3(luma(bloom)) * 0.3, Haze);
    c *= Exposure * Balance;

    // kontrast wokol szarosci
    c = (c - 0.5) * Contrast + 0.5;
    c = clamp(c, 0.0, 1.0);

    // split-toning przez "screen": cienie i swiatla dostaja pastelowy odcien, barwy zostaja
    float l = luma(c);
    vec3 tone = ShadowTint * (1.0 - smoothstep(0.0, 0.6, l)) + HighlightTint * smoothstep(0.35, 1.0, l);
    c = 1.0 - (1.0 - c) * (1.0 - tone * ToneStrength);

    // nasycenie
    l = luma(c);
    c = mix(vec3(l), c, Saturation);

    // winieta w kolorze
    vec2 d = texCoord - 0.5;
    float v = smoothstep(0.85, 0.2, length(d * vec2(1.15, 1.0)));
    vec3 edge = c * mix(vec3(1.0), VignetteTint, Vignette);
    c = mix(edge, c, v);

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
