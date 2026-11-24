package com.newtouch.app

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.max

/**
 * Botão flutuante arrastável. É uma janela de sobreposição da própria Acessibilidade
 * (TYPE_ACCESSIBILITY_OVERLAY), por isso não precisa da permissão "Sobrepor a outros apps".
 */
class FloatingController(private val service: TouchService) {

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var handle: ImageView? = null
    private var menu: LinearLayout? = null
    private var btnToggle: ImageView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var pulse: ObjectAnimator? = null
    private var expanded = false
    private val listener: (Mode) -> Unit = { update(it) }

    fun show() {
        if (root != null) return
        val m = service.resources.displayMetrics
        val size = service.dp(56)

        val hd = ImageView(service).apply {
            setImageResource(R.drawable.ic_logo)
            setBackgroundResource(R.drawable.bg_floating)
            val p = service.dp(14)
            setPadding(p, p, p, p)
            elevation = service.dp(6).toFloat()
            layoutParams = LinearLayout.LayoutParams(size, size)
            contentDescription = "Botão flutuante New Touch"
        }

        val blue = ContextCompat.getColor(service, R.color.nt_blue)
        val red = ContextCompat.getColor(service, R.color.nt_red)
        val toggle = circleButton(R.drawable.ic_play, blue) {
            service.toggleRun(); setExpanded(false)
        }
        val close = circleButton(R.drawable.ic_close, red) {
            setExpanded(false); service.disableFeature()
        }
        val settings = circleButton(R.drawable.ic_settings, blue) {
            setExpanded(false)
            val i = Intent(service, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            service.startActivity(i)
        }

        val mn = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_floating_menu)
            val p = service.dp(6)
            setPadding(p, p, p, p)
            visibility = View.GONE
            addView(toggle); addView(settings); addView(close)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = service.dp(8) }
        }

        val r = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(hd); addView(mn)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = m.widthPixels - size - service.dp(12)
            y = (m.heightPixels * 0.12f).toInt()
        }

        attachDrag(hd, r, params)
        root = r; handle = hd; menu = mn; btnToggle = toggle; lp = params
        expanded = false
        wm.addView(r, params)
        TouchState.add(listener)
        update(TouchState.mode)
    }

    fun hide() {
        pulse?.cancel(); pulse = null
        root?.let { runCatching { wm.removeView(it) } }
        root = null; handle = null; menu = null; btnToggle = null; lp = null
        expanded = false
        TouchState.remove(listener)
    }

    private fun circleButton(icon: Int, tint: Int, onClick: () -> Unit): ImageView =
        ImageView(service).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(tint)
            setBackgroundResource(R.drawable.bg_circle_light)
            val p = service.dp(11)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(service.dp(44), service.dp(44))
                .apply { topMargin = service.dp(3); bottomMargin = service.dp(3) }
            setOnClickListener { onClick() }
            pressable()
        }

    private fun attachDrag(handle: View, root: View, params: WindowManager.LayoutParams) {
        val slop = ViewConfiguration.get(service).scaledTouchSlop
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        handle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y; moved = false
                    handle.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        clampAndApply()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handle.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                    if (e.actionMasked == MotionEvent.ACTION_UP && !moved) setExpanded(!expanded)
                }
            }
            true
        }
    }

    private fun setExpanded(v: Boolean) {
        expanded = v
        menu?.visibility = if (v) View.VISIBLE else View.GONE
        menu?.alpha = 0f
        menu?.animate()?.alpha(1f)?.setDuration(160)?.start()
        root?.post { clampAndApply() }
    }

    private fun clampAndApply() {
        val r = root ?: return
        val p = lp ?: return
        val m = service.resources.displayMetrics
        val rw = if (r.width > 0) r.width else service.dp(56)
        val rh = if (r.height > 0) r.height else service.dp(56)
        p.x = p.x.coerceIn(0, max(0, m.widthPixels - rw))
        p.y = p.y.coerceIn(0, max(0, m.heightPixels - rh))
        runCatching { wm.updateViewLayout(r, p) }
    }

    private fun update(mode: Mode) {
        btnToggle?.setImageResource(if (mode == Mode.RUNNING) R.drawable.ic_pause else R.drawable.ic_play)
        val hd = handle ?: return
        if (mode == Mode.RUNNING) {
            if (pulse == null) {
                pulse = ObjectAnimator.ofFloat(hd, "alpha", 1f, 0.55f).apply {
                    duration = 700
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    start()
                }
            }
        } else {
            pulse?.cancel(); pulse = null
            hd.alpha = 1f
        }
    }
}
