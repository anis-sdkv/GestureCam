package com.example.gesturecam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class MainApp: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("OverLayChannel","OverLay service channel", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationChannel = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationChannel.createNotificationChannel(channel)
    }
}