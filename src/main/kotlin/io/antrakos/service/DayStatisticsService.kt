package io.antrakos.service

import io.antrakos.*
import io.antrakos.repository.impl.RecordRepository
import rx.Observable
import rx.Single
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @author Taras Zubrei
 */
class DayStatisticsService(val recordRepository: RecordRepository) {
    fun get(day: LocalDate, userId: String): Single<DayStatistics> = recordRepository.findWithinOfUser(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), userId)
            .let { analyze(it, day) }

    companion object {
        fun analyze(data: Observable<Record>, day: LocalDate): Single<DayStatistics> = data.map { RecordDto(it.status, it.date()) }
                .toList()
                .toSingle()
                .map { fillInGaps(it) }
                .map { create(it, day) }

        fun create(records: List<RecordDto>, day: LocalDate) = DayStatistics(calculateDuration(records), markHours(records), day)

        fun fillInGaps(records: List<RecordDto>): List<RecordDto> {
            if (records.isEmpty())
                return mutableListOf()
            val list: MutableList<RecordDto> = records.toMutableList()
            if (list[0].status == Status.STOP)
                list.add(0, RecordDto(Status.START, list[0].time.toLocalDate().atStartOfDay()))
            if (list.last().status == Status.START)
                if (!list.last().time.toLocalDate().isEqual(LocalDate.now()))
                    list.add(RecordDto(Status.STOP, list.last().time.toLocalDate().plusDays(1).atStartOfDay()))
                else
                    list.add(RecordDto(Status.STOP, LocalDateTime.now()))
            return list.toList()
        }

        fun markHours(records: List<RecordDto>): BooleanArray {
            val hours: BooleanArray = BooleanArray(24)
            (0..records.lastIndex).asSequence()
                    .filter { it % 2 == 0 }
                    .map { records[it] to records[it + 1] }
                    .forEach {
                        val start = it.first.time.hour
                        val end = if (it.second.time.dayOfYear == it.first.time.dayOfYear) it.second.time.hour else 23
                        if (start == end) {
                            if (Duration.between(it.first.time, it.second.time).toMinutes() >= 30)
                                hours[start] = true
                        } else {
                            if (Duration.between(it.first.time, LocalDateTime.of(it.first.time.toLocalDate(), LocalTime.of(it.first.time.hour + 1, 0))).toMinutes() >= 30)
                                hours[it.first.time.hour] = true
                            if (it.second.time.dayOfYear != it.first.time.dayOfYear)
                                hours[end] = true
                            if (Duration.between(LocalDateTime.of(it.second.time.toLocalDate(), LocalTime.of(it.second.time.hour, 0)), it.second.time).toMinutes() >= 30)
                                hours[it.second.time.hour] = true
                            if (start + 1 <= end - 1)
                                (start + 1..end - 1).forEach { hours[it] = true }
                        }
                    }
            return hours
        }

        fun calculateDuration(records: List<RecordDto>): Duration {
            if (records.isEmpty())
                return Duration.ZERO
            return (0..records.lastIndex).asSequence()
                    .filter { it % 2 == 0 }
                    .map { records[it] to records[it + 1] }
                    .map { Duration.between(it.first.time, it.second.time) }
                    .reduce(Duration::plus)
        }
    }
}