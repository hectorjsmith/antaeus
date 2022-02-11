package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoiceValidationService(
    private val invoiceService: InvoiceService,
    private val notificationService: NotificationService,
    private val customerService: CustomerService
) {
    /**
     * Validate the provided invoice and save the updated invoice back to the database.
     * This validation will check that the invoice references an existing customer and that the invoice amount and
     * corresponding customer use the same currency.
     * The invoice will be flagged as FAILED if any validations fail.
     * The notification service will be used to notify the system admin of any failures.
     *
     * @param invoice Invoice to validate
     * @return Updated invoice after validation
     */
    fun validateAndSaveInvoice(invoice: Invoice): Invoice {
        val validatedInvoice = validateInvoice(invoice)
        return invoiceService.update(validatedInvoice)
    }

    private fun validateInvoice(invoice: Invoice): Invoice {
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
