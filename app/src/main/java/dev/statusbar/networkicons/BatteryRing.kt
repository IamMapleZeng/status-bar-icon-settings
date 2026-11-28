package dev.statusbar.networkicons

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.SweepGradient
import android.graphics.RectF
import android.os.Build
import android.view.Surface
import android.view.View
import kotlin.math.cos

internal enum class RingChargingAnimation { OFF, SWEEP, COMET, BREATH, DOUBLE_SWEEP, WAVE, RAINBOW }

internal data class BatteryRingStyle(
    val color: Int,
    val animation: RingChargingAnimation,
    val widthDp: Int = 2,
)

/** Draws around the portrait camera cutout, without adding children to SystemUI containers. */
internal class BatteryRing(private val host: View, private val style: BatteryRingStyle) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()
    private val location = IntArray(2)
    private var rainbowGradient: SweepGradient? = null
    private val rainbowPosition = Matrix()

    private fun locate(): Boolean {
        // HyperOS on the Xiaomi 15 Ultra reserves a rectangular strip from the
        // top edge. The camera is centered within that strip, not on its edge.
        if (host.display?.rotation != Surface.ROTATION_0) return false
        val cutout = host.rootWindowInsets?.displayCutout ?: return false
        val bounds = cutout.boundingRects.singleOrNull { it.top == 0 && it.width() < host.width / 4 }
            ?: return false
        val camera = RectF(bounds)
        if (Build.VERSION.SDK_INT >= 31) {
            val pathBounds = RectF()
            cutout.cutoutPath?.computeBounds(pathBounds, true)
            // Prefer a genuine circular cutout path on ROMs that provide one.
            if (!pathBounds.isEmpty && pathBounds.top > 0 &&
                kotlin.math.abs(pathBounds.width() - pathBounds.height()) < pathBounds.width() * 0.15f
            ) camera.set(pathBounds)
        }
        if (camera.top == 0f && Build.DEVICE != "xuanyuan") return false
        host.getLocationOnScreen(location)
        val density = host.resources.displayMetrics.density
        val maxStroke = style.widthDp * density *
            if (style.animation == RingChargingAnimation.BREATH) 1.25f else 1f
        val radius = camera.width() / 2f + density + maxStroke / 2f
        val x = camera.centerX() - location[0]
        // Fine-tune the Xiaomi 15 Ultra's rectangular camera reservation.
        val verticalOffset = if (Build.DEVICE == "xuanyuan") 2 * density else 0f
        val y = camera.centerY() + verticalOffset - location[1]
        if (oval.centerX() != x || oval.centerY() != y || rainbowGradient == null) {
            rainbowGradient = SweepGradient(x, y, RAINBOW_COLORS, null)
        }
        oval.set(x - radius, y - radius, x + radius, y + radius)
        val margin = maxStroke / 2f
        return oval.left >= margin && oval.top >= margin &&
            oval.right + margin <= host.width && oval.bottom + margin <= host.height
    }

    fun draw(canvas: Canvas, level: Int, colors: BatteryLineColors?, animated: Boolean, progress: Float): Boolean {
        if (!locate()) return false
        val density = host.resources.displayMetrics.density
        val color = colors?.colorAt(level / 100.0) ?: style.color
        val mode = if (animated) style.animation else RingChargingAnimation.OFF
        val pulse = (1f - cos(progress * 2 * Math.PI).toFloat()) / 2f
        paint.strokeWidth = density * style.widthDp *
            if (mode == RingChargingAnimation.BREATH) 0.75f + pulse * 0.5f else 1f
        paint.color = color
        paint.alpha = (Color.alpha(color) * 0.18f).toInt()
        canvas.drawOval(oval, paint)
        paint.alpha = Color.alpha(color)
        val sweep = 360f * level.coerceIn(0, 10_000) / 10_000f
        if (mode == RingChargingAnimation.RAINBOW) {
            rainbowPosition.setRotate(progress * 360f - 90f, oval.centerX(), oval.centerY())
            rainbowGradient?.setLocalMatrix(rainbowPosition)
            paint.shader = rainbowGradient
        }
        if (sweep > 0) canvas.drawArc(oval, -90f, sweep, false, paint)
        paint.shader = null
        paint.color = if (Color.luminance(color) > 0.65) Color.BLACK else Color.WHITE
        when (mode) {
            RingChargingAnimation.SWEEP -> drawHighlight(canvas, sweep * progress, false)
            RingChargingAnimation.COMET -> drawHighlight(canvas, sweep * progress, true)
            RingChargingAnimation.DOUBLE_SWEEP -> {
                drawHighlight(canvas, sweep * progress, false)
                drawHighlight(canvas, sweep * (1f - progress), false)
            }
            RingChargingAnimation.WAVE -> {
                var start = 0f
                while (start < sweep) {
                    val strength = (1f + cos(start / 30f - progress * 2f * Math.PI).toFloat()) / 2f
                    paint.alpha = (180 * strength).toInt()
                    canvas.drawArc(oval, -90f + start, minOf(4f, sweep - start), false, paint)
                    start += 4f
                }
            }
            else -> Unit
        }
        return mode != RingChargingAnimation.OFF
    }

    private fun drawHighlight(canvas: Canvas, head: Float, comet: Boolean) {
        val count = if (comet) 7 else 1
        for (index in 0 until count) {
            val end = head - index * 7f
            val length = minOf(if (comet) 7f else 22f, end)
            if (length <= 0) continue
            paint.alpha = 200 * (count - index) / count
            canvas.drawArc(oval, -90f + end - length, length, false, paint)
        }
    }

}

internal val RAINBOW_COLORS = intArrayOf(
    0xFFFF3B30.toInt(), 0xFFFF9500.toInt(), 0xFFFFCC00.toInt(),
    0xFF34C759.toInt(), 0xFF00C7BE.toInt(), 0xFF007AFF.toInt(),
    0xFFAF52DE.toInt(), 0xFFFF3B30.toInt(),
)
