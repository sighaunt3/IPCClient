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

    // Messenger on the client
    private var clientMessenger: Messenger? = null

    private var infoUs = RetrofitInstance.getRetrofitInstance().create(AlbumService::class.java)

    // Service Connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Called when the connection to the server service is established
            serverMessenger = Messenger(service)
            clientMessenger = Messenger(handler)
            sendMessageToServer()
            Log.d("Messenger", "Send Message")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Called when the connection to the server service is disconnected
            serverMessenger = null
            clientMessenger = null
        }
    }

    // Handle messages from the remote service (server app)
    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            // Update UI with remote process info

            val bundle = msg.data
            val serverClass = Serverprop(bundle.getInt(PID).toString(), bundle.getInt(
                CONNECTION_COUNT).toString())

            serverprop.serverData.postValue(serverClass)
        }
    }

    val apiHandler = Handler()
    private val apiRunnable = object : Runnable {
        override fun run() {
            // Make the API call and update MutableLiveData with the response data
            GlobalScope.launch{
                Log.d("Messenger", "Fetching data from API")
                sharedViewModel.sharedMutableData.postValue(infoUs.getFact().body())
                    Log.d("Messenger", "Sending data to Server App")
                    sendMessageToServer()

            }
            // Make the call every minute
            apiHandler.postDelayed(this, 10000)
        }
    }

    // apiHandler.removeCallbacks(apiRunnable)

    private val dataObserver = Observer<Boolean> {
        if(it){
            try {
                doBindService()
            }
            catch (e : Exception){
                Log.w("MessengerError", e.toString())
                val LaunchIntent = packageManager.getLaunchIntentForPackage("com.example.messengerserverapplication")
                runBlocking {
                    startActivity(LaunchIntent)
                    delay(2000)
                }
                Log.w("Messenger", "Trying to wake up Server App")
                try {
                    doBindService()
                }catch (e : Exception){
                    Log.w("MessengerError", e.toString())
                }
            }
        }
        else{
            try {
                doUnbindService()
            }catch (e : Exception){
                // First Start
            }

        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        // ?
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

            // Register the channel with the system
            println("here")
            doBindService()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Start the service as a foreground service with a notification
        val notification = createNotification() // Implement the createNotification() method
        startForeground(1, notification)

        super.onCreate()
    }
    private fun createNotification(): Notification {
        // Create and return the notification for the foreground service
        // You can customize the notification as needed
        // For example:
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
        // Stop the foreground service and remove the notification
        stopForeground(true)
        super.onDestroy()
    }

    // Start service according to the button
    private fun doBindService(){
        println("hello22")
        Log.d("atakan", "ALREADY connected")


        // Start the server service
        Intent("messengerexample").also { intent ->
            intent.`package` = "com.example.messengerserverapplication"
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        println("hello22")

        Log.d("Messenger", "SUCCESSFULLY connected")
    }

    // Stop service activity according to the button activity
    private fun doUnbindService(){
        if(sharedButtonListener.sharedButton.value == null){
            try {
                unbindService(serviceConnection)
                Log.d("Messenger", "SUCCESSFULLY disconnected")

            }catch (e : Exception)
            {
                Log.w("Messenger", e.toString())
            }
        }
        if(sharedButtonListener.sharedButton.value!!){
            Log.d("Messenger", "ALREADY disconnected")
            return
        }
        try {
            unbindService(serviceConnection)
            Log.d("Messenger", "SUCCESSFULLY disconnected")

        }catch (e : Exception)
        {
            Log.w("Messenger", e.toString())
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Start the periodic API calls and send data if the service is bounded
        apiHandler.post(apiRunnable)

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.d("atakan", "Task Removed, Restarting Service")
        apiHandler.removeCallbacks(apiRunnable)
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessageToServer(){
        if (serverMessenger == null) {
            // Server service connection is lost or not available
            Log.w("Messenger Exception", "Server service connection lost. Cannot send message.")
            return
        }
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
        message.replyTo = clientMessenger //for communication to be two-way

        try {
            serverMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
            message.recycle()
        }
    }

}