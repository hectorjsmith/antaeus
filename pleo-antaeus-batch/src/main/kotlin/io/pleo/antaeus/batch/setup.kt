package io.pleo.antaeus.batch

import io.pleo.antaeus.batch.job.InvoicePaymentJob
import io.pleo.antaeus.batch.worker.BatchInvoicePaymentWorker
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import org.quartz.impl.StdSchedulerFactory

private val scheduler = StdSchedulerFactory.getDefaultScheduler()

fun startScheduler(
    invoiceService: InvoiceService,
    billingService: BillingService
) {
    scheduler.clear()

    val batchInvoicePaymentWorker = BatchInvoicePaymentWorker(invoiceService, billingService)
    val invoicePaymentJob = InvoicePaymentJob(batchInvoicePaymentWorker)
    scheduler.scheduleJob(invoicePaymentJob.detail, invoicePaymentJob.trigger)

    scheduler.start()
}
