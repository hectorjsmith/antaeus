package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.models.*
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class InvoiceValidationServiceTest {
    private val intSlot = slot<Int>()
    private val invoiceSlot = slot<Invoice>()
    private val notificationService = mockk<NotificationService>(relaxed = true)
    private val mockkInvoiceService = mockk<InvoiceService>(relaxed = true) {
        every { isInvoiceDue(any()) } returns true
        every { update(capture(invoiceSlot)) } answers { invoiceSlot.captured }
    }

    @Test
    fun given_InvoiceAndCustomerWithSameCurrency_When_Validated_Then_InvoiceStatusSetToReady() {
        // Assemble
        val currency = Currency.EUR
        val customerService = mockk<CustomerService> {
            every { fetch(capture(intSlot)) } answers { Customer(intSlot.captured, currency) }
        }
        val validationService = InvoiceValidationService(mockkInvoiceService, notificationService, customerService)
        val invoice = newInvoice(currency)
        assertNotEquals(InvoiceStatus.READY, invoice.status, "Invoice status should not be READY before test - this is an invalid test")

        // Act
        val newInvoice = validationService.validateAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.READY, newInvoice.status, "Invoice status should be set to READY when no errors found")
    }

    @Test
    fun given_FailedInvoiceThatIsNowValid_When_Validated_Then_InvoiceStatusSetToReady() {
        // Assemble
        val currency = Currency.EUR
        val customerService = mockk<CustomerService> {
            every { fetch(capture(intSlot)) } answers { Customer(intSlot.captured, currency) }
        }
        val validationService = InvoiceValidationService(mockkInvoiceService, notificationService, customerService)
        val invoice = newInvoice(currency).copy(status = InvoiceStatus.FAILED)
        assertEquals(InvoiceStatus.FAILED, invoice.status, "Invoice status should be FAILED before test - this is an invalid test")

        // Act
        val newInvoice = validationService.validateAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.READY, newInvoice.status, "Invoice status should be set to READY when no errors found")
    }

    @Test
    fun given_InvoiceAndCustomerWithDifferentCurrencies_When_Validated_Then_InvoiceStatusSetToFailed() {
        // Assemble
        val invoiceCurrency = Currency.EUR
        val customerCurrency = Currency.DKK
        val customerService = mockk<CustomerService> {
            every { fetch(capture(intSlot)) } answers { Customer(intSlot.captured, customerCurrency) }
        }
        val validationService = InvoiceValidationService(mockkInvoiceService, notificationService, customerService)
        val invoice = newInvoice(invoiceCurrency)
        assertNotEquals(InvoiceStatus.READY, invoice.status, "Invoice status should not be READY before test - this is an invalid test")
        assertNotEquals(invoiceCurrency, customerCurrency, "Invoice currency should not match customer currency - this is an invalid test")

        // Act
        val newInvoice = validationService.validateAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, newInvoice.status, "Invoice status should be set to FAILED when validation fails")
    }

    @Test
    fun given_InvoiceWithNotFoundCustomerId_When_Validated_Then_InvoiceStatusSetToFailed() {
        // Assemble
        val customerService = mockk<CustomerService> {
            every { fetch(capture(intSlot)) } answers { throw CustomerNotFoundException(intSlot.captured) }
        }
        val validationService = InvoiceValidationService(mockkInvoiceService, notificationService, customerService)
        val invoice = newInvoice(Currency.EUR)
        assertNotEquals(InvoiceStatus.FAILED, invoice.status, "Invoice status should not be FAILED before test - this is an invalid test")

        // Act
        val newInvoice = validationService.validateAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, newInvoice.status, "Invoice status should be set to FAILED when validation fails")
    }

    @Test
    fun given_InvoiceWithPaidStatus_When_Validated_Then_ExceptionThrown() {
        // Assemble
        val customerService = mockk<CustomerService>(relaxed = true)
        val validationService = InvoiceValidationService(mockkInvoiceService, notificationService, customerService)
        val invoice = newInvoice(Currency.EUR).copy(status = InvoiceStatus.PAID)

        // Act / Assert
        assertThrows<InvoiceAlreadyPaidException> { validationService.validateAndSaveInvoice(invoice) }
    }

    private fun newInvoice(currency: Currency): Invoice {
        return Invoice(
            10,
            20,
            Money(BigDecimal.valueOf(500), currency),
            InvoiceStatus.PENDING,
            DateTime.now(),
            null
        )
    }
}