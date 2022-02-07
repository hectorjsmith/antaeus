package io.pleo.antaeus.batch.job

import io.pleo.antaeus.batch.firstDayOfEachMonth
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class SchedulesTest {

    companion object {
        @JvmStatic
        fun firstDayOfMonthDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("01/01/2000 00:00:00")),
                Arguments.of(format.parse("01/02/2001 00:00:00")),
                Arguments.of(format.parse("01/12/1999 00:00:00")),
                Arguments.of(format.parse("01/7/2099 00:00:00"))
            )
        }

        @JvmStatic
        fun notFirstDayOfMonthDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("01/01/2000 00:01:00")),
                Arguments.of(format.parse("02/02/2001 00:00:00")),
                Arguments.of(format.parse("31/12/1999 00:00:00")),
                Arguments.of(format.parse("31/8/2099 23:59:59")),
                Arguments.of(format.parse("01/7/2099 00:00:01"))
            )
        }
    }

    @ParameterizedTest
    @MethodSource("firstDayOfMonthDateProvider")
    fun given_FirstDayOfTheMonthSchedule_When_DatesObjectsOnTheFirstOfAnyMonth_Then_ReturnsTrue(
        date: Date
    ) {
        // Act
        val matches = firstDayOfEachMonth.isSatisfiedBy(date)

        // Assert
        Assertions.assertTrue(matches, "First day of the month schedule should be satisfied by date: $date")
    }

    @ParameterizedTest
    @MethodSource("notFirstDayOfMonthDateProvider")
    fun given_FirstDayOfTheMonthSchedule_When_DatesObjectsNotOnTheFirstOfAnyMonth_Then_ReturnsFalse(
        date: Date
    ) {
        // Act
        val matches = firstDayOfEachMonth.isSatisfiedBy(date)

        // Assert
        Assertions.assertFalse(matches, "Schedule should never be satisfied by dates not on the first day of the month: $date")
    }
}