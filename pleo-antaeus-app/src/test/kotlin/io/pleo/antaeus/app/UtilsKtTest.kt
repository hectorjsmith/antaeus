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
}
