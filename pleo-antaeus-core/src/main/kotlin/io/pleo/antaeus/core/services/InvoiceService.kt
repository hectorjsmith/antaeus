/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetchAllByStatusAndMaxCreationTime(
        statusList: Set<InvoiceStatus>,
        maxCreationTime: DateTime
    ): List<Invoice> {
        return dal.fetchInvoices(statusList, maxCreationTime)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(invoice: Invoice): Invoice {
        return dal.updateInvoice(invoice) ?: throw InvoiceNotFoundException(invoice.id)
    }
}
