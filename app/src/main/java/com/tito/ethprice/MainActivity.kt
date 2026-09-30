package com.tito.ethprice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var tv: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { load(); handler.postDelayed(this, 10000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val prefs = getSharedPreferences("alert", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(48, 48, 48, 48)
        }
        tv = TextView(this).apply {
            textSize = 44f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            text = "..."
        }
        val status = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            textSize = 14f
        }
        val input = EditText(this).apply {
            hint = "Alert when ETH is over (€)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        val saved = prefs.getFloat("target", 0f)
        if (saved > 0f) {
            input.setText(saved.toString())
            status.text = String.format(Locale.US, "Alert on: over €%,.2f", saved)
        } else {
            status.text = "Alert off"
        }
        val save = Button(this).apply {
            text = "Save alert"
            setOnClickListener {
                val v = input.text.toString().replace(',', '.').toFloatOrNull()
                if (v == null || v <= 0f) {
                    prefs.edit().putFloat("target", 0f).apply()
                    status.text = "Alert off"
                } else {
                    prefs.edit().putFloat("target", v).putBoolean("armed", true).apply()
                    status.text = String.format(Locale.US, "Alert on: over €%,.2f", v)
                }
            }
        }
        val live = Button(this).apply {
            val on = prefs.getBoolean("live", false)
            text = if (on) "Live notification: ON" else "Live notification: OFF"
            setOnClickListener {
                val now = !prefs.getBoolean("live", false)
                prefs.edit().putBoolean("live", now).apply()
                val i = Intent(this@MainActivity, PriceService::class.java)
                if (now) startForegroundService(i) else stopService(i)
                text = if (now) "Live notification: ON" else "Live notification: OFF"
            }
        }
        root.addView(tv)
        root.addView(input)
        root.addView(save)
        root.addView(status)
        root.addView(live)
        setContentView(root)
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun load() {
        thread {
            val p = Alerts.fetchPrice()
            if (p != null) Alerts.check(this, p)
            runOnUiThread {
                tv.text = if (p != null) String.format(Locale.US, "ETH\n€%,.2f", p) else "Error"
            }
        }
    }
}
