package com.newtouch.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionsActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        status = findViewById(R.id.permStatus)

        findViewById<android.view.View>(R.id.btnBack).apply {
            pressable(); setOnClickListener { finish() }
        }
        findViewById<android.view.View>(R.id.btnAccessibility).apply {
            pressable()
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        findViewById<android.view.View>(R.id.btnAppInfo).apply {
            pressable()
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ok = isTouchServiceEnabled()
        status.text = if (ok) "Status: ativado ✓" else "Status: ainda não ativado"
        status.setTextColor(ContextCompat.getColor(this, if (ok) R.color.nt_green else R.color.nt_amber))
    }
}
