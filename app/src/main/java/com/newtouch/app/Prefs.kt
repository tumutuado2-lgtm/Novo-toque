package com.newtouch.app

import android.content.Context

/** Configurações salvas no aparelho. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("newtouch", Context.MODE_PRIVATE)

    /** 1 (lento) a 10 (rápido). */
    fun speed(c: Context) = sp(c).getInt("speed", 5)
    fun setSpeed(c: Context, v: Int) = sp(c).edit().putInt("speed", v).apply()

    /** 0 = horizontal, 1 = vertical, 2 = diagonal ↘↖, 3 = diagonal ↗↙. */
    fun direction(c: Context) = sp(c).getInt("direction", 0)
    fun setDirection(c: Context, v: Int) = sp(c).edit().putInt("direction", v).apply()

    /** 0 = sem soltar (vai e volta), 1 = mesma direção (reinicia rápido). */
    fun style(c: Context) = sp(c).getInt("style", 0)
    fun setStyle(c: Context, v: Int) = sp(c).edit().putInt("style", v).apply()
}
