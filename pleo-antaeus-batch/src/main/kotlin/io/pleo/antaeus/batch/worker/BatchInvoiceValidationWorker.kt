package io.pleo.antaeus.batch.worker

import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoiceValidationService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BatchInvoiceValidationWorker(
    private val invoiceService: InvoiceService,
    private val invoiceValidationService: InvoiceValidationService
): Worker {
    private val logger = KotlinLogging.logger("BatchInvoiceValidationWorker")

    override fun run() {
        val invoices = invoiceService.fetchAllByStatus(setOf(InvoiceStatus.PENDING))

        logger.info("Found ${invoices.size} invoices to pre-validate")
        invoices.forEach {
            try {
                invoiceValidationService.validateAndSaveInvoice(it)
            } catch(ex: Exception) {
                logger.warn("Error validating invoice ${it.id}", ex)
            }
        }
    }
}