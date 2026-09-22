package com.ps168.idr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Day 1: screen must stay on
        setContent { MaterialTheme { LoggerScreen() } }
    }
}

@Composable
fun LoggerScreen() {
    val context = LocalContext.current
    val logger = remember { SensorLogger(context) }
    var running by remember { mutableStateOf(false) }
    var imuRows by remember { mutableStateOf(0L) }
    var gnssRows by remember { mutableStateOf(0L) }
    var message by remember { mutableStateOf("Ready. Press START.") }

    fun startLogging() {
        try {
            val dir = logger.start()
            running = true
            message = "Logging...\n${logger.notes}\n\nFolder:\n${dir.absolutePath}"
        } catch (e: Exception) {
            message = "Error: ${e.message}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) startLogging()
        else message = "Location permission denied. Cannot log GNSS."
    }

    LaunchedEffect(running) {
        while (running) {
            imuRows = logger.imuCount.get()
            gnssRows = logger.gnssCount.get()
            delay(500)
        }
    }

    DisposableEffect(Unit) { onDispose { logger.stop() } }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PS168 Sensor Logger", style = MaterialTheme.typography.headlineSmall)
        Text("IMU rows: $imuRows")
        Text("GNSS rows: $gnssRows")
        Button(onClick = {
            if (running) {
                logger.stop()
                running = false
                message = "Saved. IMU=$imuRows, GNSS=$gnssRows rows.\n\n" + message
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) startLogging()
                else permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }) { Text(if (running) "STOP" else "START") }
        Text(message)
    }
}