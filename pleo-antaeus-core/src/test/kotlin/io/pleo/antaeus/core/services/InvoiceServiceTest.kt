package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun given_InvoiceWithCreationTimeLastMonth_When_CheckIfDue_Then_ReturnTrue() {
        // Assemble
        val now = DateTime.parse("2022-02-15T12:00:00")
        val invoiceCreationTime = DateTime.parse("2022-01-31T23:59:59")
        val invoice = newInvoice(invoiceCreationTime)

        // Act
        val isDue = invoiceService.isInvoiceDue(invoice, now)

        // Assert
        Assertions.assertTrue(isDue, "Invoice should be due with creation time last month")
    }

    @Test
    fun given_InvoiceWithCreationTimeThisMonth_When_CheckIfDue_Then_ReturnFalse() {
        // Assemble
        val now = DateTime.parse("2022-01-15T12:00:00")
        val invoice = newInvoice(now.minusHours(1))

        // Act
        val isDue = invoiceService.isInvoiceDue(invoice, now)

        // Assert
        Assertions.assertFalse(isDue, "Invoice should not be due with creation time this month")
    }

    private fun newInvoice(creationTime: DateTime): Invoice {
        return Invoice(10, 20, Money(BigDecimal.valueOf(10), Currency.EUR), InvoiceStatus.PENDING, creationTime, null)
    }
}
