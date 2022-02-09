package io.pleo.antaeus.batch.worker

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import mu.KLogger

fun payInvoice(
    logger: KLogger,
    billingService: BillingService,
    invoiceService: InvoiceService,
    invoice: Invoice
) {
    try {
        val updatedInvoice = billingService.processInvoice(invoice)
        val savedInvoice = invoiceService.update(updatedInvoice)
        logger.info("Processed invoice id: ${savedInvoice.id}")
    } catch (ex: Exception) {
        logger.error("Exception found while processing invoice id: ${invoice.id}", ex)
    }
}
