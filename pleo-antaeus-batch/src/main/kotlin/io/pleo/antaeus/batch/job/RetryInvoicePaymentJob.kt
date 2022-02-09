package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.everyHourOnTheHour
import io.pleo.antaeus.batch.worker.BatchRetryInvoicePaymentWorker
import org.quartz.*

class RetryInvoicePaymentJob(
    worker: BatchRetryInvoicePaymentWorker
) : BaseJob(worker) {

    override fun execute(context: JobExecutionContext?) {
        val worker = getWorkerFromContext(context)
        worker.run()
    }

    override fun buildJobDetail(): JobDetail {
        return JobBuilder.newJob(this::class.java)
            .withIdentity(jobName, jobGroup)
            .build()
    }

    override fun buildJobTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(jobName, jobGroup))
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule(everyHourOnTheHour)
            )
            .build()
    }
}