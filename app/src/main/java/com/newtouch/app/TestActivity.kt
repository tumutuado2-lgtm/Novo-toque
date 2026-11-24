package com.newtouch.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** Tela de teste: mostra os toques reais recebidos, inclusive quantas vezes o "dedo" foi solto. */
class TestActivity : AppCompatActivity() {

    private lateinit var trace: TraceView
    private lateinit var hint: TextView
    private lateinit var btnFeature: MaterialButton
    private val listener: (Mode) -> Unit = { render(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        trace = findViewById(R.id.trace)
        hint = findViewById(R.id.hint)
        btnFeature = findViewById(R.id.btnFeature)
        val stats = findViewById<TextView>(R.id.stats)
        trace.onStats = { stats.text = it }
        trace.clear()

        findViewById<View>(R.id.btnBack).apply { pressable(); setOnClickListener { finish() } }
        findViewById<View>(R.id.btnClear).apply { pressable(); setOnClickListener { trace.clear() } }
        btnFeature.apply { pressable(); setOnClickListener { toggleFeature() } }
    }

    override fun onResume() {
        super.onResume()
        TouchState.add(listener)
        render(TouchState.mode)
    }

    override fun onPause() {
        super.onPause()
        TouchState.remove(listener)
    }

    private fun render(mode: Mode) {
        btnFeature.text = if (mode == Mode.OFF) "Ativar recurso" else "Desativar recurso"
        hint.text = when {
            !isTouchServiceEnabled() -> "Primeiro ative a Acessibilidade em Permissões."
            mode == Mode.OFF -> "1) Toque em “Ativar recurso”."
            mode == Mode.READY -> "2) Toque no botão flutuante e depois em ▶."
            mode == Mode.RUNNING -> "Arrastando… o traço azul mostra o toque real. Pause com ⏸."
            else -> "Pausado. Toque no botão flutuante e em ▶ para retomar."
        }
    }
}
