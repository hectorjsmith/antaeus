package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.worker.Worker
import org.quartz.Job
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.Trigger

abstract class BaseJob(worker: Worker? = null) : Job {
    val detail : JobDetail by lazy { buildJobDetail() }
    val trigger : Trigger by lazy { buildJobTrigger() }
    protected val jobGroup = this::class.java.packageName
    protected val jobName = this::class.java.simpleName
    private val contextId = "${jobGroup}_${jobName}_ctx"

    init {
        storeWorkerInContext(worker)
    }

    protected abstract fun buildJobDetail(): JobDetail
    protected abstract fun buildJobTrigger(): Trigger

    protected fun getWorkerFromContext(context: JobExecutionContext?): Worker {
        if (context == null) {
            throw NullPointerException("JobExecution context is null")
        }
        val rawWorker = context.jobDetail.jobDataMap[contextId]
        if (rawWorker is Worker) {
            return rawWorker
        }
        throw IllegalArgumentException("Worker not found in the job context - cannot continue")
    }

    private fun storeWorkerInContext(worker: Worker?) {
        if (worker != null) {
            detail.jobDataMap[contextId] = worker
        }
    }
}
