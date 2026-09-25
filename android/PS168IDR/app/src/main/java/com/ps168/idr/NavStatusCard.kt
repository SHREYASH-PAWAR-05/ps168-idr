package com.ps168.idr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NavStatusCard(
    mode: NavMode,
    driftMeters: Float?,      // null = not available yet (waiting on A's EKF)
    confidencePercent: Int?,  // null = not available yet
    gnssRows: Int,
    imuRows: Int
) {
    val modeColor = when (mode) {
        NavMode.GNSS_GOOD -> Color(0xFF2E7D32)   // green
        NavMode.DEGRADED -> Color(0xFFF9A825)    // amber
        NavMode.DENIED -> Color(0xFFC62828)      // red
        NavMode.RECOVERY -> Color(0xFF1565C0)    // blue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(modeColor.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("MODE: ${mode.name}", fontWeight = FontWeight.Bold, color = modeColor, style = MaterialTheme.typography.titleMedium)
        Text("Drift: ${driftMeters?.let { "%.1f m".format(it) } ?: "— (pending EKF)"}")
        Text("Confidence: ${confidencePercent?.let { "$it%" } ?: "— (pending EKF)"}")
        Text("Sensors: IMU=$imuRows  GNSS=$gnssRows")
    }
}