package com.brandonwallace.bastiontd.gl

/**
 * All GLSL ES 3.00 shaders for Bastion 3D TD.
 * Linear HDR lighting with Cook-Torrance GGX PBR, 9-tap hardware PCF shadows, geometric specular AA,
 * procedural biome surface detailing, HDR bloom, and ACES filmic tone mapping.
 */
object Shaders {
    private const val V = "#version 300 es\n"
    private const val FP = "precision highp float;\nprecision highp int;\nprecision highp sampler2D;\nprecision highp sampler2DShadow;\n"

    private const val NOISE = """
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float vnoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), f.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}
float fbm2(vec2 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        s += a * vnoise2(p);
        p = p * 2.03 + 13.7;
        a *= 0.5;
    }
    return s;
}
"""

    // ------------------------------------------------------------------ Shadow Depth Pass
    val SHADOW_VS = V + """
layout(location = 0) in vec3 aPos;
uniform mat4 uLightMVP;
void main() {
    gl_Position = uLightMVP * vec4(aPos, 1.0);
}
"""

    val SHADOW_FS = V + FP + """
out vec4 oColor;
void main() {
    oColor = vec4(1.0);
}
"""

    // ------------------------------------------------------------------ Full-Screen Quad Vertex Shader
    val FULL_VS = V + """
layout(location = 0) in vec2 aPos;
out vec2 vUV;
out vec2 vNdc;
void main() {
    vNdc = aPos;
    vUV = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    // ------------------------------------------------------------------ Planetary Sky Backdrop
    val SKY_FS = V + FP + NOISE + """
in vec2 vUV;
in vec2 vNdc;
out vec4 oColor;
uniform vec3 uZenith;
uniform vec3 uHorizon;
uniform vec3 uConduit;
uniform float uTime;
uniform float uOutScale;

void main() {
    float h = clamp(vUV.y, 0.0, 1.0);
    vec3 col = mix(uHorizon, uZenith, pow(h, 0.65));
    // Subtle nebula / aurora band in the upper sky
    float wave = sin(vNdc.x * 3.2 + uTime * 0.15) * 0.12;
    float band = exp(-abs(h - 0.68 - wave) * 7.5);
    float n = fbm2(vNdc * 3.5 + vec2(uTime * 0.04, 0.0));
    col += uConduit * band * n * 0.32;
    // Crisp starfield
    vec2 cell = floor(vUV * vec2(180.0, 320.0));
    float r = hash12(cell);
    if (r > 0.992 && h > 0.35) {
        float twinkle = 0.65 + 0.35 * sin(uTime * 3.0 + r * 90.0);
        col += vec3(0.85, 0.92, 1.0) * (r - 0.992) * 140.0 * twinkle * smoothstep(0.35, 0.7, h);
    }
    oColor = vec4(col * uOutScale, 1.0);
}
"""

    // ------------------------------------------------------------------ Cook-Torrance GGX PBR Lit Pass
    val LIT_VS = V + """
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNrm;
layout(location = 2) in vec3 aCol;
layout(location = 3) in vec2 aMat;
uniform mat4 uVP;
uniform mat4 uModel;
uniform mat3 uNrmMat;
uniform mat4 uShadowVP;
out vec3 vWPos;
out vec3 vN;
out vec3 vCol;
out vec2 vMat;
out vec3 vObj;
out vec4 vShadow;
void main() {
    vec4 w = uModel * vec4(aPos, 1.0);
    vWPos = w.xyz;
    vN = normalize(uNrmMat * aNrm);
    vCol = aCol;
    vMat = aMat;
    vObj = aPos;
    vShadow = uShadowVP * w;
    gl_Position = uVP * w;
}
"""

    val LIT_FS = V + FP + NOISE + """
in vec3 vWPos;
in vec3 vN;
in vec3 vCol;
in vec2 vMat;
in vec3 vObj;
in vec4 vShadow;
out vec4 oColor;

const float PI = 3.14159265;
uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform vec3 uSunCol;
uniform vec3 uSkyAmb;
uniform vec3 uGroundAmb;
uniform vec3 uTint;
uniform vec3 uEmissive;
uniform int uMatType; // 0: standard PBR, 1: terrain/canyon, 2: road with glowing chevrons, 3: glowing energy conduit
uniform vec3 uConduitCol;
uniform float uTime;
uniform sampler2DShadow uShadowMap;
uniform float uShadowOn;
uniform float uShadowTexel;
uniform int uShadowTaps;
uniform float uOutScale;

float shadowAt(vec3 n, vec3 l) {
    if (uShadowOn < 0.5) return 1.0;
    vec3 p = vShadow.xyz / vShadow.w * 0.5 + 0.5;
    if (p.x <= 0.01 || p.x >= 0.99 || p.y <= 0.01 || p.y >= 0.99 || p.z >= 0.999) return 1.0;
    float ndl = clamp(dot(n, l), 0.0, 1.0);
    float bias = uShadowTexel * (0.75 + 1.75 * sqrt(1.0 - ndl * ndl));
    float z = p.z - bias;
    if (uShadowTaps <= 1) return texture(uShadowMap, vec3(p.xy, z));
    float s = 0.0;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            s += texture(uShadowMap, vec3(p.xy + vec2(float(i), float(j)) * uShadowTexel * 1.2, z));
        }
    }
    return s / 9.0;
}

