package com.newtouch.app

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

/** Animação suave ao pressionar (encolhe e volta). */
fun View.pressable() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
        }
        false
    }
}

fun Context.isTouchServiceEnabled(): Boolean {
    if (TouchState.serviceConnected) return true
    val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val cn = ComponentName(this, TouchService::class.java)
    return s.split(':').any {
        it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
    }
}

/** Liga/desliga o recurso (mostra ou esconde o botão flutuante). */
fun AppCompatActivity.toggleFeature() {
    if (!isTouchServiceEnabled()) {
        Toast.makeText(this, "Ative o serviço de Acessibilidade primeiro.", Toast.LENGTH_LONG).show()
        startActivity(android.content.Intent(this, PermissionsActivity::class.java))
        return
    }
    val service = TouchService.instance
    if (service == null) {
        Toast.makeText(this, "O serviço está iniciando. Tente novamente em instantes.", Toast.LENGTH_SHORT).show()
        return
    }
    if (TouchState.mode == Mode.OFF) service.enableFeature() else service.disableFeature()
}
