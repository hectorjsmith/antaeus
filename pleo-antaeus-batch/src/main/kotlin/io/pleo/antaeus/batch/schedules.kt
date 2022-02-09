package io.pleo.antaeus.batch

import org.quartz.CronExpression

val firstDayOfEachMonth = CronExpression("0 0 0 1 * ?")
val nineAmOnEveryWorkingDay = CronExpression("0 0 9 ? * MON-FRI")
val everyHourOnTheHour = CronExpression("0 0 0/1 * * ?")
