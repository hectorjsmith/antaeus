/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoices(
        statusList: Set<InvoiceStatus>? = null,
        maxCreationTime: DateTime? = null,
        maxRetryPaymentTime: DateTime? = null
    ): List<Invoice> {
        var query = Op.TRUE as Op<Boolean>
        if (statusList != null) {
            query = query.and(InvoiceTable.status.inList(statusList.map { it.toString() }))
        }
        if (maxCreationTime != null) {
            query = query.and(InvoiceTable.creationTime.less(maxCreationTime))
        }
        if (maxRetryPaymentTime != null) {
            query = query.and(InvoiceTable.retryPaymentTime.less(maxRetryPaymentTime))
        }

        return transaction(db) {
            InvoiceTable
                .select { query }
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.creationTime] = DateTime.now()
                    it[this.retryPaymentTime] = null
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoice(invoice: Invoice): Invoice? {
        return updateInvoice(invoice, invoice.status, invoice.retryPaymentTime)
    }

    fun updateInvoice(invoice: Invoice, status: InvoiceStatus): Invoice? {
        return updateInvoice(invoice, status, invoice.retryPaymentTime)
    }

    fun updateInvoice(invoice: Invoice, status: InvoiceStatus, nextRetry: DateTime?): Invoice? {
        transaction(db) {
            InvoiceTable
                .update (
                    where = { InvoiceTable.id.eq(invoice.id)},
                    body = {
                        it[this.status] = status.toString()
                        it[this.retryPaymentTime] = nextRetry
                    }
                )
        }
        return fetchInvoice(invoice.id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
