package com.island.phone.core.ui.glass

import android.annotation.TargetApi
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer

internal class LensInput(
    val width: Float,
    val height: Float,
    val shapes: FloatArray,
    val radii: FloatArray,
    val count: Int,
    val smooth: Float,
    val zone: Float,
    val ior: Float,
    val thick: Float,
    val dispersion: Float,
    val magnify: Float,
    val light: Float,
    val edgeDark: Float,
    val dp: Float,
    val tint: FloatArray,
    val ambient: FloatArray,
) {
    fun flat(): FloatArray {
        val a = FloatArray(39)
        var i = 0
        fun put(v: Float) {
            a[i++] = v
        }
        put(width)
        put(height)
        shapes.forEach { put(it) }
        radii.forEach { put(it) }
        put(count.toFloat())
        put(smooth)
        put(zone)
        put(ior)
        put(thick)
        put(dispersion)
        put(magnify)
        put(light)
        put(edgeDark)
        put(dp)
        tint.forEach { put(it) }
        ambient.forEach { put(it) }
        return a
    }
}

internal class GlassLens {
    private var impl: Any? = null

    fun apply(layer: GraphicsLayer, input: LensInput) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val lens = (impl as? LensImpl) ?: LensImpl().also { impl = it }
        lens.apply(layer, input)
    }
}

@TargetApi(33)
private class LensImpl {
    private val shader = RuntimeShader(LensSource)
    private var last: FloatArray? = null

    fun apply(layer: GraphicsLayer, input: LensInput) {
        val now = input.flat()
        if (last?.contentEquals(now) == true) return
        val s = input.shapes
        val r = input.radii
        val t = input.tint
        val a = input.ambient
        shader.setFloatUniform("size", input.width, input.height)
        shader.setFloatUniform("s0", s[0], s[1], s[2], s[3])
        shader.setFloatUniform("s1", s[4], s[5], s[6], s[7])
        shader.setFloatUniform("s2", s[8], s[9], s[10], s[11])
        shader.setFloatUniform("s3", s[12], s[13], s[14], s[15])
        shader.setFloatUniform("rad", r[0], r[1], r[2], r[3])
        shader.setFloatUniform("count", input.count.toFloat())
        shader.setFloatUniform("smoothK", input.smooth)
        shader.setFloatUniform("zone", input.zone)
        shader.setFloatUniform("ior", input.ior)
        shader.setFloatUniform("thick", input.thick)
        shader.setFloatUniform("dispersion", input.dispersion)
        shader.setFloatUniform("magnify", input.magnify)
        shader.setFloatUniform("light", input.light)
        shader.setFloatUniform("edgeDark", input.edgeDark)
        shader.setFloatUniform("dp", input.dp)
        shader.setFloatUniform("tint", t[0], t[1], t[2], t[3])
        shader.setFloatUniform("ambient", a[0], a[1], a[2])
        layer.renderEffect = android.graphics.RenderEffect
            .createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()
        last = now
    }
}

