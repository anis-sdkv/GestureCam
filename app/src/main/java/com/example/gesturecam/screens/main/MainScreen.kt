package com.example.gesturecam.screens.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import com.example.gesturecam.services.GesturesServices
import com.example.gesturecam.services.OverlayService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        if (cameraPermissionState.status.isGranted)
            MainContent()
        else if (cameraPermissionState.status.shouldShowRationale) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Please grant camera permission")
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(text = "Grant")
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No camera permission granted")
                Button(onClick = cameraPermissionState::launchPermissionRequest) {
                    Text(text = "Request")
                }
            }
        }
    }
}


@Composable
private fun MainContent() {
    val context = LocalContext.current
    Column {
        Button(onClick = {
            if (!checkPermission(context)) return@Button
            Intent(context, OverlayService::class.java).also {
                it.setAction(OverlayService.START_ACTION)
                context.startService(it)
            }
        }) {
            Text(text = "Start overlay")
        }
        Button(onClick = {
            Intent(context, OverlayService::class.java).also {
                it.setAction(OverlayService.STOP_ACTION)
                context.startService(it)
            }
        }) {
            Text(text = "Stop overlay")
        }
    }
}

private fun checkPermission(context: Context): Boolean {

    if (!Settings.canDrawOverlays(context)) {
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$context.packageName")
        ).also { context.startActivity(it) }
        return false
    }

    if (!isAccessServiceEnabled(context, GesturesServices::class.java)) {
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).also { context.startActivity(it) }
        return false
    }

    return true
}


private fun isAccessServiceEnabled(context: Context, accessibilityServiceClass: Class<*>): Boolean {
    val prefString = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )

    return prefString != null && prefString.contains(context.packageName + "/" + accessibilityServiceClass.name)
}
