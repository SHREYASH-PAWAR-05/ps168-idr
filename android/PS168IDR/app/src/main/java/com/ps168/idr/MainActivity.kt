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
import android.content.Intent
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Day 1: screen must stay on
        setContent { MaterialTheme { LoggerScreen() } }
//        setContent { MaterialTheme { ReplayScreen() } }
    }
}

@Composable
fun LoggerScreen() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(LoggingService.isServiceRunning) }
    var imuRows by remember { mutableStateOf(0L) }
    var gnssRows by remember { mutableStateOf(0L) }
    var message by remember { mutableStateOf(if (running) "Logging (service running)..." else "Ready. Press START.") }

    fun startService() {
        val intent = Intent(context, LoggingService::class.java).apply { action = LoggingService.ACTION_START }
        ContextCompat.startForegroundService(context, intent)
        running = true
        message = "Logging in background service...\nYou can lock the screen now."
    }

    fun stopService() {
        val intent = Intent(context, LoggingService::class.java).apply { action = LoggingService.ACTION_STOP }
        context.startService(intent)
        running = false
        message = "Stopped. IMU=$imuRows, GNSS=$gnssRows rows."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) startService()
        else message = "Location permission denied. Cannot log GNSS."
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed either way — notification is nice-to-have, not required for logging */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    LaunchedEffect(running) {
        while (running) {
            imuRows = LoggingService.currentLogger?.imuCount?.get() ?: 0L
            gnssRows = LoggingService.currentLogger?.gnssCount?.get() ?: 0L
            delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PS168 Sensor Logger", style = MaterialTheme.typography.headlineSmall)
        Text("IMU rows: $imuRows")
        Text("GNSS rows: $gnssRows")
        Button(onClick = {
            if (running) {
                stopService()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) startService()
                else permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }) { Text(if (running) "STOP" else "START") }
        Text(message)
    }
}