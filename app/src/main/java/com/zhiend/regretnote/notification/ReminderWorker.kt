package com.zhiend.regretnote.notification

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhiend.regretnote.data.UndoDatabase
import com.zhiend.regretnote.util.Dates
import java.util.Calendar

/**
 * Posts the nightly reminder. WorkManager's 24h periodic request may drift
 * because of Doze, so this worker checks the settings itself:
 *  - reminders must be enabled,
 *  - the current time must be at/after the chosen hour (or shortly past
 *    midnight when a delayed run spilled into the next day),
 *  - it only notifies once per day,
 *  - and it stays quiet if you already wrote something today. A nudge to write
 *    when the note is already there is how a reminder becomes an annoyance.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMINDER_ENABLED, false)) return Result.success()

        val today = Dates.today()
        if (prefs.getString(KEY_LAST_NOTIFIED, null) == today) return Result.success()

        val hour = prefs.getInt(KEY_REMINDER_HOUR, 20)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val withinWindow = currentHour >= hour || currentHour < 3
        if (!withinWindow) return Result.success()

        val writtenToday = UndoDatabase.get(appContext).entryDao().countForDate(today) > 0
        if (writtenToday) return Result.success()

        ReminderScheduler.ensureChannel(appContext)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, ReminderScheduler.notification(appContext))
        prefs.edit().putString(KEY_LAST_NOTIFIED, today).apply()
        return Result.success()
    }

    private companion object {
        const val PREFS_NAME = "undo_settings"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_LAST_NOTIFIED = "last_reminder_date"
        const val NOTIFICATION_ID = 1001
    }
}
