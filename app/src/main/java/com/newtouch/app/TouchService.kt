package com.newtouch.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class TouchService : AccessibilityService() {

    companion object {
        @Volatile var instance: TouchService? = null
    }

    private var engine: DragEngine? = null
    private var floating: FloatingController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        TouchState.serviceConnected = true
        val f = FloatingController(this)
        floating = f
        engine = DragEngine(this) { kind ->
            when (kind) {
                End.OFF -> { f.hide(); TouchState.set(Mode.OFF) }
                End.PAUSE -> TouchState.set(Mode.PAUSED)
                End.STOP -> TouchState.set(Mode.READY)
            }
        }
        TouchState.set(Mode.OFF)
    }

    /** Mostra o botão flutuante. */
    fun enableFeature() {
        floating?.show()
        TouchState.set(Mode.READY)
    }

    /** Encerra o recurso: para o arraste e remove o botão flutuante. */
    fun disableFeature() {
        val e = engine
        if (e != null && e.isRunning) {
            e.requestEnd(End.OFF)
        } else {
            floating?.hide()
            TouchState.set(Mode.OFF)
        }
    }

    /** Iniciar / pausar / retomar. */
    fun toggleRun() {
        when (TouchState.mode) {
            Mode.READY, Mode.PAUSED -> engine?.start()
            Mode.RUNNING -> engine?.requestEnd(End.PAUSE)
            else -> {}
        }
    }

    private fun cleanup() {
        floating?.hide()
        instance = null
        TouchState.serviceConnected = false
        TouchState.set(Mode.OFF)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        engine?.let { if (it.isRunning) it.requestEnd(End.STOP) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }
}
