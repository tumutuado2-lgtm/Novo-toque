package com.newtouch.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Desenha o caminho dos toques recebidos e conta quantas vezes o dedo foi solto. */
class TraceView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var onStats: ((String) -> Unit)? = null

    private val path = Path()
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0866FF.toInt(); style = Paint.Style.STROKE
        strokeWidth = context.dp(4).toFloat()
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0866FF.toInt() }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x330866FF }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt(); textSize = context.dp(15).toFloat(); textAlign = Paint.Align.CENTER
    }

    private var downs = 0
    private var ups = 0
    private var moves = 0
    private var distance = 0f
    private var held = false
    private var downTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var seconds = 0.0

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downs++; held = true; downTime = e.eventTime
                path.moveTo(e.x, e.y); lastX = e.x; lastY = e.y
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(e.x, e.y)
                distance += hypot(e.x - lastX, e.y - lastY)
                lastX = e.x; lastY = e.y; moves++
                seconds = (e.eventTime - downTime) / 1000.0
                if (moves % 800 == 0) { path.reset(); path.moveTo(e.x, e.y) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                ups++; held = false
            }
        }
        publish()
        invalidate()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    fun clear() {
        path.reset(); downs = 0; ups = 0; moves = 0; distance = 0f; held = false; seconds = 0.0
        publish(); invalidate()
    }

    private fun publish() {
        onStats?.invoke(
            "Toques iniciados: $downs  ·  Dedo solto: $ups\n" +
                "Distância: ${distance.toInt()} px  ·  Em contato: ${"%.1f".format(seconds)} s"
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (downs == 0) {
            canvas.drawText("Área de teste", width / 2f, height / 2f, textPaint)
            return
        }
        canvas.drawPath(path, line)
        if (held) {
            canvas.drawCircle(lastX, lastY, context.dp(18).toFloat(), ringPaint)
            canvas.drawCircle(lastX, lastY, context.dp(7).toFloat(), dotPaint)
        }
    }
}
