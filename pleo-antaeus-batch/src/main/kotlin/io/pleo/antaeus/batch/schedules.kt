package io.pleo.antaeus.batch

import org.quartz.CronExpression

val firstDayOfEachMonth = CronExpression("0 0 0 1 * ?")
