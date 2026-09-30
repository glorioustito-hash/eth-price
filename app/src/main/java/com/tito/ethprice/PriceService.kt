package com.tito.ethprice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import java.util.Locale
import kotlin.concurrent.thread

class PriceService : Service() {
    private val ch = "eth_live_min"
    @Volatile private var running = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(3)
        nm.createNotificationChannel(
            NotificationChannel(ch, "ETH price (in shade only)", NotificationManager.IMPORTANCE_MIN)
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

    private fun blankIcon(): Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))

    private fun build(t: Pair<Double, Double>?): Notification {
        val b = Notification.Builder(this, ch)
            .setSmallIcon(blankIcon())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openIntent())
        if (t == null) {
            b.setContentTitle("ETH ...")
        } else {
            val pct = (t.first - t.second) / t.second * 100
            val arrow = if (pct >= 0) "▲" else "▼"
            b.setContentTitle(String.format(Locale.US, "ETH €%,.2f", t.first))
                .setContentText(
                    String.format(Locale.US, "%s %.2f%% today", arrow, Math.abs(pct))
                )
        }
        return b.build()
    }
}
