package io.pleo.antaeus.batch.worker

import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Test

internal class BatchRetryInvoicePaymentWorkerTest {
    @Test
    fun given_BatchWorker_When_Run_Then_FetchFunctionCalledOnceWithCorrectStatus() {
        // Assemble
        val invoiceService = mockk<InvoiceService>(relaxed = true)
        val billingService = mockk<BillingService>(relaxed = true)

        val worker = BatchRetryInvoicePaymentWorker(
            invoiceService,
            billingService
        )

        // Act
        worker.run()

        // Assert
        verify(exactly = 1) { invoiceService.fetchAllByStatusAndRetryPaymentTime(
            statusList = setOf(InvoiceStatus.FAILED),
            maxRetryPaymentTime = any()
        ) }
    }
}