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
        val startOfMonth = calcDateTimeForStartOfMonth(DateTime.now())

        // Find all invoices ready for payment created before the start of the month (i.e. that are due for payment)
        val invoices = invoiceService.fetchAllByStatusAndMaxCreationTime(
            statusList = setOf(InvoiceStatus.PENDING, InvoiceStatus.READY),
            maxCreationTime = startOfMonth
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
