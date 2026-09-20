package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Single source of truth for business calendar date management and conversions.
 * Configured explicitly to the clinic's business timezone (Asia/Kolkata, UTC+05:30).
 */
object BusinessCalendarHelper {

    val BUSINESS_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    data class BusinessDate(
        val year: Int,
        val month: Int, // 0-based: 0 = January, 8 = September, 11 = December
        val dayOfMonth: Int
    ) : Comparable<BusinessDate> {
        override fun compareTo(other: BusinessDate): Int {
            if (year != other.year) return year.compareTo(other.year)
            if (month != other.month) return month.compareTo(other.month)
            return dayOfMonth.compareTo(other.dayOfMonth)
        }

        fun toKey(): Int = year * 10000 + (month + 1) * 100 + dayOfMonth
    }

    data class CalendarDayInfo(
        val businessDate: BusinessDate,
        val isCurrentMonth: Boolean,
        val isToday: Boolean
    )

    /**
     * Converts an epoch millisecond instant into a business calendar date.
     * Guaranteed to use the explicit business timezone.
     */
    fun instantToBusinessDate(millis: Long): BusinessDate {
        val cal = Calendar.getInstance(BUSINESS_TIME_ZONE).apply {
            timeInMillis = millis
        }
        return BusinessDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts a business calendar date into epoch millis for a default publishing time (e.g. 10:00 AM IST).
     */
    fun businessDateToInstant(date: BusinessDate, hourOfDay: Int = 10, minute: Int = 0): Long {
        val cal = Calendar.getInstance(BUSINESS_TIME_ZONE).apply {
            clear()
            set(date.year, date.month, date.dayOfMonth, hourOfDay, minute, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns today's date in the business timezone.
     */
    fun getTodayBusinessDate(): BusinessDate {
        val cal = Calendar.getInstance(BUSINESS_TIME_ZONE)
        return BusinessDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Creates a Calendar instance initialized to the given date in the business timezone.
     */
    fun createBusinessCalendar(date: BusinessDate? = null): Calendar {
        return Calendar.getInstance(BUSINESS_TIME_ZONE).apply {
            if (date != null) {
                clear()
                set(date.year, date.month, date.dayOfMonth, 12, 0, 0)
            }
        }
    }

    /**
     * Formats the schedule section heading consistently with the selected business date.
     * Example: "Thursday, September 17, 2026" or "Friday, September 18, 2026"
     */
    fun formatScheduleHeading(date: BusinessDate): String {
        val cal = Calendar.getInstance(BUSINESS_TIME_ZONE).apply {
            clear()
            set(date.year, date.month, date.dayOfMonth, 12, 0, 0)
        }
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH).apply {
            timeZone = BUSINESS_TIME_ZONE
        }
        return sdf.format(cal.time)
    }

    /**
     * Formats the month/year header for the calendar view in the business timezone.
     */
    fun formatMonthHeader(cal: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).apply {
            timeZone = BUSINESS_TIME_ZONE
        }
        return sdf.format(cal.time)
    }

    /**
     * Generates day cells for the week or month view, all resolved in the business timezone.
     */
    fun getCalendarDays(baseCal: Calendar, isWeekOnly: Boolean): List<CalendarDayInfo> {
        val today = getTodayBusinessDate()
        val cal = Calendar.getInstance(BUSINESS_TIME_ZONE).apply {
            clear()
            set(
                baseCal.get(Calendar.YEAR),
                baseCal.get(Calendar.MONTH),
                baseCal.get(Calendar.DAY_OF_MONTH),
                12, 0, 0
            )
        }

        if (isWeekOnly) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY))
            val result = mutableListOf<CalendarDayInfo>()
            for (i in 0 until 7) {
                val bDate = BusinessDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                result.add(
                    CalendarDayInfo(
                        businessDate = bDate,
                        isCurrentMonth = true,
                        isToday = bDate == today
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return result
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val currentMonth = cal.get(Calendar.MONTH)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            cal.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - Calendar.SUNDAY))

            val result = mutableListOf<CalendarDayInfo>()
            for (i in 0 until 35) {
                val bDate = BusinessDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                result.add(
                    CalendarDayInfo(
                        businessDate = bDate,
                        isCurrentMonth = bDate.month == currentMonth,
                        isToday = bDate == today
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return result
        }
    }
}
