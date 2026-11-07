package com.philkes.notallyx.core.ui.platform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

class AndroidPermissionChecker(private val context: Context) : PermissionChecker {
    override fun isGranted(permission: AppPermission): Boolean =
        when (permission) {
            AppPermission.POST_NOTIFICATIONS ->
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
            AppPermission.SCHEDULE_EXACT_ALARM ->
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
            AppPermission.RECORD_AUDIO ->
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
        }
}
