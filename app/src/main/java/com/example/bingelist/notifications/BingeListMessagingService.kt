package com.example.bingelist.notifications

import com.example.bingelist.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BingeListMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        NotificationHelper.saveToken(token)
    }

    // Runs when the app is open, and for data-only messages in any state.
    override fun onMessageReceived(message: RemoteMessage) {
        if (!NotificationHelper.isEnabled(this)) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return

        NotificationHelper.show(this, title, body, message.data[NotificationHelper.EXTRA_IMDB_ID])
    }
}