package com.rondinella.strhack.tracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rondinella.strhack.utils.convertLongToTime
import com.google.android.gms.location.*
import com.rondinella.strhack.R

class TrackerService : Service() {

    lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var notificationManager: NotificationManager

    private val NOTIFICATION_ID = 9083150
    val CHANNEL_ID = "CHANNEL_ID"
    val CHANNEL_ID_NAME = "Channel name"


    lateinit var track: GpxFileWriter

    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {

        try {
            track = GpxFileWriter(applicationContext)

            notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_ID_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.setSound(null, null)
                channel.setShowBadge(false)
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
                notificationManager.createNotificationChannel(channel)
            }
            var notification = createNotification(applicationContext, CHANNEL_ID, 0)
            if (notification == null) {
                notification = NotificationCompat.Builder(this, CHANNEL_ID)
            }
            startForeground(NOTIFICATION_ID, notification.build())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val locationRequest = LocationRequest.create()?.apply {
            interval = 1000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        track.line.outlinePaint.strokeCap = Paint.Cap.ROUND
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                val builder: NotificationCompat.Builder? = createNotification(applicationContext, CHANNEL_ID, 0)
                for (location in p0.locations) {
                    builder?.setContentText("${convertLongToTime(location.time)}\nLONG: ${location.longitude}\nLAT: ${location.latitude}")!!
                    notificationManager.notify(NOTIFICATION_ID, builder.build())

                    track.addPoint(location.longitude.toString(), location.latitude.toString(), location.time.toString(), location.altitude.toString(), null, null)
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (locationRequest != null) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        track.close()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)

    }


    private fun createNotification(
        context: Context,
        channelId: String,
        type: Int
    ): NotificationCompat.Builder? {
        try {
            return NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.strhack_is_recording))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setBadgeIconType(type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}
