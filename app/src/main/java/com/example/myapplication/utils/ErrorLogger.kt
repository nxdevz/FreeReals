package com.example.myapplication.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogger {
    private const val TAG = "NxDramaError"
    private var appContext: Context? = null
    private var onErrorCallback: ((ErrorDetail) -> Unit)? = null
    
    data class ErrorDetail(
        val message: String,
        val timestamp: String,
        val stackTrace: String? = null,
        val context: Map<String, String> = emptyMap()
    )
    
    fun init(context: Context, onError: (ErrorDetail) -> Unit) {
        appContext = context.applicationContext
        onErrorCallback = onError
    }
    
    fun logException(throwable: Throwable, additionalContext: String = "") {
        val errorDetail = ErrorDetail(
            message = throwable.message ?: "Unknown error",
            timestamp = getCurrentTimestamp(),
            stackTrace = throwable.stackTraceToString(),
            context = mapOf(
                "type" to throwable.javaClass.simpleName,
                "additional_context" to additionalContext
            )
        )
        
        // Log to Android logcat
        Log.e(TAG, "Exception occurred: ${errorDetail.message}", throwable)
        
        // Save to file
        saveErrorToFile(errorDetail)
        
        // Show dialog via callback
        onErrorCallback?.invoke(errorDetail)
    }
    
    fun logVideoError(errorMessage: String, videoUrl: String, dramaTitle: String, dramaId: String) {
        val errorDetail = ErrorDetail(
            message = errorMessage,
            timestamp = getCurrentTimestamp(),
            stackTrace = null,
            context = mapOf(
                "type" to "VIDEO_PLAYBACK_ERROR",
                "video_url" to videoUrl,
                "drama_title" to dramaTitle,
                "drama_id" to dramaId
            )
        )
        
        // Log to Android logcat
        Log.e(TAG, "Video playback error: $errorMessage | Drama: $dramaTitle | URL: $videoUrl")
        
        // Save to file
        saveErrorToFile(errorDetail)
        
        // Show dialog via callback
        onErrorCallback?.invoke(errorDetail)
    }
    
    fun logError(message: String, additionalContext: Map<String, String> = emptyMap()) {
        val errorDetail = ErrorDetail(
            message = message,
            timestamp = getCurrentTimestamp(),
            stackTrace = null,
            context = additionalContext
        )
        
        // Log to Android logcat
        Log.e(TAG, message)
        
        // Save to file
        saveErrorToFile(errorDetail)
        
        // Show dialog via callback
        onErrorCallback?.invoke(errorDetail)
    }
    
    private fun saveErrorToFile(errorDetail: ErrorDetail) {
        try {
            val context = appContext ?: return
            
            // Create directory if it doesn't exist
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val nxDramaDir = File(downloadDir, "NxDrama")
            if (!nxDramaDir.exists()) {
                nxDramaDir.mkdirs()
            }
            
            val errorFile = File(nxDramaDir, "Error.txt")
            
            // Prepare error log entry
            val logEntry = buildString {
                appendLine("=" .repeat(80))
                appendLine("Timestamp: ${errorDetail.timestamp}")
                appendLine("Message: ${errorDetail.message}")
                
                if (errorDetail.context.isNotEmpty()) {
                    appendLine("Context:")
                    errorDetail.context.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
                }
                
                errorDetail.stackTrace?.let {
                    appendLine("Stack Trace:")
                    appendLine(it)
                }
                
                appendLine("=" .repeat(80))
                appendLine()
            }
            
            // Append to file
            FileOutputStream(errorFile, true).use { outputStream ->
                outputStream.write(logEntry.toByteArray())
            }
            
            Log.d(TAG, "Error saved to: ${errorFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save error to file", e)
        }
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
    
    private fun Throwable.stackTraceToString(): String {
        return Log.getStackTraceString(this)
    }
}