void main() {
    vec3 N = normalize(vN);
    if (!gl_FrontFacing) N = -N;
    // Compute derivative in uniform control flow to prevent quad lane artifacts on mobile GPUs
    vec3 dn = fwidth(N);

    vec3 albedo = vCol * uTint;
    float rough = clamp(vMat.x, 0.05, 1.0);
    float metal = clamp(vMat.y, 0.0, 1.0);
    vec3 emissive = uEmissive;

    if (uMatType == 1) {
        // Canyon stone / basalt / glacier procedural macro + micro detail
        float n1 = fbm2(vWPos.xz * 0.38);
        float n2 = vnoise2(vWPos.xz * 2.8);
        albedo *= (0.76 + 0.36 * n1 + 0.14 * (n2 - 0.5));
        // Subtle hex grid lines on the tactical floor
        vec2 g = abs(fract(vWPos.xz * 0.5) - 0.5);
        float gridLine = smoothstep(0.47, 0.495, max(g.x, g.y));
        emissive += uConduitCol * gridLine * 0.10 * step(-0.05, vWPos.y) * step(vWPos.y, 0.55);
    } else if (uMatType == 2) {
        // Tactical roadway with subtle animated energy pulse
        float n = vnoise2(vWPos.xz * 3.5);
        albedo *= (0.88 + 0.20 * n);
        float pulse = 0.5 + 0.5 * sin(vWPos.z * 1.6 - uTime * 3.8);
        float centerStripe = smoothstep(0.18, 0.04, abs(fract(vWPos.x * 0.25 + 0.5) - 0.5));
        emissive += uConduitCol * pulse * centerStripe * 0.14;
    } else if (uMatType == 3) {
        // Sunken magma / cryo / singularity conduit river
        float flow = fbm2(vWPos.xz * 0.9 + vec2(0.0, -uTime * 0.65));
        emissive += uConduitCol * (0.95 + 1.25 * flow);
        albedo = uConduitCol * 0.5;
    }

    // Geometric specular anti-aliasing
    rough = max(rough, min(0.55, length(dn) * 1.15));

    vec3 V = normalize(uCamPos - vWPos);
    vec3 L = normalize(uSunDir);
    vec3 H = normalize(V + L);
    float nl = max(dot(N, L), 0.0);
    float nv = max(dot(N, V), 1e-3);
    float nh = max(dot(N, H), 0.0);
    float vh = max(dot(V, H), 0.0);

    float a = rough * rough;
    float a2 = a * a;
    float dd = nh * nh * (a2 - 1.0) + 1.0;
    float D = a2 / (PI * dd * dd);
    float k = (rough + 1.0) * (rough + 1.0) / 8.0;
    float G = (nv / (nv * (1.0 - k) + k)) * (nl / (nl * (1.0 - k) + k));
    vec3 F0 = mix(vec3(0.04), albedo, metal);
    vec3 F = F0 + (1.0 - F0) * pow(1.0 - vh, 5.0);
    vec3 spec = D * G * F / max(4.0 * nv * nl, 1e-3);
    vec3 kd = (1.0 - F) * (1.0 - metal);

    float sh = shadowAt(N, L);
    vec3 col = (kd * albedo / PI + spec) * uSunCol * nl * sh;

    // Hemisphere ambient + analytic sky reflection
    vec3 hemi = mix(uGroundAmb, uSkyAmb, N.y * 0.5 + 0.5);
    float ao = 0.72 + 0.28 * sh;
    col += kd * albedo * hemi * ao;
    vec3 R = reflect(-V, N);
    vec3 envSky = mix(uGroundAmb, uSkyAmb * 1.45, smoothstep(-0.2, 0.5, R.y));
    col += envSky * F * (1.0 - rough * 0.75) * ao;

    // Rim silhouette highlight
    float rim = pow(1.0 - nv, 3.8);
    col += rim * uSkyAmb * 0.32;

    col += emissive;

    // Distance atmospheric perspective blend
    float dist = length(uCamPos - vWPos);
    float fog = smoothstep(28.0, 58.0, dist);
    col = mix(col, uGroundAmb * 0.85, fog * 0.45);

    oColor = vec4(col * uOutScale, 1.0);
}
"""

    // ------------------------------------------------------------------ Unlit / Translucent / Additive FX Pass
    val FX_VS = V + """
