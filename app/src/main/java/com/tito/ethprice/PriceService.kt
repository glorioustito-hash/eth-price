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
import android.graphics.Rect
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
        val h = 96
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 104f
        }
        val w = (p.measureText(text) + 8f).toInt().coerceAtLeast(h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Rect()
        p.getTextBounds(text, 0, text.length, r)
        val y = h / 2f - (r.top + r.bottom) / 2f
        c.drawText(text, w / 2f, y, p)
        return Icon.createWithBitmap(bmp)
    }
}
