package io.pleo.antaeus.app

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.math.BigDecimal
import java.util.stream.Stream

class InvoiceQueryTests {
    companion object {
        @JvmStatic
        fun statusAndMaxCreationTimeProviderForQueryTest(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(setOf<InvoiceStatus>(), { DateTime.now().plusMinutes(1) }, false),
                Arguments.of(setOf(InvoiceStatus.READY), { DateTime.now().plusMinutes(1) }, true),
                Arguments.of(setOf(InvoiceStatus.READY, InvoiceStatus.PENDING), { DateTime.now().plusMinutes(1) }, true),
                Arguments.of(setOf(InvoiceStatus.READY, InvoiceStatus.PENDING, InvoiceStatus.PAID),
                    { DateTime.now().plusMinutes(1) }, true),
                Arguments.of(setOf(InvoiceStatus.READY, InvoiceStatus.PENDING, InvoiceStatus.PAID, InvoiceStatus.FAILED),
                    { DateTime.now().plusMinutes(1) }, true),
                Arguments.of(setOf(InvoiceStatus.READY), { DateTime.now().plusSeconds(2) }, true),
                Arguments.of(setOf(InvoiceStatus.READY), { DateTime.now() }, true),
                Arguments.of(setOf(InvoiceStatus.READY), { DateTime.now().minusMinutes(1) }, false)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("statusAndMaxCreationTimeProviderForQueryTest")
    fun given_SqliteDatabaseWithInvoices_When_FetchInvoicesWithStatusAndMaxCreationDate_Then_CorrectInvoicesReturned(
        statusSet: Set<InvoiceStatus>,
        maxCreationTimeFn: () -> DateTime,
        atLeastOneResultExpected: Boolean
    ) {
        // Assemble
        val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
        val dal = connectToDbAndBuildDal(dbFile)

        val money = Money(BigDecimal.valueOf(100), Currency.EUR)
        val customer = dal.createCustomer(Currency.EUR)!!
        for (i in 0 until 20) {
            dal.createInvoice(money, customer, InvoiceStatus.values().toList().shuffled()[0])
        }

        // Act
        val maxCreationTime = maxCreationTimeFn()
        val invoices = dal.fetchInvoices(statusSet, maxCreationTime)
        println("Found ${invoices.size} invoices")

        // Assert
        Assertions.assertNotEquals(atLeastOneResultExpected, invoices.isEmpty(), "Unexpected number of results")
        Assertions.assertEquals(
            null,
            invoices.firstOrNull { !statusSet.contains(it.status) },
            "There should be no invoice with a status not included in the query"
        )
        Assertions.assertEquals(
            null,
            invoices.firstOrNull { it.creationTime.isAfter(maxCreationTime) },
            "There should be no invoice with a creation time after the max provided in the query"
        )
    }
}
