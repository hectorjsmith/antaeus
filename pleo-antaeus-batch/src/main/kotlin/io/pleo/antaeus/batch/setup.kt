package io.pleo.antaeus.batch

import io.pleo.antaeus.batch.job.InvoicePaymentJob
import io.pleo.antaeus.batch.job.InvoiceValidationJob
import io.pleo.antaeus.batch.worker.BatchInvoicePaymentWorker
import io.pleo.antaeus.batch.worker.BatchInvoiceValidationWorker
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoiceValidationService
import org.quartz.impl.StdSchedulerFactory

private val scheduler = StdSchedulerFactory.getDefaultScheduler()

fun startScheduler(
    invoiceService: InvoiceService,
    billingService: BillingService,
    invoiceValidationService: InvoiceValidationService
) {
    scheduler.clear()

    val batchInvoicePaymentWorker = BatchInvoicePaymentWorker(invoiceService, billingService)
    val invoicePaymentJob = InvoicePaymentJob(batchInvoicePaymentWorker)
    scheduler.scheduleJob(invoicePaymentJob.detail, invoicePaymentJob.trigger)

    val batchInvoiceValidationWorker = BatchInvoiceValidationWorker(
        invoiceService,
        invoiceValidationService
    )
    val invoiceValidationJob = InvoiceValidationJob(batchInvoiceValidationWorker)
    scheduler.scheduleJob(invoiceValidationJob.detail, invoiceValidationJob.trigger)

    scheduler.start()
}
