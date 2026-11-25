package com.island.phone.core.ui.glass

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlin.math.abs

enum class GlassUnit { INT, ONE_DECIMAL, PERCENT }

enum class GlassParam(
    val title: String,
    val min: Float,
    val max: Float,
    val default: Float,
    val unit: GlassUnit,
) {
    BLUR("模糊", 0f, 60f, 6f, GlassUnit.INT),
    REFRACTION("折射", 0f, 1f, 1f, GlassUnit.PERCENT),
    THICKNESS("厚度", 0f, 1f, 0.6f, GlassUnit.PERCENT),
    EDGE("边缘宽度", 0f, 12f, 2.7f, GlassUnit.ONE_DECIMAL),
    DISPERSION("色散", 0f, 1f, 0.08f, GlassUnit.PERCENT),
    MAGNIFY("放大", 0f, 1f, 0.2f, GlassUnit.PERCENT),
    TINT("着色", 0f, 2f, 0.8f, GlassUnit.PERCENT),
    SATURATION("饱和度", 0f, 2f, 1f, GlassUnit.PERCENT),
    HIGHLIGHT("高光", 0f, 2f, 1f, GlassUnit.PERCENT),
    SHADOW("阴影", 0f, 2f, 0.3f, GlassUnit.PERCENT),
}

class GlassSettings private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("glass_prefs", Context.MODE_PRIVATE)

    init {
        if (prefs.getInt(LOOK_KEY, 0) < LOOK_VERSION) {
            prefs.edit().clear().putInt(LOOK_KEY, LOOK_VERSION).apply()
        }
    }

    private val states = GlassParam.entries.associateWith { param ->
        mutableStateOf(prefs.getFloat(param.name, param.default).coerceIn(param.min, param.max))
    }

    fun valueOf(param: GlassParam): Float = states.getValue(param).value

    fun set(param: GlassParam, value: Float) {
        val fixed = value.coerceIn(param.min, param.max)
        states.getValue(param).value = fixed
        prefs.edit().putFloat(param.name, fixed).apply()
    }

    fun reset() {
        GlassParam.entries.forEach { states.getValue(it).value = it.default }
        prefs.edit().clear().putInt(LOOK_KEY, LOOK_VERSION).apply()
    }

    val isDefault: Boolean
        get() = GlassParam.entries.all { abs(valueOf(it) - it.default) < 0.0001f }

    val blur: Float get() = valueOf(GlassParam.BLUR)
    val refraction: Float get() = valueOf(GlassParam.REFRACTION)
    val thickness: Float get() = valueOf(GlassParam.THICKNESS)
    val edge: Float get() = valueOf(GlassParam.EDGE)
    val dispersion: Float get() = valueOf(GlassParam.DISPERSION)
    val magnify: Float get() = valueOf(GlassParam.MAGNIFY)
    val tint: Float get() = valueOf(GlassParam.TINT)
    val saturation: Float get() = valueOf(GlassParam.SATURATION)
    val highlight: Float get() = valueOf(GlassParam.HIGHLIGHT)
    val shadow: Float get() = valueOf(GlassParam.SHADOW)

    val lensActive: Boolean
        get() = (refraction > 0.001f && thickness > 0.001f) || magnify > 0.001f || highlight > 0.001f

    companion object {
        private const val LOOK_KEY = "look_version"
        private const val LOOK_VERSION = 2

        @Volatile
        private var instance: GlassSettings? = null

        fun get(context: Context): GlassSettings =
            instance ?: synchronized(this) {
                instance ?: GlassSettings(context.applicationContext).also { instance = it }
            }
    }
}
