package com.example.myapplication.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.utils.ErrorLogger

@Composable
fun ErrorDialog(
    errorDetail: ErrorLogger.ErrorDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚠️ Terjadi Kesalahan",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = buildString {
                    appendLine(errorDetail.message)
                    appendLine()
                    appendLine("Waktu: ${errorDetail.timestamp}")
                    if (errorDetail.context.isNotEmpty()) {
                        appendLine()
                        appendLine("Detail:")
                        errorDetail.context.forEach { (key, value) ->
                            appendLine("• $key: $value")
                        }
                    }
                    appendLine()
                    appendLine("Error telah disimpan di:")
                    appendLine("Download/NxDrama/Error.txt")
                },
                fontFamily = FontFamily.Monospace
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}