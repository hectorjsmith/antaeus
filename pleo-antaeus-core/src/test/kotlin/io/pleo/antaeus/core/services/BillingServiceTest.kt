package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class BillingServiceTest {
    private val invoiceSlot = slot<Invoice>()
    private val mockkNotificationService = mockk<NotificationService>(relaxed = true)
    private val mockkInvoiceService = mockk<InvoiceService> {
        every { isInvoiceDue(any(), any()) } returns true
        every { update(capture(invoiceSlot)) } answers { invoiceSlot.captured }
    }

    @Test
    fun given_PaidInvoice_When_Charged_Then_ExceptionThrown() {
        // Assemble
        val paidInvoice = newInvoiceReadyForProcessing().copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act / Assert
        assertThrows<InvoiceAlreadyPaidException> { billingService.processAndSaveInvoice(paidInvoice) }
    }

    @Test
    fun given_Invoice_When_ChargeSucceeds_Then_InvoiceStatusSetToPaid() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.PAID, updatedInvoice.status, "Invoice status should be PAID after successful processing")
    }

    @Test
    fun given_InvoiceWithRetryTimeSet_When_ChargeSucceeds_Then_InvoiceRetryTimeIsNull() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing().copy(retryPaymentTime = DateTime.now())
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertNull(updatedInvoice.retryPaymentTime, "Invoice payment retry time should be null after processing")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForInsufficientFunds_Then_InvoiceStatusSetToFailedAndRetryTimeSetInFuture() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns false
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        assertNotEquals(null, updatedInvoice.retryPaymentTime, "Invoice retry time should not be null")
        assertTrue(updatedInvoice.retryPaymentTime?.isAfterNow!!, "Invoice retry time should be in the future")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForNetworkError_Then_InvoiceStatusSetToFailedAndRetryTimeSetInFuture() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws(NetworkException())
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        assertNotEquals(null, updatedInvoice.retryPaymentTime, "Invoice retry time should not be null")
        assertTrue(updatedInvoice.retryPaymentTime?.isAfterNow!!, "Invoice retry time should be in the future")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForCustomerNotFound_Then_InvoiceStatusSetToFailedAndRetryTimeIsNull() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws(CustomerNotFoundException(invoice.customerId))
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        assertEquals(null, updatedInvoice.retryPaymentTime, "Invoice retry time should be null")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForCurrencyMismatch_Then_InvoiceStatusSetToFailedAndRetryTimeIsNull() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws(CurrencyMismatchException(invoice.id, invoice.customerId))
        }
        val billingService = BillingService(mockkInvoiceService, mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processAndSaveInvoice(invoice)

        // Assert
        assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        assertEquals(null, updatedInvoice.retryPaymentTime, "Invoice retry time should be null")
    }

    @Test
    fun given_ReadyInvoice_When_ProcessFailsAfterPaymentButBeforeSave_Then_InvoiceIsNotLeftInReadyState() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        var invoiceInDb = invoice
        val invoiceSlot = slot<Invoice>()
        val invoiceService = mockk<InvoiceService> {
            every { update(capture(invoiceSlot)) } answers {
                if (invoiceSlot.captured.status == InvoiceStatus.PAID) {
                    throw Exception("some db exception")
                } else {
                    invoiceInDb = invoiceSlot.captured
                    invoiceSlot.captured
                }
            }
            every { isInvoiceDue(any(), any()) } returns true
        }
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val billingService = BillingService(invoiceService, mockkNotificationService, paymentProvider)

        // Act
        try {
            billingService.processAndSaveInvoice(invoice)
        } catch (ex: Exception) {
            println("exception thrown: " + ex.message)
        }

        // Assert
        assertFalse(
            setOf(InvoiceStatus.READY, InvoiceStatus.PENDING).contains(invoiceInDb.status),
            "Invoice should not be left in READY or PENDING state"
        )
    }

    @Test
    fun given_ReadyInvoice_When_ProcessFailsAfterPaymentButBeforeSave_Then_RetryingPaymentThrowsException() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        var invoiceInDb = invoice
        val invoiceSlot = slot<Invoice>()
        val invoiceService = mockk<InvoiceService> {
            every { update(capture(invoiceSlot)) } answers {
                if (invoiceSlot.captured.status == InvoiceStatus.PAID) {
                    throw Exception("some db exception")
                } else {
                    invoiceInDb = invoiceSlot.captured
                    invoiceSlot.captured
                }
            }
            every { isInvoiceDue(any(), any()) } returns true
        }
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val billingService = BillingService(invoiceService, mockkNotificationService, paymentProvider)

        // Act
        try {
            billingService.processAndSaveInvoice(invoice)
        } catch (ex: Exception) {
            println("exception thrown: " + ex.message)
        }

        // Assert
        assertThrows<InvoiceAlreadyInProcessException> { billingService.processAndSaveInvoice(invoiceInDb) }
    }

    private fun newInvoiceReadyForProcessing(): Invoice {
        return Invoice(
            10,
            20,
            Money(BigDecimal.valueOf(500), Currency.EUR),
            InvoiceStatus.READY,
            DateTime.now(),
            null
        )
    }
}