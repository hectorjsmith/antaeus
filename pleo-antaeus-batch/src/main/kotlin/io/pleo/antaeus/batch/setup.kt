package io.pleo.antaeus.batch

import io.pleo.antaeus.batch.job.PlaceholderJob
import org.quartz.impl.StdSchedulerFactory

private val scheduler = StdSchedulerFactory.getDefaultScheduler()

fun startScheduler() {
    scheduler.clear()

    val job = PlaceholderJob()
    scheduler.scheduleJob(job.detail, job.trigger)

    scheduler.start()
}
