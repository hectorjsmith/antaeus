package io.pleo.antaeus.batch.worker

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoiceValidationService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BatchInvoiceValidationWorkerTest {
    @Test
    fun given_BatchWorker_When_Run_Then_EveryInvoiceFoundIsValidatedAndSaved() {
        // Assemble
        val invoiceSlot = slot<Invoice>()
        val invoiceService = mockk<InvoiceService> {
            every { fetchAllByStatus(any()) } returns listOf(
                newInvoice(1),
                newInvoice(2),
                newInvoice(3)
            )
            every { update(capture(invoiceSlot)) } answers { invoiceSlot.captured }
        }

        val validationService = mockk<InvoiceValidationService> {
            every { validateAndSaveInvoice(capture(invoiceSlot)) } answers { invoiceSlot.captured }
        }

        val worker = BatchInvoiceValidationWorker(
            invoiceService,
            validationService
        )

        // Act
        worker.run()

        // Assert
        verify(exactly = 3) { validationService.validateAndSaveInvoice(any()) }
    }

    private fun newInvoice(id: Int): Invoice {
        return Invoice(
            id,
            20,
            Money(BigDecimal.valueOf(100), Currency.EUR),
            InvoiceStatus.PENDING,
            DateTime.now(),
            null
        )
    }
}