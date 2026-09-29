package com.tito.ethprice

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
        tv = TextView(this).apply {
            textSize = 44f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            text = "..."
        }
        setContentView(tv)
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun load() {
        thread {
            val out = try {
                val c = URL("https://api.kraken.com/0/public/Ticker?pair=ETHEUR")
                    .openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val res = JSONObject(body).getJSONObject("result")
                val p = res.getJSONObject(res.keys().next())
                    .getJSONArray("c").getString(0).toDouble()
                String.format(Locale.US, "ETH\n€%,.2f", p)
            } catch (e: Exception) { "Error" }
            runOnUiThread { tv.text = out }
        }
    }
}
