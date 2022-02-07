package io.pleo.antaeus.app

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import setupInitialData
import java.io.File
import java.math.BigDecimal

internal class UtilsKtTest {
    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InitialDataSetup_Then_AtLeastOneInvoiceAdded() {
        // Assemble
        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)

        // Act / Assert
        assertDoesNotThrow { setupInitialData(dal) }
        assertTrue(dal.fetchInvoices().isNotEmpty(), "At least one invoice should exist after initial data setup")
    }

    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InvoiceStatusUpdated_Then_DataIsSetCorrectly() {
        // Assemble
        val initialStatus = InvoiceStatus.PENDING
        val newStatus = InvoiceStatus.PAID
        assertNotEquals(initialStatus, newStatus, "Initial and new status values cannot be the same - this is an invalid test")

        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)
        val customer = dal.createCustomer(Currency.EUR)!!
        val invoice = dal.createInvoice(Money(BigDecimal.valueOf(1000), Currency.EUR), customer, initialStatus)!!

        // Act
        val updatedInvoice = dal.updateInvoice(invoice, newStatus)!!
        val reloadedInvoice = dal.fetchInvoice(invoice.id)!!

        // Assert
        assertEquals(newStatus, updatedInvoice.status, "Invoice status should be updated on returned object")
        assertEquals(newStatus, reloadedInvoice.status, "Invoice status should be updated in database")
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
        assertEquals(newRetryTime, updatedInvoice.nextRetry, "Invoice retryTime should be updated on returned object")
        assertEquals(newRetryTime, reloadedInvoice.nextRetry, "Invoice retryTime should be updated in database")
    }

    @Test
    fun given_SqliteDatabaseBackedByTempFile_When_InvoiceUpdated_Then_DoesNotAffectOtherInvoices() {
        // Assemble
        val newRetryTime = DateTime.now()
        val initialStatus = InvoiceStatus.PENDING
        val newStatus = InvoiceStatus.PAID
        assertNotEquals(initialStatus, newStatus, "Initial and new status values cannot be the same - this is an invalid test")

        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)
        val customer = dal.createCustomer(Currency.EUR)!!
        val invoice1 = dal.createInvoice(Money(BigDecimal.valueOf(1000), Currency.EUR), customer, initialStatus)!!
        val invoice2 = dal.createInvoice(Money(BigDecimal.valueOf(2000), Currency.EUR), customer, initialStatus)!!

        // Act
        val updatedInvoice1 = dal.updateInvoice(invoice1, newStatus, newRetryTime)!!
        val reloadedInvoice2 = dal.fetchInvoice(invoice2.id)!!

        // Assert
        assertEquals(newStatus, updatedInvoice1.status, "Invoice status should be updated on returned object")
        assertEquals(newRetryTime, updatedInvoice1.nextRetry, "Invoice retryTime should be updated on returned object")
        assertNotEquals(newStatus, reloadedInvoice2.status, "Invoice status should not be updated on unrelated invoice")
        assertNotEquals(newRetryTime, reloadedInvoice2.nextRetry, "Invoice retryTime should not be updated on unrelated invoice")
    }
}
