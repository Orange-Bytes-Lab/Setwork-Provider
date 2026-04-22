package com.designlife.justdo_provider.common.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.designlife.justdo_provider.common.receiver.WidgetUpdateReceiver

object WidgetAlarmScheduler {

    const val ACTION_CLOCK_TICK = "com.designlife.justdo_provider.ACTION_CLOCK_TICK"
    const val ACTION_TEMP_TICK  = "com.designlife.justdo_provider.ACTION_TEMP_TICK"
    const val ACTION_DAILY_TICK = "com.designlife.justdo_provider.ACTION_DAILY_TICK"

    private const val RC_CLOCK = 100
    private const val RC_TEMP  = 101
    private const val RC_DAILY = 102

    fun scheduleAll(context: Context) {
        scheduleClockAlarm(context)
        scheduleTempAlarm(context)
        scheduleDailyAlarm(context)
    }

    fun cancelAll(context: Context) {
        cancel(context, ACTION_CLOCK_TICK, RC_CLOCK)
        cancel(context, ACTION_TEMP_TICK,  RC_TEMP)
        cancel(context, ACTION_DAILY_TICK, RC_DAILY)
    }

    fun scheduleClockAlarm(context: Context) {
        val now        = System.currentTimeMillis()
        val nextMinute = now - (now % 60_000L) + 60_000L
        schedule(context, ACTION_CLOCK_TICK, RC_CLOCK, nextMinute)
    }

    fun scheduleTempAlarm(context: Context) {
        val interval    = 15 * 60_000L
        val now         = System.currentTimeMillis()
        val nextQuarter = now - (now % interval) + interval
        schedule(context, ACTION_TEMP_TICK, RC_TEMP, nextQuarter)
    }

    fun scheduleDailyAlarm(context: Context) {
        val now         = System.currentTimeMillis()
        val dayInterval = 24 * 60 * 60_000L
        val nextMidnight = now - (now % dayInterval) + dayInterval
        schedule(context, ACTION_DAILY_TICK, RC_DAILY, nextMidnight)
    }

    private fun schedule(context: Context, action: String, requestCode: Int, triggerAt: Long) {
        val alarmManager  = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPI(context, action, requestCode)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->   // API 33+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC, triggerAt, pendingIntent
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->          // API 23–32
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC, triggerAt, pendingIntent
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->     // API 19–22
                alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent)
            else ->                                                      // API <19
                alarmManager.setRepeating(
                    AlarmManager.RTC, triggerAt, 60_000L, pendingIntent
                )
        }
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPI(context, action, requestCode))
    }

    private fun buildPI(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            this.action = action
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}