/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.calcDateTimeForStartOfMonth
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime

class InvoiceService(private val dal: AntaeusDal) {
    /**
     * Checks whether the provided invoice is due for payment or not.
     * Due for payment is defined as having a creation time before the start of the current month (that is why this
     * function takes a date parameter - to know when "now" is)
     *
     * @param invoice Invoice to check
     * @param now DateTime that represents when "now" is. This is used to get the current month and validate that the
     * invoice's creation time is before the start of this month.
     * @return true if the invoice is due, false otherwise
     */
    fun isInvoiceDue(invoice: Invoice, now: DateTime = DateTime.now()): Boolean {
        val startOfCurrentMonth = calcDateTimeForStartOfMonth(now)
        return invoice.creationTime.isBefore(startOfCurrentMonth.toInstant())
    }

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    /**
     * Fetch all invoices that have a status in the provided set
     */
    fun fetchAllByStatus(statusList: Set<InvoiceStatus>): List<Invoice> {
        return dal.fetchInvoices(statusList = statusList)
    }

    /**
     * Fetch all invoices where the status is in the provided set, and the "nextRetryPayment" field is non-null and
     * before the provided date.
     */
    fun fetchAllByStatusAndRetryPaymentTime(
        statusList: Set<InvoiceStatus>,
        maxRetryPaymentTime: DateTime
    ): List<Invoice> {
        return dal.fetchInvoices(statusList = statusList, maxRetryPaymentTime = maxRetryPaymentTime)
    }

    /**
     * Fetch all invoices where the status is in the provided set, and the "creationTime" field is non-null and
     * before the provided date.
     */
    fun fetchAllByStatusAndMaxCreationTime(
        statusList: Set<InvoiceStatus>,
        maxCreationTime: DateTime
    ): List<Invoice> {
        return dal.fetchInvoices(statusList = statusList, maxCreationTime = maxCreationTime)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(invoice: Invoice): Invoice {
        return dal.updateInvoice(invoice) ?: throw InvoiceNotFoundException(invoice.id)
    }
}
