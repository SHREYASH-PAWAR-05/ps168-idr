package com.ps168.idr

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class SensorLogger(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val sensorNames = linkedMapOf(
        Sensor.TYPE_ACCELEROMETER to "acc",
        Sensor.TYPE_GYROSCOPE to "gyro",
        Sensor.TYPE_MAGNETIC_FIELD to "mag",
        Sensor.TYPE_GRAVITY to "grav",
        Sensor.TYPE_ROTATION_VECTOR to "rotvec"
    )

    private var thread: HandlerThread? = null
    private var imuWriter: BufferedWriter? = null
    private var gnssWriter: BufferedWriter? = null

    val imuCount = AtomicLong(0)
    val gnssCount = AtomicLong(0)
    @Volatile var isRunning = false
    var notes: String = ""

    @SuppressLint("MissingPermission")
    fun start(): File {
        check(!isRunning) { "Already running" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(context.getExternalFilesDir(null), "log_$stamp")
        dir.mkdirs()

        imuWriter = BufferedWriter(FileWriter(File(dir, "imu_log.csv")), 65536).also {
            it.write("t_ns,sensor,x,y,z,w\n")
        }
        gnssWriter = BufferedWriter(FileWriter(File(dir, "gnss_log.csv")), 65536).also {
            it.write("t_ns,utc_ms,lat,lon,speed_mps,bearing_deg,accuracy_m\n")
        }
        imuCount.set(0)
        gnssCount.set(0)

        // All callbacks run on ONE background thread -> UI thread stays free
        val t = HandlerThread("sensor-logger").also { it.start() }
        thread = t
        val handler = Handler(t.looper)

        val missing = mutableListOf<String>()
        for ((type, label) in sensorNames) {
            val s = sensorManager.getDefaultSensor(type)
            if (s == null) missing.add(label)
            else sensorManager.registerListener(this, s, 10_000, handler) // 10,000 us = 100 Hz
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this, t.looper)
        } else {
            missing.add("GPS is switched off")
        }
        notes = if (missing.isEmpty()) "All sensors OK" else "Missing: " + missing.joinToString()
        isRunning = true
        return dir
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        thread?.quitSafely()
        thread?.join(2000)
        thread = null
        imuWriter?.close(); imuWriter = null
        gnssWriter?.close(); gnssWriter = null
    }

    override fun onSensorChanged(e: SensorEvent) {
        val name = sensorNames[e.sensor.type] ?: return
        val v = e.values
        val w = if (e.sensor.type == Sensor.TYPE_ROTATION_VECTOR && v.size > 3) v[3].toString() else ""
        imuWriter?.append("${e.timestamp},$name,${v[0]},${v[1]},${v[2]},$w\n")
        imuCount.incrementAndGet()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(l: Location) {
        val speed = if (l.hasSpeed()) l.speed.toString() else ""
        val bearing = if (l.hasBearing()) l.bearing.toString() else ""
        val acc = if (l.hasAccuracy()) l.accuracy.toString() else ""
        gnssWriter?.append("${l.elapsedRealtimeNanos},${l.time},${l.latitude},${l.longitude},$speed,$bearing,$acc\n")
        gnssCount.incrementAndGet()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}