package io.pleo.antaeus.batch.worker

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.joda.time.DateTime

class BatchRetryInvoicePaymentWorker(
    private val invoiceService: InvoiceService,
    private val billingService: BillingService
) : Worker {
    private val logger = KotlinLogging.logger("BatchInvoicePaymentWorker")

    override fun run() {
        val invoices = invoiceService.fetchAllByStatusAndRetryPaymentTime(
            statusList = setOf(InvoiceStatus.FAILED, InvoiceStatus.PROCESSING),
            maxRetryPaymentTime = DateTime.now()
        )

        logger.info("Found ${invoices.size} invoices to retry")
        invoices.forEach {
            if (it.status == InvoiceStatus.PROCESSING) {
                logger.error("Found invoice ${it.id} stuck in PROCESSING state - Needs manual review")
                invoiceService.update(it.copy(status = InvoiceStatus.FAILED, retryPaymentTime = null))
            } else {
                try {
                    billingService.processAndSaveInvoice(it)
                } catch(ex: Exception) {
                    logger.error("Error retrying invoice payment ${it.id}", ex)
                }
            }
        }
    }
}
