package com.example.bingelist

import android.app.Application
import com.example.bingelist.notifications.NotificationHelper
import com.example.bingelist.notifications.NotificationScheduler
import com.google.firebase.FirebaseApp

class BingeListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        NotificationHelper.createChannel(this)
        NotificationHelper.applyTopicSubscription(NotificationHelper.isEnabled(this))
        NotificationScheduler.scheduleAll(this)
    }
}