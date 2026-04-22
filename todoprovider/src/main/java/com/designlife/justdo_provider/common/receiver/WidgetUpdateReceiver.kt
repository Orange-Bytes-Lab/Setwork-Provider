package com.designlife.justdo_provider.common.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import com.designlife.justdo_provider.R
import com.designlife.justdo_provider.common.ProviderServiceLocator
import com.designlife.justdo_provider.common.ProviderUtils
import com.designlife.justdo_provider.common.scheduler.WidgetAlarmScheduler
import com.designlife.justdo_provider.presentation.AppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetAlarmScheduler.ACTION_CLOCK_TICK -> {
                updateClock(context)
                WidgetAlarmScheduler.scheduleClockAlarm(context)
            }
            Intent.ACTION_TIME_TICK -> {
                updateClock(context)
            }

            WidgetAlarmScheduler.ACTION_TEMP_TICK -> {
                updateTemperature(context)
                WidgetAlarmScheduler.scheduleTempAlarm(context)
            }

            WidgetAlarmScheduler.ACTION_DAILY_TICK -> {
                updateDate(context)
                updateList(context)
                WidgetAlarmScheduler.scheduleDailyAlarm(context)
            }

            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                WidgetAlarmScheduler.scheduleAll(context)
            }

            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                updateDate(context)
                updateClock(context)
                WidgetAlarmScheduler.scheduleClockAlarm(context)
                WidgetAlarmScheduler.scheduleDailyAlarm(context)
            }
        }
    }

    private fun updateClock(context: Context) {
        try {
            val now = System.currentTimeMillis()
            val views = RemoteViews(context.packageName, R.layout.widget_task).apply {
                setTextViewText(R.id.clockHour, ProviderUtils.getClockTime(now))
                setTextViewText(R.id.clockHMA,  ProviderUtils.getClockHMA(now))
            }
            appWidgetManager(context).updateAppWidget(component(context), views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun updateDate(context: Context) {
        try {
            val now = System.currentTimeMillis()
            val views = RemoteViews(context.packageName, R.layout.widget_task).apply {
                setTextViewText(R.id.dayText,   ProviderUtils.getCurrentDay(now))
                setTextViewText(R.id.dayNumber, ProviderUtils.getMonthDay(now))
            }
            appWidgetManager(context).updateAppWidget(component(context), views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTemperature(context: Context) {
        try {
            ProviderServiceLocator.provideWeatherUpdateRepository().let { repo ->
                CoroutineScope(Dispatchers.IO).launch {
                    repo.fetchReleaseUpdates()?.let {
                        withContext(Dispatchers.Main.immediate) {
                            updateTemperatureInfo(context)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateTemperatureInfo(context)
        }
    }

    private fun updateTemperatureInfo(context: Context) {
        try {
            val now     = System.currentTimeMillis()
            val weather = ProviderUtils.APP_WEATHER_REPORT
            val icon: Icon = getWeatherIcon(
                context,
                weather.currentWeather.weatherCode,
                weather.currentWeather.isDay
            )
            val views = RemoteViews(context.packageName, R.layout.widget_task).apply {
                setTextViewText(R.id.clockHour,    ProviderUtils.getClockTime(now))
                setTextViewText(R.id.clockHMA,     ProviderUtils.getClockHMA(now))
                setTextViewText(R.id.clockTemp,    "%.1f".format(weather.currentWeather.temperature))
                setImageViewIcon(R.id.clockTempIcon, icon)
            }
            appWidgetManager(context).updateAppWidget(component(context), views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateList(context: Context) {
        try {
            val manager = appWidgetManager(context)
            manager.notifyAppWidgetViewDataChanged(
                manager.getAppWidgetIds(component(context)),
                R.id.listView
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appWidgetManager(context: Context) =
        AppWidgetManager.getInstance(context)

    private fun component(context: Context) =
        ComponentName(context, AppWidget::class.java)

    private fun getWeatherIcon(context: Context, weatherCode: Int, isDay: Int): Icon {
        @DrawableRes val resId = when (weatherCode) {
            0                  -> if (isDay == 1) R.drawable.ic_temp_day else R.drawable.ic_temp_night
            1, 2, 3            -> R.drawable.ic_temp_cloud
            45, 48             -> R.drawable.ic_temp_cloud
            51, 53, 55         -> R.drawable.ic_temp_rain
            56, 57             -> R.drawable.ic_temp_rain
            61, 63, 65         -> R.drawable.ic_temp_rain
            66, 67             -> R.drawable.ic_temp_rain
            71, 73, 75         -> R.drawable.ic_temp_snow
            77                 -> R.drawable.ic_temp_snow
            80, 81, 82         -> R.drawable.ic_temp_rain
            85, 86             -> R.drawable.ic_temp_snow
            95, 96, 99         -> R.drawable.ic_temp_rain
            else               -> R.drawable.ic_temp_day
        }
        return Icon.createWithResource(context, resId)
    }
}