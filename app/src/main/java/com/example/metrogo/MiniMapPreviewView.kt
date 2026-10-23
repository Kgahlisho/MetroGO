package com.example.metrogo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator

class MiniMapPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val density = resources.displayMetrics.density

    private class Route(val color: Int, val pts: Array<FloatArray>, val speed: Float, val phase: Float) {
        val path = Path()
        val measure = PathMeasure()
        var length = 0f
    }

    private val routes = listOf(
        Route(0xFF17A9BA.toInt(), arrayOf(f(.04f, .82f), f(.28f, .64f), f(.50f, .52f), f(.76f, .30f), f(.96f, .14f)), 1.0f, 0.00f),
        Route(0xFFC7E576.toInt(), arrayOf(f(.08f, .14f), f(.30f, .34f), f(.50f, .52f), f(.72f, .76f), f(.94f, .90f)), 0.8f, 0.35f),
        Route(0xFFFFB74D.toInt(), arrayOf(f(.04f, .50f), f(.26f, .46f), f(.50f, .52f), f(.80f, .58f), f(.97f, .50f)), 0.6f, 0.70f)
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B2540.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; alpha = 22; style = Paint.Style.STROKE; strokeWidth = density
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        strokeWidth = 3f * density; pathEffect = CornerPathEffect(14f * density)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density }

    private var progress = 0.3f
    private var animator: ValueAnimator? = null

    private val reduceMotion: Boolean
        get() = AccessibilitySettings.isReduceMotion(context)

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 12f * density)
            }
        }
        clipToOutline = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO // decorative; the card has the description
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        routes.forEach { r ->
            r.path.reset()
            r.pts.forEachIndexed { i, p ->
                if (i == 0) r.path.moveTo(p[0] * w, p[1] * h) else r.path.lineTo(p[0] * w, p[1] * h)
            }
            r.measure.setPath(r.path, false)
            r.length = r.measure.length
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (reduceMotion) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 9000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel(); animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val step = 22f * density
        var x = 0f
        while (x < w) { canvas.drawLine(x, 0f, x, h, gridPaint); x += step }
        var y = 0f
        while (y < h) { canvas.drawLine(0f, y, w, y, gridPaint); y += step }

        routes.forEach { r ->
            routePaint.color = r.color; routePaint.alpha = 170
            canvas.drawPath(r.path, routePaint)
        }

        // Interchange hub
        fill.color = Color.WHITE
        canvas.drawCircle(w * .50f, h * .52f, 6f * density, fill)
        fill.color = 0xFF0B2540.toInt()
        canvas.drawCircle(w * .50f, h * .52f, 3.5f * density, fill)

        val pos = FloatArray(2)
        routes.forEach { r ->
            if (r.length <= 0f) return@forEach
            val raw = (progress * r.speed + r.phase) % 1f
            val f = if (raw < .5f) raw * 2f else (1f - raw) * 2f   // ping-pong along the route
            r.measure.getPosTan(r.length * f, pos, null)

            val pulse = if (reduceMotion) 0f else (progress * 8f + r.phase) % 1f
            ring.color = r.color
            ring.alpha = (140 * (1f - pulse)).toInt()
            canvas.drawCircle(pos[0], pos[1], (6f + 10f * pulse) * density, ring)

            fill.color = Color.WHITE
            canvas.drawCircle(pos[0], pos[1], 5.5f * density, fill)
            fill.color = r.color
            canvas.drawCircle(pos[0], pos[1], 3.8f * density, fill)
        }
    }

    private fun f(x: Float, y: Float) = floatArrayOf(x, y)
}