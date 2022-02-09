package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.nineAmOnEveryWorkingDay
import io.pleo.antaeus.batch.worker.BatchInvoiceValidationWorker
import org.quartz.*

class InvoiceValidationJob(
    batchWorker: BatchInvoiceValidationWorker? = null
) : BaseJob(batchWorker) {

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
                    .cronSchedule(nineAmOnEveryWorkingDay)
            )
            .build()
    }

}