private const val LensSource = """
uniform shader content;
uniform float2 size;
uniform float4 s0;
uniform float4 s1;
uniform float4 s2;
uniform float4 s3;
uniform float4 rad;
uniform float count;
uniform float smoothK;
uniform float zone;
uniform float ior;
uniform float thick;
uniform float dispersion;
uniform float magnify;
uniform float light;
uniform float edgeDark;
uniform float dp;
uniform float4 tint;
uniform float3 ambient;

float rr(float2 p, float4 s, float r) {
    if (s.z < 0.5) {
        return 100000.0;
    }
    float2 q = abs(p - s.xy) - (s.zw - float2(r));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float sminK(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / max(k, 0.0001);
    return min(a, b) - h * h * k * 0.25;
}

float field(float2 p) {
    float d = rr(p, s0, rad.x);
    if (count > 1.5) {
        d = sminK(d, rr(p, s1, rad.y), smoothK);
    }
    if (count > 2.5) {
        d = sminK(d, rr(p, s2, rad.z), smoothK);
    }
    if (count > 3.5) {
        d = sminK(d, rr(p, s3, rad.w), smoothK);
    }
    return d;
}

float2 nearestCenter(float2 p) {
    float2 c = s0.xy;
    float best = rr(p, s0, rad.x);
    float e1 = rr(p, s1, rad.y);
    if (count > 1.5 && e1 < best) {
        best = e1;
        c = s1.xy;
    }
    float e2 = rr(p, s2, rad.z);
    if (count > 2.5 && e2 < best) {
        best = e2;
        c = s2.xy;
    }
    float e3 = rr(p, s3, rad.w);
    if (count > 3.5 && e3 < best) {
        best = e3;
        c = s3.xy;
    }
    return c;
}

half4 main(float2 p) {
    float sd = field(p);
    float cov = clamp(0.5 - sd, 0.0, 1.0);
    if (cov <= 0.0) {
        return half4(0.0);
    }
    float depth = max(-sd, 0.0);
    float t = clamp(depth / max(zone, 1.0), 0.0, 1.0);

    float gx = field(p + float2(1.0, 0.0)) - field(p - float2(1.0, 0.0));
    float gy = field(p + float2(0.0, 1.0)) - field(p - float2(0.0, 1.0));
    float2 g = float2(gx, gy);
    float gl = length(g);
    float2 dirOut = float2(0.0);
    if (gl > 0.0001) {
        dirOut = g / gl;
    }

    float u = max(1.0 - t, 0.0001);
    float u3 = u * u * u;
    float u4 = u3 * u;
    float fp = min(u3 / pow(max(1.0 - u4, 0.001), 0.75), 12.0);
    float f = pow(max(1.0 - u4, 0.0), 0.25);
    float slope = thick / max(zone, 1.0) * fp;
    float3 N = normalize(float3(dirOut * slope, 1.0));

    float n1 = max(ior, 1.0);
    float3 I = float3(0.0, 0.0, -1.0);
    float3 R1 = refract(I, N, 1.0 / n1);
    float T = thick * (0.25 + 0.75 * f);
    float2 d1 = R1.xy * (T / max(-R1.z, 0.05));
    float3 R2 = refract(R1, float3(0.0, 0.0, 1.0), n1);
    if (length(R2) < 0.001) {
        R2 = R1;
    }
    float2 d2 = R2.xy * (0.35 * thick / max(-R2.z, 0.05));
    float2 off = d1 + d2;
    float ol = length(off);
    float cap = zone * 2.2;
    if (ol > cap) {
        off = off * (cap / ol);
    }

    float wc = smoothstep(0.5, 0.9, t);
    float wo = 1.0 - smoothstep(0.0, 0.16, t);
    float wi = max(1.0 - wc - wo, 0.0);
    float zoom = 1.0 + magnify * 0.3;
    float2 cen = nearestCenter(p);
    float2 pc = cen + (p - cen) / zoom;
    float2 pin = p + off;
    float rb = 0.16 * zone;
    float2 po = p - dirOut * (rb - 2.0 * min(depth, rb));
    float ws = max(wc + wi + wo, 0.0001);
    float2 P = (pc * wc + pin * wi + po * wo) / ws;

    float2 dv = P - p;
    float2 lo = float2(0.5);
    float2 hi = size - float2(0.5);
    float2 pr = clamp(p + dv * (1.0 - dispersion * 0.25), lo, hi);
    float2 pg = clamp(P, lo, hi);
    float2 pb = clamp(p + dv * (1.0 + dispersion * 0.25), lo, hi);
    half4 cg = content.eval(pg);
    half3 col = half3(content.eval(pr).r, cg.g, content.eval(pb).b);

    col = mix(col, half3(tint.rgb), half(tint.a));

    float dk = exp(-depth / max(1.6 * dp, 0.5));
    col = col * half(1.0 - edgeDark * dk);

    float fres = pow(1.0 - N.z, 2.0);
    col = col + half3(ambient) * half(fres * light);

    float3 H1 = normalize(normalize(float3(-0.55, -0.75, 0.6)) + float3(0.0, 0.0, 1.0));
    float3 H2 = normalize(normalize(float3(0.55, 0.75, 0.6)) + float3(0.0, 0.0, 1.0));
    float sp = pow(max(dot(N, H1), 0.0), 30.0) + 0.6 * pow(max(dot(N, H2), 0.0), 30.0);
    sp = max(sp - 0.01, 0.0);
    col = col + half3(half(sp * 0.6 * light));

    return half4(clamp(col, half3(0.0), half3(1.0)) * half(cov), half(cov) * cg.a);
}
"""
