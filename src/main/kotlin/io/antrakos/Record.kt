package io.antrakos

import org.bson.types.ObjectId
import java.time.LocalDateTime

/**
 * @author Taras Zubrei
 */
class Record() : Entity() {

}
fun Record.date(): LocalDateTime = ObjectId(this._id).date.toLocalDateTime()