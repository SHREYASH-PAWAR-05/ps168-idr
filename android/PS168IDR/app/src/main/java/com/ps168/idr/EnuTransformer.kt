package com.ps168.idr

import android.hardware.SensorManager

object EnuTransformer {
    // Converts device-frame accel (x,y,z) + rotation vector (from TYPE_ROTATION_VECTOR)
    // into East-North-Up frame, matching Person A's convention.
    fun deviceToEnu(rotVec: FloatArray, deviceAccel: FloatArray): FloatArray {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotVec)

        val enu = FloatArray(3)
        enu[0] = rotationMatrix[0] * deviceAccel[0] + rotationMatrix[1] * deviceAccel[1] + rotationMatrix[2] * deviceAccel[2] // East
        enu[1] = rotationMatrix[3] * deviceAccel[0] + rotationMatrix[4] * deviceAccel[1] + rotationMatrix[5] * deviceAccel[2] // North
        enu[2] = rotationMatrix[6] * deviceAccel[0] + rotationMatrix[7] * deviceAccel[1] + rotationMatrix[8] * deviceAccel[2] // Up
        return enu // [East, North, Up] in m/s²
    }
}