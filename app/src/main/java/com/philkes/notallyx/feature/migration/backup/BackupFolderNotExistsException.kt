package com.philkes.notallyx.feature.migration.backup

class BackupFolderNotExistsException(val path: String, cause: Throwable? = null) :
    IllegalArgumentException("Folder '$path' does not exist", cause)
