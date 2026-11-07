package com.philkes.notallyx.feature.migration.imports

data class ImportResult(val inserted: Int, val duplicates: Int, val corruptedNotes: Int)
