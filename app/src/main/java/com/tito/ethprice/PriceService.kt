package com.tito.ethprice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import java.util.Locale
import kotlin.concurrent.thread

class PriceService : Service() {
    private val ch = "eth_live"
    @Volatile private var running = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(3)
        nm.createNotificationChannel(
            NotificationChannel(ch, "ETH live price", NotificationManager.IMPORTANCE_LOW)
        )
        val first = build(null)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(2, first)
        }
        if (!running) {
            running = true
            thread {
                while (running) {
                    val t = Alerts.fetchTicker()
                    if (t != null && running) {
                        Alerts.check(this, t.first)
                        nm.notify(2, build(t))
                    }
                    try { Thread.sleep(20000) } catch (e: InterruptedException) { }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun openIntent(): PendingIntent = PendingIntent.getActivity(
        this, 3, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun build(t: Pair<Double, Double>?): Notification {
        val b = Notification.Builder(this, ch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openIntent())
        if (t == null) {
            b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("ETH ...")
        } else {
            val pct = (t.first - t.second) / t.second * 100
            val arrow = if (pct >= 0) "▲" else "▼"
            b.setSmallIcon(priceIcon(t.first))
                .setContentTitle(String.format(Locale.US, "ETH €%,.2f", t.first))
                .setContentText(
                    String.format(Locale.US, "%s %.2f%% today", arrow, Math.abs(pct))
                )
        }
        return b.build()
    }

    private fun priceIcon(price: Double): Icon {
        val text = String.format(Locale.US, "%.0f", price)
        val mid = (text.length + 1) / 2
        return textIcon(listOf(text.substring(0, mid), text.substring(mid)))
    }

    private fun textIcon(lines: List<String>): Icon {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rowH = size / lines.size.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = ((rowH - 4f) / 0.72f).coerceAtMost(90f)
        }
        while (lines.any { p.measureText(it) > size - 2 } && p.textSize > 20f) {
            p.textSize -= 2f
        }
        val off = (p.descent() + p.ascent()) / 2f
        for ((i, l) in lines.withIndex()) {
            c.drawText(l, size / 2f, rowH * i + rowH / 2f - off, p)
        }
        return Icon.createWithBitmap(bmp)
    }
}
