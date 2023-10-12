package com.example.albumnextbutton

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.R as RESS


class MessengerService : Service(){

    private val sharedViewModel: SharedLiveData
        get() = (application as Helper).sharedViewModel

    private val sharedButtonListener: ButtonLiveData
        get() = (application as Helper).sharedButtonListener

    private val serverprop: ServerLiveData
        get() = (application as Helper).serverprop

    private var serverMessenger: Messenger? = null
    private var clientMessenger: Messenger? = null
    private var infoUs = RetrofitInstance.getRetrofitInstance().create(AlbumService::class.java)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serverMessenger = Messenger(service)
            clientMessenger = Messenger(handler)
            sendMessageToServer()
           
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverMessenger = null
            clientMessenger = null
        }
    }

    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val serverClass = Serverprop(bundle.getInt(PID).toString(), bundle.getInt(
                CONNECTION_COUNT).toString())
            serverprop.serverData.postValue(serverClass)
        }
    }

    val apiHandler = Handler()
    private val apiRunnable = object : Runnable {
        override fun run() {
            GlobalScope.launch{
                sharedViewModel.sharedMutableData.postValue(infoUs.getFact().body())
                    sendMessageToServer()

            }
            apiHandler.postDelayed(this, 10000)
        }
    }

    private val dataObserver = Observer<Boolean> {
        if(it){
            doBindService()
         
        }
        else{
            doUnbindService()
            
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        sharedButtonListener.sharedButton.observeForever(dataObserver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Foreground Service Channel"
            val descriptionText = "Foreground service channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ForegroundServiceChannel", name, importance).apply {
                description = descriptionText
            }

            println("here")
            doBindService()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = createNotification() // Implement the createNotification() method
        startForeground(1, notification)

        super.onCreate()
    }
    private fun createNotification(): Notification {
    
        val notificationTitle = "Messenger Service"
        val notificationText = "Service is running in the background"
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                // Add the extra value for fragment identification
                putExtra("FRAGMENT_ID", R.id.messengerFrag)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(RESS.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .build()
    }


    override fun onDestroy() {
        apiHandler.removeCallbacks(apiRunnable)
        sharedButtonListener.sharedButton.removeObserver(dataObserver)
        doUnbindService()
        stopForeground(true)
        super.onDestroy()
    }

    private fun doBindService(){
        println("hello22")
        Intent("messengerexample").also { intent ->
            intent.`package` = "com.example.messengerserverapplication"
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        println("hello22")

    }

    private fun doUnbindService(){
        if(sharedButtonListener.sharedButton.value == null){
            try {
                unbindService(serviceConnection)

            }catch (e : Exception)
            {
            }
        }
        if(sharedButtonListener.sharedButton.value!!){
            return
        }
        unbindService(serviceConnection)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        apiHandler.post(apiRunnable)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        apiHandler.removeCallbacks(apiRunnable)
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessageToServer(){
        println("send messassa")
        val message = Message.obtain(handler)
        val bundle = Bundle()
        val currData = sharedViewModel.sharedMutableData.value?.data

        println(currData)
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formatted = current.format(formatter)
        bundle.putString(PACKAGE_NAME, applicationContext.packageName)
        bundle.putString(DATA, currData.toString())
        bundle.putString(ZAMAN,formatted.toString())
        message.data = bundle
        message.replyTo = clientMessenger 
        serverMessenger?.send(message)
         message.recycle()

    }

}
