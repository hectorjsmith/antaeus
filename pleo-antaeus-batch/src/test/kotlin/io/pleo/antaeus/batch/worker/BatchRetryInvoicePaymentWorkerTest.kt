package io.pleo.antaeus.batch.worker

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.pleo.antaeus.core.external.NotificationService
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BatchRetryInvoicePaymentWorkerTest {
    private val notificationService = mockk<NotificationService>(relaxed = true)
    private val billingService = mockk<BillingService>(relaxed = true)

    @Test
    fun given_BatchWorker_When_Run_Then_FetchFunctionCalledOnceWithCorrectStatus() {
        // Assemble
        val invoiceService = mockk<InvoiceService>(relaxed = true)

        val worker = BatchRetryInvoicePaymentWorker(
            invoiceService,
            billingService,
            notificationService
        )

        // Act
        worker.run()

        // Assert
        verify(exactly = 1) { invoiceService.fetchAllByStatusAndRetryPaymentTime(
            statusList = setOf(InvoiceStatus.FAILED, InvoiceStatus.PROCESSING),
            maxRetryPaymentTime = any()
        ) }
    }

    @Test
    fun given_BatchWorker_When_RunFindsProcessingInvoices_Then_InvoicesSetToFailedAndRetryTimeIsNull() {
        // Assemble
        var invoiceInDb = newInvoice(InvoiceStatus.PROCESSING)
        val invoiceSlot = slot<Invoice>()
        val invoiceService = mockk<InvoiceService>(relaxed = true) {
            every { fetchAllByStatusAndRetryPaymentTime(any(), any()) } answers { listOf(invoiceInDb) }
            every { update(capture(invoiceSlot)) } answers {
                invoiceInDb = invoiceSlot.captured
                invoiceInDb
            }
        }

        val worker = BatchRetryInvoicePaymentWorker(
            invoiceService,
            billingService,
            notificationService
        )

        // Act
        worker.run()

        // Assert
        assertEquals(InvoiceStatus.FAILED, invoiceInDb.status, "Invoice status should be set to FAILED when invoice stuck in PROCESSING")
        assertNull(invoiceInDb.retryPaymentTime, "Invoice retry time should be null when invoice stuck in PROCESSING")
    }

    private fun newInvoice(status: InvoiceStatus): Invoice {
        return Invoice(10, 20, Money(BigDecimal.valueOf(10), Currency.EUR), status, DateTime.now().minusDays(2), DateTime.now().minusDays(1))
    }
}