package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogger {
    private const val TAG = "FreeReelsError"
    private var errorListener: ((ErrorDetail) -> Unit)? = null

    data class ErrorDetail(
        val timestamp: String,
        val exception: String,
        val message: String,
        val stackTrace: String,
        val thread: String,
        val deviceInfo: DeviceInfo
    )

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkVersion: Int,
        val appVersion: String,
        val appVersionCode: Long
    )

    fun init(context: Context, listener: ((ErrorDetail) -> Unit)? = null) {
        errorListener = listener
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val errorDetail = captureError(throwable, context)
            Log.e(TAG, "Uncaught exception: ${errorDetail.exception}", throwable)
            errorListener?.invoke(errorDetail)
            
            // Call original handler
            defaultUncaughtExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    fun captureError(throwable: Throwable, context: Context): ErrorDetail {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return ErrorDetail(
            timestamp = dateFormat.format(Date()),
            exception = throwable.javaClass.simpleName,
            message = throwable.message ?: "Tidak ada pesan error",
            stackTrace = stackTrace,
            thread = Thread.currentThread().name,
            deviceInfo = getDeviceInfo(context)
        )
    }

    private fun getDeviceInfo(context: Context): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            sdkVersion = android.os.Build.VERSION.SDK_INT,
            appVersion = packageInfo.versionName ?: "unknown",
            appVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        )
    }
}
