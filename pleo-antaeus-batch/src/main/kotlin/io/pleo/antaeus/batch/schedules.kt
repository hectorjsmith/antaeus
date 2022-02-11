package io.pleo.antaeus.batch

import org.quartz.CronExpression

// Runs at 00:00:00 on the first day of the month
val firstDayOfEachMonth = CronExpression("0 0 0 1 * ?")

// Runs at 09:00 on every workday
val nineAmOnEveryWorkingDay = CronExpression("0 0 9 ? * MON-FRI")

// Runs at the top of the hour, every single hour
val everyHourOnTheHour = CronExpression("0 0 0/1 * * ?")