layout(location = 0) in vec3 aPos;
layout(location = 2) in vec3 aCol;
uniform mat4 uVP;
uniform mat4 uModel;
out vec3 vCol;
void main() {
    vCol = aCol;
    gl_Position = uVP * (uModel * vec4(aPos, 1.0));
}
"""

    val FX_FS = V + FP + """
in vec3 vCol;
out vec4 oColor;
uniform vec4 uColor;
uniform float uOutScale;
void main() {
    oColor = vec4(vCol * uColor.rgb * uOutScale, uColor.a);
}
"""

    // ------------------------------------------------------------------ 3D Point-Sprite Particle Pass
    val PARTICLE_VS = V + """
layout(location = 0) in vec4 aPosSize;
layout(location = 1) in vec4 aColAlpha;
uniform mat4 uVP;
uniform float uViewportH;
out vec4 vCol;
void main() {
    vCol = aColAlpha;
    gl_Position = uVP * vec4(aPosSize.xyz, 1.0);
    float w = max(gl_Position.w, 0.1);
    gl_PointSize = clamp(aPosSize.w * uViewportH / w, 2.0, 64.0);
}
"""

    val PARTICLE_FS = V + FP + """
in vec4 vCol;
out vec4 oColor;
uniform float uOutScale;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 > 1.0) discard;
    float falloff = (1.0 - r2) * (1.0 - r2);
    oColor = vec4(vCol.rgb * (1.6 * falloff) * uOutScale, vCol.a * falloff);
}
"""

    // ------------------------------------------------------------------ HDR Bloom & ACES Tone Mapping
    val BLOOM_DOWN_FS = V + FP + """
in vec2 vUV;
out vec4 oColor;
uniform sampler2D uTex;
uniform vec2 uTexel;
uniform int uFirstPass;
uniform float uOutScale;

void main() {
    vec3 a = texture(uTex, vUV + vec2(-1.0, -1.0) * uTexel).rgb;
    vec3 b = texture(uTex, vUV + vec2( 1.0, -1.0) * uTexel).rgb;
    vec3 c = texture(uTex, vUV + vec2(-1.0,  1.0) * uTexel).rgb;
    vec3 d = texture(uTex, vUV + vec2( 1.0,  1.0) * uTexel).rgb;
    vec3 e = texture(uTex, vUV).rgb;
    vec3 col = (a + b + c + d) * 0.125 + e * 0.5;
    if (uFirstPass == 1) {
        vec3 lin = col / uOutScale;
        float lum = max(lin.r, max(lin.g, lin.b));
        float knee = max(0.0, lum - 0.92) / max(lum, 1e-4);
        col = lin * knee * uOutScale;
    }
    oColor = vec4(col, 1.0);
}
"""

    val BLOOM_UP_FS = V + FP + """
in vec2 vUV;
out vec4 oColor;
uniform sampler2D uTex;
uniform vec2 uTexel;

void main() {
    vec3 s = texture(uTex, vUV).rgb * 4.0;
    s += texture(uTex, vUV + vec2(-uTexel.x, 0.0)).rgb * 2.0;
    s += texture(uTex, vUV + vec2( uTexel.x, 0.0)).rgb * 2.0;
    s += texture(uTex, vUV + vec2(0.0, -uTexel.y)).rgb * 2.0;
    s += texture(uTex, vUV + vec2(0.0,  uTexel.y)).rgb * 2.0;
    s += texture(uTex, vUV + vec2(-uTexel.x, -uTexel.y)).rgb;
    s += texture(uTex, vUV + vec2( uTexel.x, -uTexel.y)).rgb;
    s += texture(uTex, vUV + vec2(-uTexel.x,  uTexel.y)).rgb;
    s += texture(uTex, vUV + vec2( uTexel.x,  uTexel.y)).rgb;
    oColor = vec4(s / 16.0, 1.0);
}
"""

    val COMPOSITE_FS = V + FP + NOISE + """
in vec2 vUV;
out vec4 oColor;
uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uBloomOn;
uniform float uOutScale;

vec3 aces(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 hdr = texture(uScene, vUV).rgb / uOutScale;
    if (uBloomOn > 0.5) {
        hdr += (texture(uBloom, vUV).rgb / uOutScale) * 0.42;
    }
    // Subtle vignette framing the 3D tactical display
    vec2 q = vUV - 0.5;
    hdr *= 1.0 - 0.22 * dot(q, q);
    vec3 mapped = aces(hdr * 1.12);
    // Gamma encode to sRGB + 1-LSB triangular dither to eliminate banding
    vec3 srgb = pow(mapped, vec3(1.0 / 2.2));
    float dither = (hash12(gl_FragCoord.xy) - 0.5) / 255.0;
    oColor = vec4(srgb + dither, 1.0);
}
"""
}
