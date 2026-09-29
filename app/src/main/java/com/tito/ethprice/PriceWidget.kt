package com.tito.ethprice

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class PriceWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        thread {
            try {
                val price = fetch()
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val tap = Intent(context, PriceWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                val pi = PendingIntent.getBroadcast(
                    context, 0, tap,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                for (id in ids) {
                    val v = RemoteViews(context.packageName, R.layout.widget)
                    v.setTextViewText(R.id.price, price)
                    v.setTextViewText(R.id.time, "updated $time")
                    v.setOnClickPendingIntent(R.id.price, pi)
                    v.setOnClickPendingIntent(R.id.time, pi)
                    manager.updateAppWidget(id, v)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun fetch(): String = try {
        val c = URL("https://api.kraken.com/0/public/Ticker?pair=ETHEUR")
            .openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val body = c.inputStream.bufferedReader().use { it.readText() }
        val res = JSONObject(body).getJSONObject("result")
        val p = res.getJSONObject(res.keys().next())
            .getJSONArray("c").getString(0).toDouble()
        String.format(Locale.US, "ETH €%,.2f", p)
    } catch (e: Exception) {
        "ETH error"
    }
}
