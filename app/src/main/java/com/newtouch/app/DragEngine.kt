package com.newtouch.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max

enum class End { PAUSE, STOP, OFF }

/**
 * Executa o Arraste Contínuo com a API real de gestos da Acessibilidade.
 *
 * Estilo 0 ("sem soltar"): um único toque é mantido na tela. Cada trecho é enviado com
 * willContinue = true e o próximo continua do ponto exato onde o anterior parou
 * (StrokeDescription.continueStroke), indo e voltando entre dois pontos. O dedo "virtual"
 * nunca é levantado enquanto o recurso está rodando.
 *
 * Estilo 1 ("mesma direção"): o Android não deixa um toque pular de volta ao início sem
 * levantar, então o toque é solto e refeito imediatamente, sempre na mesma direção.
 */
class DragEngine(
    private val service: AccessibilityService,
    private val onEnded: (End) -> Unit
) {
    var isRunning = false
        private set

    private var endRequest: End? = null
    private var stroke: StrokeDescription? = null
    private val a = PointF()
    private val b = PointF()
    private var atA = true
    private var hold = true
    private var lastWasEnding = false

    fun start() {
        if (isRunning) return
        val m = service.resources.displayMetrics
        val w = m.widthPixels.toFloat()
        val h = m.heightPixels.toFloat()
        when (Prefs.direction(service)) {
            0 -> { a.set(w * 0.22f, h * 0.5f); b.set(w * 0.78f, h * 0.5f) }
            1 -> { a.set(w * 0.5f, h * 0.72f); b.set(w * 0.5f, h * 0.28f) }
            2 -> { a.set(w * 0.25f, h * 0.3f); b.set(w * 0.75f, h * 0.7f) }
            else -> { a.set(w * 0.25f, h * 0.7f); b.set(w * 0.75f, h * 0.3f) }
        }
        hold = Prefs.style(service) == 0
        atA = true
        stroke = null
        endRequest = null
        isRunning = true
        TouchState.set(Mode.RUNNING)
        sendNext()
    }

    fun requestEnd(kind: End) {
        if (!isRunning) { onEnded(kind); return }
        if (endRequest != End.OFF) endRequest = kind
    }

    private fun segmentMs(from: PointF, to: PointF): Long {
        val dist = hypot(to.x - from.x, to.y - from.y)
        val pxPerSec = 250f + Prefs.speed(service) * 330f
        return max(60L, (dist / pxPerSec * 1000f).toLong())
    }

    private fun sendNext() {
        val ending = endRequest != null
        if (ending && !hold) { finishRun(); return }

        val from = if (atA) a else b
        val to = if (atA) b else a
        val path = Path().apply { moveTo(from.x, from.y) }
        val duration: Long
        val willContinue: Boolean

        if (hold && ending) {
            duration = 10L              // finaliza o toque exatamente onde está
            willContinue = false
        } else {
            path.lineTo(to.x, to.y)
            duration = segmentMs(from, to)
            willContinue = hold
        }

        val prev = stroke
        val sd = if (hold && prev != null) {
            prev.continueStroke(path, 0L, duration, willContinue)
        } else {
            StrokeDescription(path, 0L, duration, willContinue)
        }
        stroke = if (willContinue) sd else null
        lastWasEnding = hold && ending

        val ok = service.dispatchGesture(
            GestureDescription.Builder().addStroke(sd).build(), callback, null
        )
        if (!ok) finishRun()
    }

    private val callback = object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            if (!isRunning) return
            if (lastWasEnding) { finishRun(); return }
            if (hold) atA = !atA
            sendNext()
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            if (isRunning) finishRun()
        }
    }

    private fun finishRun() {
        val kind = endRequest ?: End.STOP
        isRunning = false
        stroke = null
        endRequest = null
        onEnded(kind)
    }
}
