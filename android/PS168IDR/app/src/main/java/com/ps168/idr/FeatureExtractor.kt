package com.ps168.idr

import android.content.Context
import org.json.JSONObject

object FeatureExtractor {
    private var mean: DoubleArray? = null
    private var std: DoubleArray? = null

    fun loadScaler(context: Context) {
        val json = context.assets.open("scaler.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val meanArr = obj.getJSONArray("mean")
        val stdArr = obj.getJSONArray("std")
        mean = DoubleArray(meanArr.length()) { meanArr.getDouble(it) }
        std = DoubleArray(stdArr.length()) { stdArr.getDouble(it) }
    }

    // Feature order MUST match scaler.json: acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,gravity_x,gravity_y,gravity_z
    fun toNormalizedArray(window: List<RawImuSample>): FloatArray {
        val m = mean ?: error("Call FeatureExtractor.loadScaler(context) first")
        val s = std ?: error("Call FeatureExtractor.loadScaler(context) first")
        val result = FloatArray(window.size * 9)
        window.forEachIndexed { i, sample ->
            val raw = doubleArrayOf(
                sample.accX.toDouble(), sample.accY.toDouble(), sample.accZ.toDouble(),
                sample.gyroX.toDouble(), sample.gyroY.toDouble(), sample.gyroZ.toDouble(),
                sample.gravX.toDouble(), sample.gravY.toDouble(), sample.gravZ.toDouble()
            )
            for (f in 0 until 9) {
                result[i * 9 + f] = ((raw[f] - m[f]) / s[f]).toFloat()
            }
        }
        return result
    }
}