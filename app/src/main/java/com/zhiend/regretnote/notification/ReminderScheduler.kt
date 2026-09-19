package com.zhiend.regretnote.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zhiend.regretnote.MainActivity
import com.zhiend.regretnote.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the nightly check-in reminder with WorkManager. A periodic 24h
 * request is aligned to the chosen time; the worker itself decides whether to
 * actually notify (it may be delayed by Doze).
 */
object ReminderScheduler {

    const val CHANNEL_ID = "checkin_reminders"
    private const val WORK_NAME = "nightly_checkin_reminder"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Evening check-in",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminds you to write down one regret before bed."
        }
        manager.createNotificationChannel(channel)
    }

    /** (Re)schedules the periodic work so the next run lands at [hour]:[minute]. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        ensureChannel(context)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Notification tap → open the check-in screen. */
    fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun notification(context: Context): android.app.Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Undo — time for your check-in")
            .setContentText("What do you wish you'd done differently today?")
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
}
