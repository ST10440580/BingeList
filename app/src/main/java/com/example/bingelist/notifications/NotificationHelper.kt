package com.example.bingelist.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.bingelist.R
import com.example.bingelist.ui.discover.DiscoverActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object NotificationHelper {

    const val CHANNEL_ID = "new_releases"
    const val BANTER_CHANNEL_ID = "movie_banter"
    const val TOPIC = "new_releases"
    const val EXTRA_IMDB_ID = "imdbId" // same key you put in the console's "Custom data"
    const val ID_DIGEST = 1001
    const val ID_REMINDER = 1002

    private const val PREFS = "binge_list_prefs" // same file the Settings screen writes to
    private const val KEY_ENABLED = "notifications"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Movie updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "New releases and watchlist alerts" }
        )
        manager.createNotificationChannel(
            NotificationChannel(BANTER_CHANNEL_ID, "Movie banter", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Goofy movie-quote reminders" }
        )
    }

    // The Settings switch controls this: off = the device stops receiving topic pushes. */
    fun applyTopicSubscription(enabled: Boolean) {
        val messaging = FirebaseMessaging.getInstance()
        if (enabled) messaging.subscribeToTopic(TOPIC) else messaging.unsubscribeFromTopic(TOPIC)
    }

    fun saveCurrentToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { saveToken(it) }
    }

    // Stores this device's token under the signed-in user so a push can target one user. */
    fun saveToken(token: String) {
        Log.d("BingeListFCM", "FCM token: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("devices").document(token)
            .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
    }

    @SuppressLint("MissingPermission") // checked just below
    fun show(
        context: Context,
        title: String,
        body: String,
        imdbId: String?,
        target: Class<out Activity> = DiscoverActivity::class.java,
        notificationId: Int = imdbId?.hashCode() ?: System.currentTimeMillis().toInt(),
        channelId: String = CHANNEL_ID
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)

        val intent = Intent(context, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            imdbId?.let { putExtra(EXTRA_IMDB_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == BANTER_CHANNEL_ID) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}