package com.ps168.idr

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class ImuRow(val tNs: Long, val sensor: String, val x: Float, val y: Float, val z: Float, val w: Float?)
data class GnssRow(val tNs: Long, val lat: Double, val lon: Double, val speed: Float?, val bearing: Float?, val accuracy: Float?)

class ReplayEngine(
    private val imuFile: File,
    private val gnssFile: File,
    private val onImu: (ImuRow) -> Unit,
    private val onGnss: (GnssRow) -> Unit,
    private val onFinished: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var imuRows: List<ImuRow> = emptyList()
    private var gnssRows: List<GnssRow> = emptyList()

    @Volatile var isRunning = false
        private set
    @Volatile var speedMultiplier: Float = 1.0f   // 1.0 = real time, 5.0 = 5x faster
    @Volatile var gnssBlackoutActive: Boolean = false  // toggle this to simulate a tunnel

    private var imuIndex = 0
    private var gnssIndex = 0
    private var startWallTimeMs = 0L
    private var startLogTimeNs = 0L

    fun load() {
        imuRows = BufferedReader(FileReader(imuFile)).useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val p = line.split(",")
                if (p.size < 5) return@mapNotNull null
                ImuRow(
                    tNs = p[0].toLongOrNull() ?: return@mapNotNull null,
                    sensor = p[1],
                    x = p[2].toFloatOrNull() ?: 0f,
                    y = p[3].toFloatOrNull() ?: 0f,
                    z = p[4].toFloatOrNull() ?: 0f,
                    w = p.getOrNull(5)?.toFloatOrNull()
                )
            }.toList()
        }
        gnssRows = BufferedReader(FileReader(gnssFile)).useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val p = line.split(",")
                if (p.size < 3) return@mapNotNull null
                GnssRow(
                    tNs = p[0].toLongOrNull() ?: return@mapNotNull null,
                    lat = p[2].toDoubleOrNull() ?: return@mapNotNull null,
                    lon = p[3].toDoubleOrNull() ?: return@mapNotNull null,
                    speed = p.getOrNull(4)?.toFloatOrNull(),
                    bearing = p.getOrNull(5)?.toFloatOrNull(),
                    accuracy = p.getOrNull(6)?.toFloatOrNull()
                )
            }.toList()
        }
    }

    fun start() {
        if (imuRows.isEmpty() && gnssRows.isEmpty()) load()
        if (imuRows.isEmpty()) return
        isRunning = true
        imuIndex = 0
        gnssIndex = 0
        startLogTimeNs = imuRows.first().tNs
        startWallTimeMs = System.currentTimeMillis()
        scheduleNext()
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleNext() {
        if (!isRunning) return
        val nextImuT = imuRows.getOrNull(imuIndex)?.tNs
        val nextGnssT = if (!gnssBlackoutActive) gnssRows.getOrNull(gnssIndex)?.tNs else null

        if (nextImuT == null && nextGnssT == null) {
            isRunning = false
            onFinished()
            return
        }

        val useImu = nextGnssT == null || (nextImuT != null && nextImuT <= nextGnssT)
        val eventLogT = if (useImu) nextImuT!! else nextGnssT!!
        val elapsedLogNs = eventLogT - startLogTimeNs
        val elapsedLogMs = elapsedLogNs / 1_000_000
        val targetWallMs = startWallTimeMs + (elapsedLogMs / speedMultiplier).toLong()
        val delay = (targetWallMs - System.currentTimeMillis()).coerceAtLeast(0)

        handler.postDelayed({
            if (!isRunning) return@postDelayed
            if (useImu) {
                imuRows.getOrNull(imuIndex)?.let { onImu(it) }
                imuIndex++
            } else {
                gnssRows.getOrNull(gnssIndex)?.let { onGnss(it) }
                gnssIndex++
            }
            scheduleNext()
        }, delay)
    }
}