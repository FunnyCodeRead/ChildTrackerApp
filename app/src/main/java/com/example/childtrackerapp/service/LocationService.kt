package com.example.childtrackerapp.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.R
import com.example.childtrackerapp.child.data.ChildRepository

import com.example.childtrackerapp.child.helper.GeoFenceHelper
import com.google.android.gms.location.*

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val repository = ChildRepository("child1")

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundServiceWithNotification()
        checkIfLocationEnabled()
        requestLocationUpdatesSafely()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Child Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Đang theo dõi vị trí")
            .setContentText("Ứng dụng đang chạy để đảm bảo an toàn cho trẻ.")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun checkIfLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!gpsEnabled) {
                val alertIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                alertIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(alertIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestLocationUpdatesSafely() {
        // Kiểm tra quyền trước khi yêu cầu vị trí
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Không có quyền, không làm gì cả
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            repository.sendLocation("child1", location)
            GeoFenceHelper.checkDangerZone(this@LocationService, location)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
