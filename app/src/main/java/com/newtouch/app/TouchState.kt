package com.newtouch.app

import android.os.Handler
import android.os.Looper

enum class Mode { OFF, READY, RUNNING, PAUSED }

/** Estado compartilhado entre o serviço, o botão flutuante e as telas. */
object TouchState {
    @Volatile var mode: Mode = Mode.OFF
        private set
    @Volatile var serviceConnected: Boolean = false

    private val listeners = LinkedHashSet<(Mode) -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun set(m: Mode) {
        mode = m
        main.post { listeners.toList().forEach { it(m) } }
    }

    fun add(l: (Mode) -> Unit) { listeners.add(l) }
    fun remove(l: (Mode) -> Unit) { listeners.remove(l) }
}
