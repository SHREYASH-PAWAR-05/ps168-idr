package com.ps168.idr

import java.util.ArrayDeque

class RawWindowBuffer(
    private val windowSize: Int = 50,       // exact sample count per window (matches scaler.json)
    private val strideSeconds: Double = 1.0
) {
    private val buffer = ArrayDeque<RawImuSample>()
    private val maxBufferSize = windowSize * 3  // keep some slack so we never run short
    private val strideNs = (strideSeconds * 1_000_000_000L).toLong()
    private var lastEmitNs: Long = -1

    val currentSize: Int get() = buffer.size

    fun addSample(sample: RawImuSample, onWindowReady: (List<RawImuSample>) -> Unit) {
        buffer.addLast(sample)
        while (buffer.size > maxBufferSize) {
            buffer.removeFirst()
        }
        if (buffer.size >= windowSize) {
            if (lastEmitNs == -1L || sample.tNs - lastEmitNs >= strideNs) {
                onWindowReady(buffer.toList().takeLast(windowSize))
                lastEmitNs = sample.tNs
            }
        }
    }
}