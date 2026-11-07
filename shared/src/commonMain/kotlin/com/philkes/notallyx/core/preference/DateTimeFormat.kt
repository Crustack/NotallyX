package com.philkes.notallyx.core.preference

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable
import org.ocpsoft.prettytime.PrettyTime

class ThreadLocalDateFormat(private val format: String, private val locale: Locale) {
    private val threadLocal =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat(format, locale)
            }
        }

    fun format(date: Date): String = threadLocal.get()!!.format(date)

    fun toPattern(): String = threadLocal.get()!!.toPattern()
}

class ThreadLocalDateInstance(private val style: Int) {
    private val threadLocal =
        object : ThreadLocal<java.text.DateFormat>() {
            override fun initialValue(): java.text.DateFormat {
                return java.text.DateFormat.getDateInstance(style)
            }
        }

    fun format(date: Date): String = threadLocal.get()!!.format(date)
}

private val ISO_DATE_FORMAT = ThreadLocalDateFormat("yyyy-MM-dd", Locale.US)
private val MM_DD_YY_FORMAT = ThreadLocalDateFormat("MM/dd/yy", Locale.US)
private val DD_MM_YY_FORMAT = ThreadLocalDateFormat("dd/MM/yy", Locale.UK)
private val DD_MM_YY_FORMAT_GER = ThreadLocalDateFormat("dd.MM.yy", Locale.GERMANY)
private val FULL_FORMAT = ThreadLocalDateInstance(java.text.DateFormat.FULL)

@Serializable
enum class DateFormat(val format: (Date) -> String, val textHint: String = "") {
    NONE({ "" }),
    FULL(FULL_FORMAT::format),
    RELATIVE({ PrettyTime().format(it) }),
    DD_MM_YY_GER(DD_MM_YY_FORMAT_GER::format, " (${DD_MM_YY_FORMAT_GER.toPattern()})"),
    DD_MM_YY(DD_MM_YY_FORMAT::format, " (${DD_MM_YY_FORMAT.toPattern()})"),
    MM_DD_YY(MM_DD_YY_FORMAT::format, " (${MM_DD_YY_FORMAT.toPattern()})"),
    SHORT_ISO(ISO_DATE_FORMAT::format, " (${ISO_DATE_FORMAT.toPattern()})"),
}

private val TWENTY_FOUR_H_FORMAT = ThreadLocalDateFormat("HH:mm", Locale.GERMANY)
private val AM_PM_FORMAT = ThreadLocalDateFormat("hh:mm a", Locale.US)

@Serializable
enum class TimeFormat(val format: (Date) -> String) {
    NONE({ "" }),
    TWENTY_FOUR_H(TWENTY_FOUR_H_FORMAT::format),
    AM_PM(AM_PM_FORMAT::format),
}
