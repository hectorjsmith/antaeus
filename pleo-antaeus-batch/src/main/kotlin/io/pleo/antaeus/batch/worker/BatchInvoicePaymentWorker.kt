package io.pleo.antaeus.batch.worker

import io.pleo.antaeus.core.calcDateTimeForStartOfMonth
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.joda.time.DateTime

class BatchInvoicePaymentWorker(
    private val invoiceService: InvoiceService,
    private val billingService: BillingService
): Worker {
    private val logger = KotlinLogging.logger("BatchInvoicePaymentWorker")

    override fun run() {
        val maxCreationTime = calcDateTimeForStartOfMonth(DateTime.now())

        val invoices = invoiceService.fetchAllByStatusAndMaxCreationTime(
            statusList = setOf(InvoiceStatus.PENDING, InvoiceStatus.READY),
            maxCreationTime = maxCreationTime
        )

        logger.info("Found ${invoices.size} invoices to process")
        invoices.forEach {
            try {
                billingService.processAndSaveInvoice(it)
            } catch(ex: Exception) {
                logger.error("Error paying invoice ${it.id}", ex)
            }
        }
    }
}
