package io.antrakos.repository.impl

import com.mongodb.client.model.Filters.*
import com.mongodb.rx.client.MongoCollection
import io.antrakos.Record
import io.antrakos.toDate
import org.bson.types.ObjectId
import rx.Observable
import java.time.LocalDateTime


/**
 * @author Taras Zubrei
 */
class RecordRepository(collection: MongoCollection<Record>) : AbstractRepository<Record>(collection) {
    fun findWithin(from: LocalDateTime, to: LocalDateTime): Observable<Record> = collection.find()
            .filter(and(gte("_id", ObjectId(from.toDate()).toString()), lte("_id", ObjectId(to.toDate()).toString())))
            .toObservable()
}