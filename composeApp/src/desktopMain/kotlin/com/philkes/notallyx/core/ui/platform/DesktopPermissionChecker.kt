package com.philkes.notallyx.core.ui.platform

class DesktopPermissionChecker : PermissionChecker {
    override fun isGranted(permission: AppPermission): Boolean = true
}
