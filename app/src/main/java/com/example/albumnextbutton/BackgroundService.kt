package com.example.albumnextbutton

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.logging.Logger.global

class BackgroundService: Service() {
    val apihandler = Handler()
    private var catapi: AlbumService= RetrofitInstance.getRetrofitInstance().create(AlbumService::class.java)

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            GlobalScope.launch {
                var result = catapi.getFact()
                val name:String = result.body()?.data.toString()
                val intent = Intent()
                println(name)

                intent.action = "com.example.myapplication"
                intent.putExtra(ContactsContract.Directory.PACKAGE_NAME, packageName)
                intent.putExtra(ContactsContract.Intents.Insert.DATA, name)
                intent.component = ComponentName(
                    "com.example.messengerserverapplication",
                    "com.example.messengerserverapplication.IPCBroadcastReceiver"
                )
                sendBroadcast(intent)
            }
            apihandler.postDelayed(this,5000)
        }

    }

    override fun onCreate() {
        super.onCreate()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Foreground Service Channel"
            val descriptionText = "Foreground service channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ForegroundServiceChannel", name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification for the foreground service
        val notification: Notification = createNotification()

        // Start the service as a foreground service with the notification
        startForeground(2, notification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("hello")

        apihandler.post(broadcastRunnable)
        return START_STICKY

    }

    private fun createNotification(): Notification {
        val notificationTitle = "Broadcast Service"
        val notificationText = "Service is running in the background"
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                // Add the extra value for fragment identification
                putExtra("FRAGMENT_ID", R.id.frag1)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


}