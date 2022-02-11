package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.firstDayOfEachMonth
import io.pleo.antaeus.batch.worker.BatchInvoicePaymentWorker
import org.quartz.*

class InvoicePaymentJob(
    // This needs a no-args constructor for Quartz
    batchWorker: BatchInvoicePaymentWorker? = null
) : BaseJob(batchWorker) {

    override fun execute(context: JobExecutionContext?) {
        withLock {
            val worker = getWorkerFromContext(context)
            worker.run()
        }
    }

    override fun buildJobDetail(): JobDetail {
        return JobBuilder.newJob(this::class.java)
            .withIdentity(jobName, jobGroup)
            .requestRecovery()
            .build()
    }

    override fun buildJobTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(jobName, jobGroup))
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule(firstDayOfEachMonth)
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .build()
    }
}
