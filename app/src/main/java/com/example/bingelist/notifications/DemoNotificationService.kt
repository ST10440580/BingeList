package com.example.bingelist.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.bingelist.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class DemoNotificationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        NotificationHelper.createChannel(this)
        ServiceCompat.startForeground(
            this, FOREGROUND_ID, buildOngoingNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        scope.launch {
            val end = System.currentTimeMillis() + MAX_RUNTIME_MS
            var index = 0
            while (isActive && System.currentTimeMillis() < end) {
                val banter = MovieBanter.ALL[index % MovieBanter.ALL.size]
                NotificationHelper.show(
                    applicationContext, banter.title, banter.body, banter.imdbId,
                    channelId = NotificationHelper.BANTER_CHANNEL_ID
                )
                index++
                delay(INTERVAL_MS)
            }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    private fun buildOngoingNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(STATUS_CHANNEL_ID, "Demo mode", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, DemoNotificationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Movie Banter running")
            .setContentText("Movie banter every ${INTERVAL_MS / 1000} seconds")
            .setOngoing(true)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        const val INTERVAL_MS = 30_000L          // use 60_000L for one per minute
        const val MAX_RUNTIME_MS = 10 * 60_000L  // auto-stops after 10 minutes
        private const val FOREGROUND_ID = 2001
        private const val STATUS_CHANNEL_ID = "demo_status"
        private const val ACTION_STOP = "com.example.bingelist.STOP_DEMO"

        @Volatile
        var isRunning = false
    }
}