package com.ps168.idr

data class RawImuSample(
    val tNs: Long,
    val accX: Float, val accY: Float, val accZ: Float,
    val gyroX: Float, val gyroY: Float, val gyroZ: Float,
    val gravX: Float, val gravY: Float, val gravZ: Float
)