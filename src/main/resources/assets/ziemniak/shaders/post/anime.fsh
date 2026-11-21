#version 150

// Ziemniak Client - styl anime/kreskowki: kontury z glebi i kolorow + plaskie cieniowanie
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

uniform float Levels;
uniform float Saturation;
uniform float EdgeStrength;
uniform vec3 LineColor;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float linDepth(vec2 uv) {
    float z = texture(DepthSampler, uv).r * 2.0 - 1.0;
    float n = 0.05;
    float f = 512.0;
    return (2.0 * n * f) / (f + n - z * (f - n));
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;

    // kontury z glebi (krawedzie blokow, postaci, horyzont)
    float dc = linDepth(texCoord);
    float dl = linDepth(texCoord - vec2(oneTexel.x, 0.0));
    float dr = linDepth(texCoord + vec2(oneTexel.x, 0.0));
    float du = linDepth(texCoord - vec2(0.0, oneTexel.y));
    float dd = linDepth(texCoord + vec2(0.0, oneTexel.y));
    float depthEdge = (abs(dl - dc) + abs(dr - dc) + abs(du - dc) + abs(dd - dc)) / max(dc, 0.001);
    float e1 = smoothstep(0.06, 0.2, depthEdge);

    // kontury z kolorow (filtr Sobela na jasnosci)
    float tl = luma(texture(InSampler, texCoord + oneTexel * vec2(-1.0, -1.0)).rgb);
    float t = luma(texture(InSampler, texCoord + oneTexel * vec2(0.0, -1.0)).rgb);
    float tr = luma(texture(InSampler, texCoord + oneTexel * vec2(1.0, -1.0)).rgb);
    float l = luma(texture(InSampler, texCoord + oneTexel * vec2(-1.0, 0.0)).rgb);
    float r = luma(texture(InSampler, texCoord + oneTexel * vec2(1.0, 0.0)).rgb);
    float bl = luma(texture(InSampler, texCoord + oneTexel * vec2(-1.0, 1.0)).rgb);
    float b = luma(texture(InSampler, texCoord + oneTexel * vec2(0.0, 1.0)).rgb);
    float br = luma(texture(InSampler, texCoord + oneTexel * vec2(1.0, 1.0)).rgb);
    float gx = -tl - 2.0 * l - bl + tr + 2.0 * r + br;
    float gy = -tl - 2.0 * t - tr + bl + 2.0 * b + br;
    float e2 = smoothstep(0.6, 1.2, length(vec2(gx, gy)));

    float edge = clamp(max(e1, e2 * 0.3), 0.0, 1.0) * EdgeStrength;

    // niebo jak z anime: mocny blekit u gory, jasny przy horyzoncie
    if (texture(DepthSampler, texCoord).r >= 0.99999) {
        vec3 skyA = mix(vec3(0.62, 0.85, 1.0), vec3(0.22, 0.52, 0.98), smoothstep(0.35, 1.0, texCoord.y));
        float sun = smoothstep(0.85, 0.98, luma(c));
        c = mix(skyA, vec3(1.0), sun);
        fragColor = vec4(c, 1.0);
        return;
    }

    // plaskie cieniowanie (posteryzacja jasnosci z zachowaniem barwy)
    float lc = luma(c);
    float q = (floor(lc * Levels) + 0.6) / Levels;
    vec3 cel = c * (q / max(lc, 0.001));
    c = mix(c, cel, 0.7) * 1.12;

    // zywsze kolory
    float l2 = luma(c);
    c = mix(vec3(l2), c, Saturation);

    c = mix(c, LineColor, edge);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
