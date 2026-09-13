package com.example.bingelist

import android.app.Application
import com.google.firebase.FirebaseApp

class BingeListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}