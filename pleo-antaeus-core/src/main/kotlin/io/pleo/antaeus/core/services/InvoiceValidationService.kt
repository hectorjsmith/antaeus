package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoiceValidationService(
    private val notificationService: NotificationService,
    private val customerService: CustomerService
) {
    fun validateAndSaveInvoice(invoice: Invoice, invoiceService: InvoiceService): Invoice {
        val validatedInvoice = validateInvoice(invoice)
        return invoiceService.update(validatedInvoice)
    }

    fun validateInvoice(invoice: Invoice): Invoice {
        if (invoice.status == InvoiceStatus.PAID) {
            throw InvoiceAlreadyPaidException(invoice.id)
        }
        try {
            val customer = customerService.fetch(invoice.customerId)
            if (customer.currency != invoice.amount.currency) {
                notificationService.notifyAdministrator(
                    invoice.id,
                    "Invoice currency (${invoice.amount.currency}) does not match account currency (${customer.currency})"
                )
                return invoice.copy(status = InvoiceStatus.FAILED)
            }
            return invoice.copy(status = InvoiceStatus.READY)
        } catch(ex: CustomerNotFoundException) {
            notificationService.notifyAdministrator(
                invoice.id,
                "Invoice associated with account that was not found: ${invoice.customerId}"
            )
        } catch(ex: Exception) {
            notificationService.notifyAdministrator(
                invoice.id,
                "Invoice pre-validation failed for unknown reason: ${ex.message}"
            )
        }
        return invoice.copy(status = InvoiceStatus.FAILED)
    }
}
