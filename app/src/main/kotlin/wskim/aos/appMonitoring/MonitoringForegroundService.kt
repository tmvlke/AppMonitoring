package wskim.aos.appMonitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class MonitoringForegroundService : Service() {

    private var isRunningYn = false

    private val binder: IBinder = LocalBinder()
    var data: MutableStateFlow<String?> = MutableStateFlow(null)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    inner class LocalBinder : Binder() {
        val service: MonitoringForegroundService
            get() = this@MonitoringForegroundService
    }

    private fun setData(newData: String) {
        data.value = newData
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunningYn) {
//            stopForeground(startId)
//            stopSelf()
//        } else {
            isRunningYn = true
            generateForegroundNotification()

            coroutineScope.launch {
                while (true) {
                    val packageName = AppFinder.getPackageName(this@MonitoringForegroundService)
                    Log.d("getPackageName", packageName)
                    setData(packageName)
                    delay(1000L)
                }
            }
        }

        return START_STICKY
    }

    //Notififcation for ON-going
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    private var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    private fun generateForegroundNotification() {
        val intentMainLanding = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intentMainLanding, PendingIntent.FLAG_IMMUTABLE)
        iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        if (mNotificationManager == null) {
            mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        assert(mNotificationManager != null)
        mNotificationManager?.createNotificationChannelGroup(
            NotificationChannelGroup("chats_group", "Chats")
        )
        val notificationChannel =
            NotificationChannel("service_channel", "Service Notifications",
                NotificationManager.IMPORTANCE_MIN)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager?.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "service_channel")

        builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" service is running").toString())
            .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
            .setContentText("Touch to open") //                    , swipe down for more options.
            .setSmallIcon(R.drawable.ic_alaram)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (iconNotification != null) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
        }
        builder.color = resources.getColor(R.color.purple_200)
        notification = builder.build()

        startForeground(mNotificationId, notification)
    }
}