package com.tito.ethprice

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Locale
import kotlin.concurrent.thread

class PriceWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.tito.ethprice.REFRESH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PriceWidget::class.java))
            if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        scheduleNext(context)
        val pending = goAsync()
        thread {
            try {
                val p = Alerts.fetchPrice()
                if (p != null) Alerts.check(context, p)
                val text = if (p != null) String.format(Locale.US, "ETH €%,.2f", p) else "ETH error"
                val tap = Intent(context, PriceWidget::class.java).apply {
                    action = ACTION_REFRESH
                }
                val pi = PendingIntent.getBroadcast(
                    context, 0, tap,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                for (id in ids) {
                    val v = RemoteViews(context.packageName, R.layout.widget)
                    v.setTextViewText(R.id.price, text)
                    v.setOnClickPendingIntent(R.id.price, pi)
                    manager.updateAppWidget(id, v)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val i = Intent(context, PriceWidget::class.java).apply { action = ACTION_REFRESH }
        return PendingIntent.getBroadcast(
            context, 1, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleNext(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(
            AlarmManager.RTC,
            System.currentTimeMillis() + 60_000,
            refreshIntent(context)
        )
    }
}
