package io.antrakos.repository.impl

import com.mongodb.rx.client.MongoCollection
import io.antrakos.Record
import rx.Observable
import java.time.LocalDateTime


/**
 * @author Taras Zubrei
 */
class RecordRepository(collection: MongoCollection<Record>) : AbstractRepository<Record>(collection) {
    fun findWithin(from: LocalDateTime, to: LocalDateTime): Observable<Record> = collection.find()
//            .filter(and(gte("_id", ObjectId(from.toDate())), lte("_id", ObjectId(to.toDate()))))
            .toObservable()
}