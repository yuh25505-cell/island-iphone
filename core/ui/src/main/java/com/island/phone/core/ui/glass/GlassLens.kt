package com.island.phone.core.ui.glass

import android.annotation.TargetApi
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer

internal class GlassLens {
    private var impl: Any? = null

    fun apply(
        layer: GraphicsLayer,
        width: Float,
        height: Float,
        radius: Float,
        zone: Float,
        index: Float,
        thick: Float,
        dispersion: Float,
        magnify: Float,
        light: Float,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val lens = (impl as? LensImpl) ?: LensImpl().also { impl = it }
        lens.apply(layer, width, height, radius, zone, index, thick, dispersion, magnify, light)
    }
}

@TargetApi(33)
private class LensImpl {
    private val shader = RuntimeShader(LensSource)
    private var last: FloatArray? = null

    fun apply(
        layer: GraphicsLayer,
        width: Float,
        height: Float,
        radius: Float,
        zone: Float,
        index: Float,
        thick: Float,
        dispersion: Float,
        magnify: Float,
        light: Float,
    ) {
        val now = floatArrayOf(width, height, radius, zone, index, thick, dispersion, magnify, light)
        if (last?.contentEquals(now) == true) return
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("zone", zone)
        shader.setFloatUniform("index", index)
        shader.setFloatUniform("thick", thick)
        shader.setFloatUniform("dispersion", dispersion)
        shader.setFloatUniform("magnify", magnify)
        shader.setFloatUniform("light", light)
        layer.renderEffect = android.graphics.RenderEffect
            .createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()
        last = now
    }
}

private const val LensSource = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float zone;
uniform float index;
uniform float thick;
uniform float dispersion;
uniform float magnify;
uniform float light;

half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = p - c;
    float2 inner = size * 0.5 - float2(radius);
    float2 d = abs(q) - inner;
    float2 m = max(d, float2(0.0));
    float outer = length(m);
    float sd = outer + min(max(d.x, d.y), 0.0) - radius;
    float t = clamp(-sd / max(zone, 1.0), 0.0, 1.0);
    float u = max(1.0 - t, 0.0001);
    float u3 = u * u * u;
    float u4 = u3 * u;
    float slope = min(u3 / pow(max(1.0 - u4, 0.001), 0.75), 12.0);
    float alpha = atan(slope);
    float beta = asin(clamp(sin(alpha) / max(index, 1.0), -1.0, 1.0));
    float shift = tan(alpha - beta) * thick;
    float2 dir;
    if (outer > 0.0001) {
        dir = normalize(m) * sign(q);
    } else if (d.x > d.y) {
        dir = float2(sign(q.x), 0.0);
    } else {
        dir = float2(0.0, sign(q.y));
    }
    float zoom = 1.0 + magnify * 0.3;
    float2 base = c + q / zoom;
    float2 sp = base - dir * shift;
    float2 spread = dir * shift * dispersion * 0.6;
    float2 lo = float2(0.5);
    float2 hi = size - float2(0.5);
    float2 pr = clamp(sp - spread, lo, hi);
    float2 pg = clamp(sp, lo, hi);
    float2 pb = clamp(sp + spread, lo, hi);
    half4 g = content.eval(pg);
    half3 rgb = half3(content.eval(pr).r, g.g, content.eval(pb).b);
    float sa = sin(alpha);
    float2 L = normalize(float2(-0.6, -0.8));
    float facing = abs(dot(dir, L));
    float lit = sa * facing * facing;
    float dark = sa * (1.0 - facing * facing);
    rgb = rgb + half3(lit * 0.5 * light) - half3(dark * 0.12 * light);
    return half4(clamp(rgb, half3(0.0), half3(1.0)), g.a);
}
"""
