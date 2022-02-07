package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.firstDayOfEachMonth
import org.quartz.*
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.TriggerKey.triggerKey

class PlaceholderJob : Job {
    private val jobGroup = this::class.java.packageName
    private val jobName = this::class.java.simpleName
    val detail : JobDetail
    val trigger : Trigger

    init {
        detail = newJob(PlaceholderJob::class.java)
            .withIdentity(jobName, jobGroup)
            .build()

        trigger = newTrigger()
            .withIdentity(triggerKey(jobName, jobGroup))
            .withSchedule(CronScheduleBuilder.cronSchedule(firstDayOfEachMonth))
            .build()
    }

    override fun execute(context: JobExecutionContext?) {
        TODO("Not yet implemented")
    }
}
