package io.antrakos

import org.bson.types.ObjectId
import java.time.LocalDateTime

/**
 * @author Taras Zubrei
 */
class Record(val status: Status, val userId: String) : Entity()

data class User(val username: String, val password: String) : Entity()

fun Record.date(): LocalDateTime = ObjectId(this._id).date.toLocalDateTime()
enum class Status {
    START, STOP;

    operator fun not(): Status = if (this == STOP) START else STOP
}