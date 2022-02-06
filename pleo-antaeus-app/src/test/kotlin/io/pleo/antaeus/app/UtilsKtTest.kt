package io.pleo.antaeus.app

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import setupInitialData
import java.io.File

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
