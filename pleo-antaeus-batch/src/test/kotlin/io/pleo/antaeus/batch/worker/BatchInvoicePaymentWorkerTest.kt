package io.pleo.antaeus.batch.worker

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BatchInvoicePaymentWorkerTest {
    @Test
    fun given_BatchWorker_When_Run_Then_FetchFunctionCalledOnceWithCorrectStatus() {
        // Assemble
        val invoiceService = mockk<InvoiceService>(relaxed = true)
        val billingService = mockk<BillingService>(relaxed = true)

        val worker = BatchInvoicePaymentWorker(
            invoiceService,
            billingService
        )

        // Act
        worker.run()

        // Assert
        verify(exactly = 1) { invoiceService.fetchAllByStatusAndMaxCreationTime(
            statusList = setOf(InvoiceStatus.READY, InvoiceStatus.PENDING),
            maxCreationTime = any()
        ) }
    }

    @Test
    fun given_BatchWorker_When_Run_Then_EveryInvoiceFoundIsProcessedAndSaved() {
        // Assemble
        val invoiceSlot = slot<Invoice>()
        val invoiceService = mockk<InvoiceService> {
            every { fetchAllByStatusAndMaxCreationTime(any(), any()) } returns listOf(
                Invoice(10, 20, Money(BigDecimal.valueOf(10), Currency.EUR), InvoiceStatus.READY, DateTime.now(), null),
                Invoice(11, 20, Money(BigDecimal.valueOf(20), Currency.EUR), InvoiceStatus.READY, DateTime.now(), null),
                Invoice(12, 20, Money(BigDecimal.valueOf(30), Currency.EUR), InvoiceStatus.READY, DateTime.now(), null)
            )
            every { update(capture(invoiceSlot)) } answers { invoiceSlot.captured }
        }

        val billingService = mockk<BillingService> {
            every { processAndSaveInvoice(capture(invoiceSlot)) } answers { invoiceSlot.captured }
        }

        val worker = BatchInvoicePaymentWorker(
            invoiceService,
            billingService
        )

        // Act
        worker.run()

        // Assert
        verify(exactly = 3) { billingService.processAndSaveInvoice(any()) }
    }
}
