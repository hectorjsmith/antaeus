package io.pleo.antaeus.app

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal

class InvoiceUpdateTests {
    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InvoiceStatusUpdated_Then_DataIsSetCorrectly() {
        // Assemble
        val initialStatus = InvoiceStatus.PENDING
        val newStatus = InvoiceStatus.PAID
        Assertions.assertNotEquals(
            initialStatus,
            newStatus,
            "Initial and new status values cannot be the same - this is an invalid test"
        )

        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)
        val customer = dal.createCustomer(Currency.EUR)!!
        val invoice = dal.createInvoice(Money(BigDecimal.valueOf(1000), Currency.EUR), customer, initialStatus)!!

        // Act
        val updatedInvoice = dal.updateInvoice(invoice, newStatus)!!
        val reloadedInvoice = dal.fetchInvoice(invoice.id)!!

        // Assert
        Assertions.assertEquals(newStatus, updatedInvoice.status, "Invoice status should be updated on returned object")
        Assertions.assertEquals(newStatus, reloadedInvoice.status, "Invoice status should be updated in database")
    }

    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InvoiceNextRetryUpdated_Then_DataIsSetCorrectly() {
        // Assemble
        val newRetryTime = DateTime.now()
        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)
        val customer = dal.createCustomer(Currency.EUR)!!
        val invoice = dal.createInvoice(Money(BigDecimal.valueOf(1000), Currency.EUR), customer, InvoiceStatus.PENDING)!!

        // Act
        val updatedInvoice = dal.updateInvoice(invoice, invoice.status, newRetryTime)!!
        val reloadedInvoice = dal.fetchInvoice(invoice.id)!!

        // Assert
        Assertions.assertEquals(
            newRetryTime,
            updatedInvoice.retryPaymentTime,
            "Invoice retryPaymentTime should be updated on returned object"
        )
        Assertions.assertEquals(
            newRetryTime,
            reloadedInvoice.retryPaymentTime,
            "Invoice retryPaymentTime should be updated in database"
        )
    }

    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InvoiceUpdated_Then_DoesNotAffectOtherInvoices() {
        // Assemble
        val newRetryTime = DateTime.now()
        val initialStatus = InvoiceStatus.PENDING
        val newStatus = InvoiceStatus.PAID
        Assertions.assertNotEquals(
            initialStatus,
            newStatus,
            "Initial and new status values cannot be the same - this is an invalid test"
        )

        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)
        val customer = dal.createCustomer(Currency.EUR)!!
        val invoice1 = dal.createInvoice(Money(BigDecimal.valueOf(1000), Currency.EUR), customer, initialStatus)!!
        val invoice2 = dal.createInvoice(Money(BigDecimal.valueOf(2000), Currency.EUR), customer, initialStatus)!!

        // Act
        val updatedInvoice1 = dal.updateInvoice(invoice1, newStatus, newRetryTime)!!
        val reloadedInvoice2 = dal.fetchInvoice(invoice2.id)!!

        // Assert
        Assertions.assertEquals(
            newStatus,
            updatedInvoice1.status,
            "Invoice status should be updated on returned object"
        )
        Assertions.assertEquals(
            newRetryTime,
            updatedInvoice1.retryPaymentTime,
            "Invoice retryPaymentTime should be updated on returned object"
        )
        Assertions.assertNotEquals(
            newStatus,
            reloadedInvoice2.status,
            "Invoice status should not be updated on unrelated invoice"
        )
        Assertions.assertNotEquals(
            newRetryTime,
            reloadedInvoice2.retryPaymentTime,
            "Invoice retryPaymentTime should not be updated on unrelated invoice"
        )
    }
}
