#version 150

// Ziemniak Client - wyciaga jasne miejsca obrazu (do bloomu / poswiaty)
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
uniform float Threshold;
uniform vec3 Tint;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float sky = step(0.99999, texture(DepthSampler, texCoord).r);
    float k = smoothstep(mix(Threshold, 0.93, sky), 1.0, l);
    fragColor = vec4(c * k * Tint, 1.0);
}
