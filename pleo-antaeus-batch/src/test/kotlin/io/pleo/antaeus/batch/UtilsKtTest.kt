package io.pleo.antaeus.batch

import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class UtilsKtTest {

    companion object {
        @JvmStatic
        fun startDateAndExpectedDateForStartOfMonthTest(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(DateTime.parse("2000-05-05T14:12:11"), DateTime.parse("2000-05-01T00:00:00")),
                Arguments.of(DateTime.parse("2000-01-01T23:59:59"), DateTime.parse("2000-01-01T00:00:00")),
                Arguments.of(DateTime.parse("2000-02-29T23:59:59"), DateTime.parse("2000-02-01T00:00:00")),
                Arguments.of(DateTime.parse("2000-12-31T23:59:59"), DateTime.parse("2000-12-01T00:00:00")),
                Arguments.of(DateTime.parse("2000-02-01T00:00:00"), DateTime.parse("2000-02-01T00:00:00")),
                Arguments.of(DateTime.parse("2000-01-01T00:00:01"), DateTime.parse("2000-01-01T00:00:00"))
            )
        }
    }

    @ParameterizedTest
    @MethodSource("startDateAndExpectedDateForStartOfMonthTest")
    fun given_DateTime_When_DateTimeForStartOfMonthCalculated_Then_CorrectValueReturned(
        startDateTime: DateTime,
        expectedDateTime: DateTime
    ) {
        // Act
        val startOfMonth = calcDateTimeForStartOfMonth(startDateTime)

        // Assert
        assertEquals(expectedDateTime, startOfMonth, "Start of month time should match expected")
    }
}
