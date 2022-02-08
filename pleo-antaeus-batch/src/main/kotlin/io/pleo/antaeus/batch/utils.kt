package io.pleo.antaeus.batch

import org.joda.time.DateTime

fun calcDateTimeForStartOfMonth(now: DateTime): DateTime {
    return now
        .withTime(0, 0, 0, 0)
        .withDayOfMonth(1)
}
