package com.philkes.notallyx.core.ui.platform

enum class AppPermission {
    POST_NOTIFICATIONS,
    SCHEDULE_EXACT_ALARM,
    RECORD_AUDIO,
}

interface PermissionChecker {
    fun isGranted(permission: AppPermission): Boolean
}

class AlwaysGrantedPermissionChecker : PermissionChecker {
    override fun isGranted(permission: AppPermission): Boolean = true
}
