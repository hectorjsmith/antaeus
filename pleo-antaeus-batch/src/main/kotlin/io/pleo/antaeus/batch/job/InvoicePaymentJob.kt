package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.firstDayOfEachMonth
import io.pleo.antaeus.batch.worker.BatchInvoicePaymentWorker
import org.quartz.*

class InvoicePaymentJob(batchWorker: BatchInvoicePaymentWorker? = null) : Job {
    private val jobGroup = this::class.java.packageName
    private val jobName = this::class.java.simpleName
    private val contextId = "${jobGroup}_${jobName}_ctx"
    val detail : JobDetail
    val trigger : Trigger

    init {
        detail = JobBuilder.newJob(this::class.java)
            .withIdentity(jobName, jobGroup)
            .requestRecovery()
            .build()

        trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(jobName, jobGroup))
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule(firstDayOfEachMonth)
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .build()

        storeWorkerInContext(batchWorker)
    }

    override fun execute(context: JobExecutionContext?) {
        val worker = getWorkerFromContext(context)
        worker.run()
    }

    private fun getWorkerFromContext(context: JobExecutionContext?): BatchInvoicePaymentWorker {
        if (context == null) {
            throw NullPointerException("JobExecution context is null")
        }
        val rawWorker = context.jobDetail.jobDataMap[contextId]
        if (rawWorker is BatchInvoicePaymentWorker) {
            return rawWorker
        }
        throw IllegalArgumentException("Worker not found in the job context - cannot continue")
    }

    private fun storeWorkerInContext(billingService: BatchInvoicePaymentWorker?) {
        if (billingService != null) {
            detail.jobDataMap[contextId] = billingService
        }
    }
}
