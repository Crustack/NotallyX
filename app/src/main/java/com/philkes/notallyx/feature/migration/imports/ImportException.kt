package com.philkes.notallyx.feature.migration.imports

class ImportException(val textResId: Int, cause: Throwable) : RuntimeException(cause)
