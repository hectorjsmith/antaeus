package io.pleo.antaeus.batch

import io.pleo.antaeus.batch.job.BaseJob
import io.pleo.antaeus.batch.job.InvoicePaymentJob
import io.pleo.antaeus.batch.job.InvoiceValidationJob
import io.pleo.antaeus.batch.job.RetryInvoicePaymentJob
import io.pleo.antaeus.batch.worker.BatchInvoicePaymentWorker
import io.pleo.antaeus.batch.worker.BatchInvoiceValidationWorker
import io.pleo.antaeus.batch.worker.BatchRetryInvoicePaymentWorker
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoiceValidationService
import mu.KotlinLogging
import org.quartz.impl.StdSchedulerFactory

private val logger = KotlinLogging.logger("BatchJobSetup")
private val scheduler = StdSchedulerFactory.getDefaultScheduler()

fun startScheduler(
    invoiceService: InvoiceService,
    billingService: BillingService,
    invoiceValidationService: InvoiceValidationService
) {
    scheduler.clear()

    // Setup job to pay invoices
    val batchInvoicePaymentWorker = BatchInvoicePaymentWorker(
        invoiceService = invoiceService,
        billingService = billingService
    )
    val invoicePaymentJob = InvoicePaymentJob(batchInvoicePaymentWorker)
    scheduleJob(invoicePaymentJob)

    // Setup job to retry failed invoices
    val batchRetryInvoicePaymentWorker = BatchRetryInvoicePaymentWorker(
        invoiceService = invoiceService,
        billingService = billingService
    )
    val retryInvoicePaymentJob = RetryInvoicePaymentJob(batchRetryInvoicePaymentWorker)
    scheduleJob(retryInvoicePaymentJob)

    // Setup job to pre-validate invoices
    val batchInvoiceValidationWorker = BatchInvoiceValidationWorker(
        invoiceService = invoiceService,
        invoiceValidationService = invoiceValidationService
    )
    val invoiceValidationJob = InvoiceValidationJob(batchInvoiceValidationWorker)
    scheduleJob(invoiceValidationJob)

    scheduler.start()
}

private fun scheduleJob(job: BaseJob) {
    scheduler.scheduleJob(job.detail, job.trigger)
    logger.info("Scheduled ${job.detail.key.name}. Next execution scheduled for ${job.trigger.nextFireTime}")
}
