package com.ps168.idr


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ReplayScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Idle") }
    var imuCount by remember { mutableStateOf(0) }
    var gnssCount by remember { mutableStateOf(0) }
    var blackout by remember { mutableStateOf(false) }

    val engine = remember {
        val dir = File(context.getExternalFilesDir(null), "replay_logs/log_20260922_151652")
        ReplayEngine(
            imuFile = File(dir, "imu_log.csv"),
            gnssFile = File(dir, "gnss_log.csv"),
            onImu = { imuCount++ },
            onGnss = { gnssCount++ },
            onFinished = { status = "Finished" }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Replay Mode", style = MaterialTheme.typography.headlineSmall)
        Text("Status: $status")
        Text("Replayed IMU: $imuCount   GNSS: $gnssCount")
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Simulate blackout (tunnel): ")
            Switch(checked = blackout, onCheckedChange = {
                blackout = it
                engine.gnssBlackoutActive = it
            })
        }
        Button(onClick = {
            engine.speedMultiplier = 5f   // 5x speed so you don't wait real-time
            engine.start()
            status = "Playing (5x speed)"
        }) { Text("START REPLAY") }
        Button(onClick = {
            engine.stop()
            status = "Stopped"
        }) { Text("STOP REPLAY") }
    }
}