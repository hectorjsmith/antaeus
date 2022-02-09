package io.pleo.antaeus.batch

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class SchedulesKtTest {

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

        @JvmStatic
        fun nineAmOnEveryWorkingDayDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("03/01/2022 09:00:00")),
                Arguments.of(format.parse("04/01/2022 09:00:00")),
                Arguments.of(format.parse("05/01/2022 09:00:00")),
                Arguments.of(format.parse("06/01/2022 09:00:00")),
                Arguments.of(format.parse("07/01/2022 09:00:00"))
            )
        }

        @JvmStatic
        fun notNineAmOnEveryWorkingDayDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("01/01/2022 09:00:00")),
                Arguments.of(format.parse("02/01/2022 09:00:00")),
                Arguments.of(format.parse("03/01/2022 09:00:01")),
                Arguments.of(format.parse("04/01/2022 21:00:00")),
                Arguments.of(format.parse("5/01/2022 08:59:59"))
            )
        }

        @JvmStatic
        fun everyHourOnTheHourDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("03/01/2022 09:00:00")),
                Arguments.of(format.parse("04/01/2022 00:00:00")),
                Arguments.of(format.parse("05/01/2022 23:00:00")),
                Arguments.of(format.parse("06/01/2022 12:00:00"))
            )
        }

        @JvmStatic
        fun notEveryHourOnTheHourDateProvider(): Stream<Arguments> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return Stream.of(
                Arguments.of(format.parse("01/01/2022 09:00:01")),
                Arguments.of(format.parse("02/01/2022 09:59:59")),
                Arguments.of(format.parse("03/01/2022 23:59:59"))
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

    @ParameterizedTest
    @MethodSource("nineAmOnEveryWorkingDayDateProvider")
    fun given_NineAmOnWorkingDaySchedule_When_DatesObjectsAtCorrectTime_Then_ReturnsTrue(
        date: Date
    ) {
        // Act
        val matches = nineAmOnEveryWorkingDay.isSatisfiedBy(date)

        // Assert
        Assertions.assertTrue(matches, "Nine AM schedule should be satisfied by date: $date")
    }

    @ParameterizedTest
    @MethodSource("notNineAmOnEveryWorkingDayDateProvider")
    fun given_NineAmOnWorkingDaySchedule_When_DatesObjectsNotAtCorrectTime_Then_ReturnsFalse(
        date: Date
    ) {
        // Act
        val matches = nineAmOnEveryWorkingDay.isSatisfiedBy(date)

        // Assert
        Assertions.assertFalse(matches, "Schedule should never be satisfied by incorrect dates: $date")
    }

    @ParameterizedTest
    @MethodSource("everyHourOnTheHourDateProvider")
    fun given_EveryHourSchedule_When_DatesObjectsAtCorrectTime_Then_ReturnsTrue(
        date: Date
    ) {
        // Act
        val matches = everyHourOnTheHour.isSatisfiedBy(date)

        // Assert
        Assertions.assertTrue(matches, "Every hour schedule should be satisfied by date: $date")
    }

    @ParameterizedTest
    @MethodSource("notEveryHourOnTheHourDateProvider")
    fun given_EveryHourSchedule_When_DatesObjectsNotAtCorrectTime_Then_ReturnsFalse(
        date: Date
    ) {
        // Act
        val matches = everyHourOnTheHour.isSatisfiedBy(date)

        // Assert
        Assertions.assertFalse(matches, "Schedule should never be satisfied by incorrect dates: $date")
    }
}
