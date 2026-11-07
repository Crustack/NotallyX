package com.philkes.notallyx.data.model

enum class Color {
    DEFAULT,
    CORAL,
    ORANGE,
    SAND,
    STORM,
    FOG,
    SAGE,
    MINT,
    DUSK,
    FLOWER,
    BLOSSOM,
    CLAY;

    companion object {
        fun allColorStrings() = entries.map { it.toColorString() }.toList()

        fun valueOfOrDefault(value: String) =
            try {
                Color.valueOf(value)
            } catch (e: Exception) {
                DEFAULT
            }
    }
}

fun Color.toColorString() =
    when (this) {
        Color.DEFAULT -> BaseNote.COLOR_DEFAULT
        Color.CORAL -> "#FAAFA9"
        Color.ORANGE -> "#FFCC80"
        Color.SAND -> "#FFF8B9"
        Color.STORM -> "#AFCCDC"
        Color.FOG -> "#D3E4EC"
        Color.SAGE -> "#B4DED4"
        Color.MINT -> "#E2F6D3"
        Color.DUSK -> "#D3BFDB"
        Color.FLOWER -> "#F8BBD0"
        Color.BLOSSOM -> "#F5E2DC"
        Color.CLAY -> "#E9E3D3"
    }

private val HEX_COLOR_REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
private val NAMED_COLORS =
    setOf(
        "red",
        "blue",
        "green",
        "black",
        "white",
        "gray",
        "cyan",
        "magenta",
        "yellow",
        "lightgray",
        "darkgray",
        "grey",
        "lightgrey",
        "darkgrey",
        "aqua",
        "fuchsia",
        "lime",
        "maroon",
        "navy",
        "olive",
        "purple",
        "silver",
        "teal",
    )

fun String.isValidColorString(): Boolean =
    isNotEmpty() && (HEX_COLOR_REGEX.matches(this) || lowercase() in NAMED_COLORS)

fun String.parseToColorString() =
    if (isValidColorString()) {
        this
    } else {
        try {
            val colorEnum = Color.valueOf(this)
            colorEnum.toColorString()
        } catch (e: Exception) {
            BaseNote.COLOR_DEFAULT
        }
    }
