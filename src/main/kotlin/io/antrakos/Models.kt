package io.antrakos

import org.bson.types.ObjectId
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @author Taras Zubrei
 */
class Record(val status: Status, val userId: String) : Entity()

data class User(val username: String, val password: String) : Entity()
data class RecordDto(val status: Status, val time: LocalDateTime)
data class DayStatistics(val time: Duration, val hours: BooleanArray, val day: LocalDate)
data class MonthStatistics(val hours: Double, val totalHours: Long, val days: Array<DayStatistics>)

fun Record.date(): LocalDateTime = ObjectId(this._id).date.toLocalDateTime()
enum class Status {
    START, STOP;

    operator fun not(): Status = if (this == STOP) START else STOP
}