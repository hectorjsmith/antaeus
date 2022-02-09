package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime

class BillingService(
    private val notificationService: NotificationService,
    private val paymentProvider: PaymentProvider
) {
    fun processInvoice(invoice: Invoice): Invoice {
        if (invoice.status == InvoiceStatus.PAID) {
            throw InvoiceAlreadyPaidException(invoice.id)
        }
        try {
            val paymentResult = paymentProvider.charge(invoice)
            if (!paymentResult) {
                return handleInsufficientFunds(invoice)
            }
            return handleInvoicePaid(invoice)
        } catch(ex: CurrencyMismatchException) {
            return handleCurrencyMismatch(invoice, ex)
        } catch(ex: CustomerNotFoundException) {
            return handleCustomerNotFound(invoice, ex)
        } catch(ex: NetworkException) {
            return handleNetworkException(invoice, ex)
        } catch(ex: Exception) {
            return handleOtherException(invoice, ex)
        }
    }

    private fun handleInvoicePaid(invoice: Invoice): Invoice {
        notificationService.notifyAccountOwner(
            invoice.customerId,
            invoice.id,
            "Invoice of ${invoice.amount.value} ${invoice.amount.currency} paid successfully"
        )
        if (invoice.status == InvoiceStatus.FAILED) {
            notificationService.notifyAdministrator(
                invoice.id,
                "Errors resolved and invoice paid"
            )
        }
        return invoice.copy(status = InvoiceStatus.PAID, retryPaymentTime = null)
    }

    private fun handleInsufficientFunds(invoice: Invoice): Invoice {
        notificationService.notifyAccountOwner(
            invoice.customerId,
            invoice.id,
            "Insufficient funds to pay invoice. Amount due: ${invoice.amount.value} ${invoice.amount.currency}"
        )
        notificationService.notifyAdministrator(
            invoice.id,
            "Invoice not paid due to insufficient funds"
        )
        return invoice.copy(status = InvoiceStatus.FAILED, retryPaymentTime = DateTime.now().plusDays(1))
    }

    private fun handleCurrencyMismatch(invoice: Invoice, ex: CurrencyMismatchException): Invoice {
        notificationService.notifyAdministrator(
            invoice.id,
            "Invoice not paid due to a currency mismatch: ${ex.message}"
        )
        return invoice.copy(status = InvoiceStatus.FAILED, retryPaymentTime = null)
    }

    private fun handleCustomerNotFound(invoice: Invoice, ex: CustomerNotFoundException): Invoice {
        notificationService.notifyAdministrator(
            invoice.id,
            "Invoice not paid due to a missing account: ${ex.message}"
        )
        return invoice.copy(status = InvoiceStatus.FAILED, retryPaymentTime = null)
    }

    private fun handleNetworkException(invoice: Invoice, ex: NetworkException): Invoice {
        notificationService.notifyAdministrator(
            invoice.id,
            "Network error while trying to pay invoice: ${ex.message}"
        )
        return invoice.copy(status = InvoiceStatus.FAILED, retryPaymentTime = DateTime.now().plusHours(1))
    }

    private fun handleOtherException(invoice: Invoice, ex: Exception): Invoice {
        notificationService.notifyAdministrator(
            invoice.id,
            "Unknown error while processing invoice: ${ex.message}"
        )
        return invoice.copy(status = InvoiceStatus.FAILED, retryPaymentTime = null)
    }
}
