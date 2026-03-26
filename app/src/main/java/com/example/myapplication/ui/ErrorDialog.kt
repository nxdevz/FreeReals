package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.myapplication.utils.ErrorLogger

@Composable
fun ErrorDialog(
    errorDetail: ErrorLogger.ErrorDetail,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "⚠️ Aplikasi Mengalami Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorDetail.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Error summary
                Text(
                    text = "Error: ${errorDetail.exception}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorDetail.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Device info section
                Text(
                    text = "📱 Informasi Perangkat",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                DeviceInfoCard(errorDetail.deviceInfo)
                
                // Stack trace section
                Text(
                    text = "🔍 Stack Trace",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                StackTraceView(stackTrace = errorDetail.stackTrace)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val errorText = buildString {
                        appendLine("=== FREE REELS ERROR REPORT ===")
                        appendLine("Timestamp: ${errorDetail.timestamp}")
                        appendLine("Exception: ${errorDetail.exception}")
                        appendLine("Message: ${errorDetail.message}")
                        appendLine()
                        appendLine("--- Device Info ---")
                        appendLine("Manufacturer: ${errorDetail.deviceInfo.manufacturer}")
                        appendLine("Model: ${errorDetail.deviceInfo.model}")
                        appendLine("Android Version: ${errorDetail.deviceInfo.androidVersion} (SDK ${errorDetail.deviceInfo.sdkVersion})")
                        appendLine("App Version: ${errorDetail.deviceInfo.appVersion} (${errorDetail.deviceInfo.appVersionCode})")
                        appendLine()
                        appendLine("--- Stack Trace ---")
                        appendLine(errorDetail.stackTrace)
                    }
                    clipboardManager.setText(AnnotatedString(errorText))
                }
            ) {
                Text("📋 Salin Laporan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun DeviceInfoCard(deviceInfo: ErrorLogger.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Device: ${deviceInfo.manufacturer} ${deviceInfo.model}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Android: ${deviceInfo.androidVersion} (API ${deviceInfo.sdkVersion})",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "App Version: ${deviceInfo.appVersion} (${deviceInfo.appVersionCode})",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StackTraceView(stackTrace: String) {
    val lines = stackTrace.split("\n").filter { it.isNotBlank() }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(lines) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
