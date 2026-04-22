package com.designlife.justdo_provider.presentation

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.designlife.justdo_provider.R
import com.designlife.justdo_provider.common.ProviderUtils
import com.designlife.justdo_provider.common.receiver.WidgetUpdateReceiver
import com.designlife.justdo_provider.common.scheduler.WidgetAlarmScheduler
import com.designlife.justdo_provider.presentation.view.TaskWidgetService

class AppWidget : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            // Start all three alarm chains the first time any widget is pinned
            WidgetAlarmScheduler.scheduleAll(context)
            // Immediate first-paint
            initialUpdate(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        try {
            WidgetAlarmScheduler.cancelAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetAlarmScheduler.scheduleAll(context)
        val now = System.currentTimeMillis()
        RemoteViews(context.packageName, R.layout.widget_task).also { views ->
            views.setTextViewText(R.id.dayText,   ProviderUtils.getCurrentDay(now))
            views.setTextViewText(R.id.dayNumber, ProviderUtils.getMonthDay(now))
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        WidgetUpdateReceiver().updateTemperature(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_task)

            val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.listView, serviceIntent)
            views.setEmptyView(R.id.listView, R.id.emptyWidgetTask)

            // ADD
            views.setOnClickPendingIntent(
                R.id.btnAdd,
                buildBroadcastPI(context, ProviderUtils.ACTION_NEW_TASK_CLICK, 1)
            )

            // REFRESH
            views.setOnClickPendingIntent(
                R.id.emptyTaskBtnRefresh,
                buildBroadcastPI(context, ProviderUtils.ACTION_REFRESH_CLICK, 2)
            )

            // CHAT
            views.setOnClickPendingIntent(
                R.id.btnChat,
                buildBroadcastPI(context, ProviderUtils.ACTION_CHAT_CLICK, 3)
            )

            // HOME
            views.setOnClickPendingIntent(
                R.id.setwork_home,
                buildBroadcastPI(context, ProviderUtils.ACTION_HOME, 4)
            )

            // Task-item click template (mutable — filled per item in adapter)
            val clickPI = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, AppWidget::class.java).apply {
                    action = ProviderUtils.ACTION_TASK_CLICK
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.listView, clickPI)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            when (intent.action) {

                ProviderUtils.ACTION_NEW_TASK_CLICK -> {
                    launchActivity(context, actionId = -1, taskId = -1)
                    WidgetUpdateReceiver().updateList(context)
                }

                ProviderUtils.ACTION_REFRESH_CLICK -> {
                    // Manual refresh — re-run all three updates immediately
                    WidgetUpdateReceiver().apply {
                        updateTemperature(context)
                        updateList(context)
                    }
                    val now = System.currentTimeMillis()
                    RemoteViews(context.packageName, R.layout.widget_task).also { v ->
                        v.setTextViewText(R.id.dayText,   ProviderUtils.getCurrentDay(now))
                        v.setTextViewText(R.id.dayNumber, ProviderUtils.getMonthDay(now))
                        v.setTextViewText(R.id.clockHour, ProviderUtils.getClockTime(now))
                        v.setTextViewText(R.id.clockHMA,  ProviderUtils.getClockHMA(now))
                        AppWidgetManager.getInstance(context)
                            .updateAppWidget(ComponentName(context, AppWidget::class.java), v)
                    }
                }

                ProviderUtils.ACTION_CHAT_CLICK ->
                    launchActivity(context, actionId = -2, taskId = -1)

                ProviderUtils.ACTION_TASK_CLICK -> {
                    val taskId = intent.getIntExtra(ProviderUtils.TASK_ID, -1)
                    if (taskId != -1) launchActivity(context, actionId = -3, taskId = taskId)
                }

                ProviderUtils.ACTION_HOME ->
                    launchActivity(context, actionId = -4, taskId = -1)

                ProviderUtils.ACTION_MORE_CLICK -> { /* reserved */ }
            }
        } catch (e: Exception) {
            Log.e("PROVIDER_FLOW", "onReceive: ${e.message}")
        }
    }

    private fun initialUpdate(context: Context) {
        val now = System.currentTimeMillis()
        RemoteViews(context.packageName, R.layout.widget_task).also { v ->
            v.setTextViewText(R.id.dayText,   ProviderUtils.getCurrentDay(now))
            v.setTextViewText(R.id.dayNumber, ProviderUtils.getMonthDay(now))
            v.setTextViewText(R.id.clockHour, ProviderUtils.getClockTime(now))
            v.setTextViewText(R.id.clockHMA,  ProviderUtils.getClockHMA(now))
            AppWidgetManager.getInstance(context)
                .updateAppWidget(ComponentName(context, AppWidget::class.java), v)
        }
        WidgetUpdateReceiver().updateTemperature(context)
        WidgetUpdateReceiver().updateList(context)
    }

    private fun launchActivity(context: Context, actionId: Int, taskId: Int) {
        val activity = Class.forName(ProviderUtils.CLASS_PATH).newInstance() as Activity
        context.startActivity(
            Intent(context, activity::class.java).apply {
                putExtra("fromProvider", true)
                putExtra("actionId", actionId)
                putExtra("taskId", taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
    }

    private fun buildBroadcastPI(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AppWidget::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}