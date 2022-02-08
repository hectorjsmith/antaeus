package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class BillingServiceTest {
    private val mockkNotificationService = mockk<NotificationService>(relaxed = true)

    @Test
    fun given_PaidInvoice_When_Charged_Then_ExceptionThrown() {
        // Assemble
        val paidInvoice = newInvoiceReadyForProcessing().copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(paidInvoice) } returns true
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act / Assert
        assertThrows<InvoiceAlreadyPaidException> { billingService.processInvoice(paidInvoice) }
    }

    @Test
    fun given_Invoice_When_ChargeSucceeds_Then_InvoiceStatusSetToPaid() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } returns true
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processInvoice(invoice)

        // Assert
        Assertions.assertEquals(InvoiceStatus.PAID, updatedInvoice.status, "Invoice status should be PAID after successful processing")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForInsufficientFunds_Then_InvoiceStatusSetToFailedAndRetryTimeSetInFuture() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } returns false
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processInvoice(invoice)

        // Assert
        Assertions.assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        Assertions.assertNotEquals(null, updatedInvoice.nextRetry, "Invoice retry time should not be null")
        Assertions.assertTrue(updatedInvoice.nextRetry?.isAfterNow!!, "Invoice retry time should be in the future")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForNetworkError_Then_InvoiceStatusSetToFailedAndRetryTimeSetInFuture() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws(NetworkException())
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processInvoice(invoice)

        // Assert
        Assertions.assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        Assertions.assertNotEquals(null, updatedInvoice.nextRetry, "Invoice retry time should not be null")
        Assertions.assertTrue(updatedInvoice.nextRetry?.isAfterNow!!, "Invoice retry time should be in the future")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForCustomerNotFound_Then_InvoiceStatusSetToFailedAndRetryTimeIsNull() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws(CustomerNotFoundException(invoice.customerId))
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processInvoice(invoice)

        // Assert
        Assertions.assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        Assertions.assertEquals(null, updatedInvoice.nextRetry, "Invoice retry time should be null")
    }

    @Test
    fun given_Invoice_When_ChargeFailsForCurrencyMismatch_Then_InvoiceStatusSetToFailedAndRetryTimeIsNull() {
        // Assemble
        val invoice = newInvoiceReadyForProcessing()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws(CurrencyMismatchException(invoice.id, invoice.customerId))
        }
        val billingService = BillingService(mockkNotificationService, paymentProvider)

        // Act
        val updatedInvoice = billingService.processInvoice(invoice)

        // Assert
        Assertions.assertEquals(InvoiceStatus.FAILED, updatedInvoice.status, "Invoice status should be FAILED after failed processing")
        Assertions.assertEquals(null, updatedInvoice.nextRetry, "Invoice retry time should be null")
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