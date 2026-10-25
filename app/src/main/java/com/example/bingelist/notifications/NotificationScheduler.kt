package com.example.bingelist.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private val online = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // Called once at app start. KEEP means re-launching the app doesn't reset the schedule.
    fun scheduleAll(context: Context) {
        val work = WorkManager.getInstance(context)

        work.enqueueUniquePeriodicWork(
            "release_alerts", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReleaseAlertWorker>(1, TimeUnit.DAYS)
                .setConstraints(online).build()
        )
        work.enqueueUniquePeriodicWork(
            "weekly_digest", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS)
                .setConstraints(online).build()
        )
    }

    // Runs both checks immediately: used when the Settings switch is turned on
    fun runNow(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueue(OneTimeWorkRequestBuilder<WeeklyDigestWorker>().setConstraints(online).build())
        work.enqueue(OneTimeWorkRequestBuilder<ReleaseAlertWorker>().setConstraints(online).build())
    }
}