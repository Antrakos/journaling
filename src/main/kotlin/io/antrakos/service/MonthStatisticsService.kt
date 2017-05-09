package io.antrakos.service

import io.antrakos.DayStatistics
import io.antrakos.MonthStatistics
import io.antrakos.repository.impl.RecordRepository
import rx.Single
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

/**
 * @author Taras Zubrei
 */
class MonthStatisticsService(val recordRepository: RecordRepository) {
    fun get(month: Int, userId: String): Single<MonthStatistics> = recordRepository.findWithinMonthOfUserGropedByDay(LocalDate.now().year, month, userId)
            .flatMap { pair -> DayStatisticsService.analyze(pair, LocalDate.of(LocalDate.now().year, month, pair.key)).toObservable() }
            .toList()
            .map { create(LocalDate.of(LocalDate.now().year, month, 1), it) }
            .toSingle()

    companion object {
        fun create(month: LocalDate, days: List<DayStatistics>) = MonthStatistics(sumHours(days), maxWorkedHours(month), mapToArray(days, month))

        fun sumHours(days: List<DayStatistics>) = days.sumByDouble { it.time.toHours() + it.time.toMinutes() / 60.0 }

        fun maxWorkedHours(month: LocalDate) = Stream.iterate(month.withDayOfMonth(1), { it.plusDays(1) })
                .limit(month.withDayOfMonth(1).until(month.withDayOfMonth(month.lengthOfMonth()), ChronoUnit.DAYS))
                .filter { it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
                .count() * 8

        fun mapToArray(days: List<DayStatistics>, month: LocalDate): Array<DayStatistics> {
            val array = Array<DayStatistics>(if (month.withDayOfMonth(1) == LocalDate.now().withDayOfMonth(1)) LocalDate.now().dayOfMonth else month.lengthOfMonth(), { DayStatisticsService.create(listOf(), month.withDayOfMonth(it + 1)) })
            days.forEach { array[it.day.dayOfMonth - 1] = it }
            return array
        }
    }
}