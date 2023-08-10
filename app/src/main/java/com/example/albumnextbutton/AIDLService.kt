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
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.messengerserverapplication.IIPCExample
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.os.Process
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AIDLService: Service() {
    private var infoUs = RetrofitInstance.getRetrofitInstance().create(AlbumService::class.java)

    private val sharedViewModel: SharedLiveData
        get() = (application as Helper).sharedViewModel

    private val sharedButtonListener: ButtonLiveData
        get() = (application as Helper).sharedButtonListener

    private val serverprop: ServerLiveData
        get() = (application as Helper).serverprop

    var iRemoteService: IIPCExample? = null



    val apiHandler = Handler()
    private val apiRunnable = object : Runnable {
        override fun run() {
            // Make the API call and update MutableLiveData with the response data
            GlobalScope.launch{
                Log.d("AIDL", "Fetching data from API")

                sharedViewModel.sharedMutableData.postValue(infoUs.getFact().body())

                    Log.d("AIDL", "sending apiRunnable")

                    iRemoteService?.postVal(
                        applicationContext.packageName,
                        Process.myPid(),
                        sharedViewModel.sharedMutableData.value?.data.toString())
                    val serverClass = Serverprop(iRemoteService?.pid.toString(), iRemoteService?.connectionCount.toString())
                    serverprop.serverData.postValue(serverClass)

            }
            // Make the call every minute
            apiHandler.postDelayed(this, 10000)
        }
    }

    private val connection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            iRemoteService = IIPCExample.Stub.asInterface(service)
            if(sharedButtonListener.sharedButton.value!!) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formatted = current.format(formatter)
                iRemoteService?.postVal(
                    applicationContext.packageName,
                    Process.myPid(),
                    sharedViewModel.sharedMutableData.value?.data.toString())
                val serverClass = Serverprop(iRemoteService?.pid.toString(), iRemoteService?.connectionCount.toString())
                serverprop.serverData.postValue(serverClass)
            }


        }

        override fun onServiceDisconnected(name: ComponentName?) {
            iRemoteService = null
        }
    }



    // apiHandler.removeCallbacks(apiRunnable)

    private val dataObserver = Observer<Boolean> {
       connectToRemoteService()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        apiHandler.post(apiRunnable)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("AIDL", "Task Removed, Restarting Service")
        apiHandler.removeCallbacks(apiRunnable)
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    fun connectToRemoteService() {
        Log.d("AIDL", "Connected")
        val intent = Intent("aidlexample")
        val pack = IIPCExample::class.java.`package`
        pack?.let {
            intent.setPackage(pack.name)
            bindService(
                intent, connection, Context.BIND_AUTO_CREATE
            )
        }

    }

    fun disconnectToRemoteService() {
        try {
            unbindService(connection)
        }catch (e : Exception){
            Log.w("AIDLError", e.toString())
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
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
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }


        val notification = createNotification()
        startForeground(3, notification)

        super.onCreate()
    }
    private fun createNotification(): Notification {
        val notificationTitle = "AIDL Service"
        val notificationText = "Service is running in the background"
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                // Add the extra value for fragment identification
                putExtra("FRAGMENT_ID", R.id.frag3)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        apiHandler.removeCallbacks(apiRunnable)
        sharedButtonListener.sharedButton.removeObserver(dataObserver)
        disconnectToRemoteService()
        // Stop the foreground service and remove the notification
        stopForeground(true)
        super.onDestroy()
    }

    // Method to send data to the server application
    @RequiresApi(Build.VERSION_CODES.O)
    fun sendDataToServer(
        packageName: String?,
        pid: Int,
        currData: catfact
    ) {
        Log.d("AIDL", "Sending message")
        if (iRemoteService != null) {
            Log.d("AIDL", "Bound and will send message")
            // Call the server application's method using the binder
            try {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formatted = current.format(formatter)

                iRemoteService?.postVal(
                    packageName,
                    pid,
                   currData.toString(),
                )
            }catch (e : Exception){
                Log.w("ATAKNN", e.stackTraceToString())
            }
        }
    }
}