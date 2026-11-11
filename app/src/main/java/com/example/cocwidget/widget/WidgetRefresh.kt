package com.example.cocwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cocwidget.data.UpgradeRepository
import com.example.cocwidget.data.nowEpochSec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Two refresh paths:
 *  - WorkManager every 15 min (the platform minimum) keeps "Xh Ym" text roughly current.
 *  - An alarm at the next finish time flips that row to "ready" promptly.
 */
object WidgetRefresh {

    private const val PERIODIC_WORK = "widget-refresh"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    suspend fun refreshNow(context: Context) {
        UpgradesWidget().updateAll(context)
        scheduleNextFinishAlarm(context)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    private suspend fun scheduleNextFinishAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = nowEpochSec()
        val next = UpgradeRepository(context).current()
            ?.upgrades
            ?.firstOrNull { it.finishAtEpochSec > now }

        if (next == null) {
            alarmManager.cancel(alarmIntent(context))
            return
        }
        // Inexact (no SCHEDULE_EXACT_ALARM permission needed); may be a few minutes late in Doze.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC,
            next.finishAtEpochSec * 1000 + 1_000,
            alarmIntent(context),
        )
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetRefresh.refreshNow(applicationContext)
        return Result.success()
    }
}

/** Receives the finish-time alarm and BOOT_COMPLETED (alarms don't survive reboot). */
class RefreshReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                WidgetRefresh.refreshNow(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
