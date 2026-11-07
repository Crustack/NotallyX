package com.philkes.notallyx.buildlogic.tasks

import java.io.File

data class ParsedScreenshot(
    val file: File,
    val baseName: String,
    val locale: String,
)

object ScreenshotParser {
    private val screenshotRegex =
        Regex("""^(.+)_([a-zA-Z]{2,3}(?:[-_][a-zA-Z0-9]{2,4})?)(?:_[a-fA-F0-9]+_\d+)?\.png$""")

    fun parse(sourceDir: File): List<ParsedScreenshot> {
        if (!sourceDir.exists()) return emptyList()

        return sourceDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "png" }
            .mapNotNull { file ->
                val match = screenshotRegex.matchEntire(file.name)
                if (match != null) {
                    ParsedScreenshot(
                        file = file,
                        baseName = match.groupValues[1],
                        locale = match.groupValues[2],
                    )
                } else {
                    println("Skipped file not matching pattern: ${file.name}")
                    null
                }
            }
            .toList()
    }
}
