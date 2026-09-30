package com.tito.ethprice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object Alerts {
    private const val CHANNEL = "eth_alert"

    fun fetchPrice(): Double? = try {
        val c = URL("https://api.kraken.com/0/public/Ticker?pair=ETHEUR")
            .openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val body = c.inputStream.bufferedReader().use { it.readText() }
        val res = JSONObject(body).getJSONObject("result")
        res.getJSONObject(res.keys().next())
            .getJSONArray("c").getString(0).toDouble()
    } catch (e: Exception) {
        null
    }

    fun check(context: Context, price: Double) {
        val prefs = context.getSharedPreferences("alert", Context.MODE_PRIVATE)
        val target = prefs.getFloat("target", 0f)
        if (target <= 0f) return
        val armed = prefs.getBoolean("armed", true)
        if (price >= target) {
            if (armed) {
                notify(context, price, target)
                prefs.edit().putBoolean("armed", false).apply()
            }
        } else if (!armed) {
            prefs.edit().putBoolean("armed", true).apply()
        }
    }

    private fun notify(context: Context, price: Double, target: Float) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL, "ETH price alert", NotificationManager.IMPORTANCE_HIGH)
        ch.enableVibration(true)
        ch.vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        nm.createNotificationChannel(ch)
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ETH alert")
            .setContentText(
                String.format(Locale.US, "€%,.2f is over your €%,.2f target", price, target)
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
    }
}
