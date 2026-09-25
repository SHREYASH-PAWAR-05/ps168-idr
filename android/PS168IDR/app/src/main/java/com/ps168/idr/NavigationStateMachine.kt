package com.ps168.idr

enum class NavMode { GNSS_GOOD, DEGRADED, DENIED, RECOVERY }

class NavigationStateMachine(
    private val degradedAccuracyM: Float = 20f,
    private val deniedTimeoutMs: Long = 10_000L,
    private val recoveryDurationMs: Long = 5_000L
) {
    var currentMode: NavMode = NavMode.GNSS_GOOD
        private set

    private var lastGnssTimeMs: Long = System.currentTimeMillis()
    private var lastGnssAccuracy: Float = 0f
    private var recoveryStartMs: Long = -1L

    // Call this whenever a GNSS fix arrives (live sensor or replay)
    fun onGnssFix(accuracyM: Float, nowMs: Long = System.currentTimeMillis()) {
        lastGnssTimeMs = nowMs
        lastGnssAccuracy = accuracyM
        currentMode = when {
            currentMode == NavMode.DENIED -> {
                recoveryStartMs = nowMs
                NavMode.RECOVERY
            }
            currentMode == NavMode.RECOVERY -> NavMode.RECOVERY
            accuracyM > degradedAccuracyM -> NavMode.DEGRADED
            else -> NavMode.GNSS_GOOD
        }
    }

    // Call this regularly (e.g. every 500ms) even when no new GNSS fix arrives
    fun tick(nowMs: Long = System.currentTimeMillis()) {
        val gapMs = nowMs - lastGnssTimeMs
        when (currentMode) {
            NavMode.RECOVERY -> {
                if (nowMs - recoveryStartMs >= recoveryDurationMs) {
                    currentMode = if (lastGnssAccuracy > degradedAccuracyM) NavMode.DEGRADED else NavMode.GNSS_GOOD
                }
            }
            else -> {
                if (gapMs >= deniedTimeoutMs) currentMode = NavMode.DENIED
            }
        }
    }
}