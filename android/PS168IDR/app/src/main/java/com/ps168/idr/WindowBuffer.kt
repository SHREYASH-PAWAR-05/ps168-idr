package com.ps168.idr

import java.util.ArrayDeque

data class ImuSample(val tNs: Long, val east: Float, val north: Float, val up: Float,
                     val gyroX: Float, val gyroY: Float, val gyroZ: Float)

class WindowBuffer(
    private val windowSeconds: Double = 5.0,
    private val targetHz: Double = 10.0,
    private val strideSeconds: Double = 1.0   // emit a window once per this many seconds
) {
    private val buffer = ArrayDeque<ImuSample>()
    private val windowNs = (windowSeconds * 1_000_000_000L).toLong()
    private val strideNs = (strideSeconds * 1_000_000_000L).toLong()
    private var lastEmitNs: Long = -1

    fun addSample(sample: ImuSample, onWindowReady: (List<ImuSample>) -> Unit) {
        buffer.addLast(sample)
        while (buffer.isNotEmpty() && sample.tNs - buffer.first().tNs > windowNs) {
            buffer.removeFirst()
        }
        val expectedCount = (windowSeconds * targetHz).toInt()
        if (buffer.size >= expectedCount) {
            if (lastEmitNs == -1L || sample.tNs - lastEmitNs >= strideNs) {
                onWindowReady(buffer.toList())
                lastEmitNs = sample.tNs
            }
        }
    }
}