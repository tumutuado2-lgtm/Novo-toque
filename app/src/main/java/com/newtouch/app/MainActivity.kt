package com.newtouch.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var dot: View
    private lateinit var ring: View
    private lateinit var statusText: TextView
    private lateinit var mainButton: LinearLayout
    private lateinit var mainIcon: ImageView
    private lateinit var mainLabel: TextView
    private var pulse: ObjectAnimator? = null
    private var lastMode: Mode? = null
    private val listener: (Mode) -> Unit = { render(it) }

    private val dirIds = intArrayOf(R.id.chipH, R.id.chipV, R.id.chipD1, R.id.chipD2)
    private val styleIds = intArrayOf(R.id.chipHold, R.id.chipRestart)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dot = findViewById(R.id.statusDot)
        ring = findViewById(R.id.statusRing)
        statusText = findViewById(R.id.statusText)
        mainButton = findViewById(R.id.mainButton)
        mainIcon = findViewById(R.id.mainIcon)
        mainLabel = findViewById(R.id.mainLabel)

        mainButton.pressable()
        mainButton.setOnClickListener { toggleFeature() }

        findViewById<View>(R.id.btnTest).apply {
            pressable()
            setOnClickListener { startActivity(Intent(this@MainActivity, TestActivity::class.java)) }
        }
        findViewById<View>(R.id.btnPerms).apply {
            pressable()
            setOnClickListener { startActivity(Intent(this@MainActivity, PermissionsActivity::class.java)) }
        }

        val speedValue = findViewById<TextView>(R.id.speedValue)
        val slider = findViewById<Slider>(R.id.speedSlider)
        slider.value = Prefs.speed(this).toFloat()
        speedValue.text = "${Prefs.speed(this)}/10"
        slider.addOnChangeListener { _, v, _ ->
            Prefs.setSpeed(this, v.toInt())
            speedValue.text = "${v.toInt()}/10"
        }

        val dirGroup = findViewById<ChipGroup>(R.id.dirGroup)
        dirGroup.check(dirIds[Prefs.direction(this).coerceIn(0, 3)])
        dirGroup.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isNotEmpty()) Prefs.setDirection(this, dirIds.indexOf(ids[0]))
        }
        val styleGroup = findViewById<ChipGroup>(R.id.styleGroup)
        styleGroup.check(styleIds[Prefs.style(this).coerceIn(0, 1)])
        styleGroup.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isNotEmpty()) Prefs.setStyle(this, styleIds.indexOf(ids[0]))
        }

        // Entrada animada dos cartões
        val content = findViewById<LinearLayout>(R.id.content)
        for (i in 0 until content.childCount) {
            val c = content.getChildAt(i)
            c.alpha = 0f
            c.translationY = 40f
            c.animate().alpha(1f).translationY(0f).setStartDelay(60L * i).setDuration(380).start()
        }
    }

    override fun onResume() {
        super.onResume()
        TouchState.add(listener)
        lastMode = null
        render(TouchState.mode)
    }

    override fun onPause() {
        super.onPause()
        TouchState.remove(listener)
    }

    private fun render(mode: Mode) {
        val perm = isTouchServiceEnabled()
        val (text, colorRes) = when {
            !perm -> "Permissão pendente" to R.color.nt_amber
            mode == Mode.OFF -> "Desativado" to R.color.nt_gray
            mode == Mode.READY -> "Pronto: use o botão flutuante" to R.color.nt_blue
            mode == Mode.RUNNING -> "Arrastando continuamente" to R.color.nt_green
            else -> "Pausado" to R.color.nt_amber
        }
        val color = ContextCompat.getColor(this, colorRes)
        val tint = ColorStateList.valueOf(color)
        dot.backgroundTintList = tint
        ring.backgroundTintList = tint
        statusText.text = text

        pulse?.cancel(); pulse = null
        ring.alpha = 0f
        if (perm && mode != Mode.OFF) {
            pulse = ObjectAnimator.ofPropertyValuesHolder(
                ring,
                PropertyValuesHolder.ofFloat("scaleX", 1f, if (mode == Mode.RUNNING) 2.2f else 1.6f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, if (mode == Mode.RUNNING) 2.2f else 1.6f),
                PropertyValuesHolder.ofFloat("alpha", 0.5f, 0f)
            ).apply {
                duration = if (mode == Mode.RUNNING) 900 else 1500
                repeatCount = android.animation.ValueAnimator.INFINITE
                start()
            }
        }

        val active = mode != Mode.OFF
        mainButton.setBackgroundResource(if (active) R.drawable.bg_main_outline else R.drawable.bg_main_primary)
        val fg = ContextCompat.getColor(this, if (active) R.color.nt_blue else R.color.nt_white)
        mainLabel.setTextColor(fg)
        mainIcon.imageTintList = ColorStateList.valueOf(fg)
        mainIcon.setImageResource(if (active) R.drawable.ic_stop else R.drawable.ic_play)
        mainLabel.text = if (active) "Desativar Arraste Contínuo" else "Ativar Arraste Contínuo"

        if (lastMode != null && lastMode != mode) {
            mainButton.animate().scaleX(1.04f).scaleY(1.04f).setDuration(120).withEndAction {
                mainButton.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }.start()
        }
        lastMode = mode
    }
}
