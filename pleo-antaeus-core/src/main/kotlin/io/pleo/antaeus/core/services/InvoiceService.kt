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
    fun isInvoiceDue(invoice: Invoice, now: DateTime = DateTime.now()): Boolean {
        val startOfCurrentMonth = calcDateTimeForStartOfMonth(now)
        return invoice.creationTime.isBefore(startOfCurrentMonth.toInstant())
    }

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetchAllByStatus(statusList: Set<InvoiceStatus>): List<Invoice> {
        return dal.fetchInvoices(statusList = statusList)
    }

    fun fetchAllByStatusAndRetryPaymentTime(
        statusList: Set<InvoiceStatus>,
        maxRetryPaymentTime: DateTime
    ): List<Invoice> {
        return dal.fetchInvoices(statusList = statusList, maxRetryPaymentTime = maxRetryPaymentTime)
    }

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
