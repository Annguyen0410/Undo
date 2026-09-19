package com.zhiend.regretnote

import android.app.Application
import com.zhiend.regretnote.data.DraftStore
import com.zhiend.regretnote.data.SettingsStore
import com.zhiend.regretnote.data.UndoDatabase
import com.zhiend.regretnote.data.UndoRepository
import com.zhiend.regretnote.notification.ReminderScheduler
import com.zhiend.regretnote.purchase.RevenueCatManager

class UndoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RevenueCatManager.configure(this)
        ReminderScheduler.ensureChannel(this)
        val settings = container.repository.settings.value
        if (settings.reminderEnabled) {
            ReminderScheduler.schedule(this, settings.reminderHour, settings.reminderMinute)
        }
    }
}

/** Manual DI container — small enough that no framework is needed. */
class AppContainer(application: Application) {
    val database: UndoDatabase = UndoDatabase.get(application)
    private val settingsStore = SettingsStore(application)
    val repository: UndoRepository = UndoRepository(database.entryDao(), settingsStore)

    /** The note being written right now, if any — survives the process dying. */
    val draftStore: DraftStore = DraftStore(application)
}
