package io.antrakos.repository.impl

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import com.mongodb.rx.client.MongoCollection
import io.antrakos.Record
import io.antrakos.Status
import io.antrakos.toDate
import io.antrakos.toLocalDateTime
import org.bson.types.ObjectId
import rx.Observable
import rx.Single
import rx.observables.GroupedObservable
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * @author Taras Zubrei
 */
class RecordRepository(collection: MongoCollection<Record>) : AbstractRepository<Record>(collection) {
    fun findWithinOfUser(from: LocalDateTime, to: LocalDateTime, userId: String): Observable<Record> = collection.find()
            .filter(and(
                    gte("_id", ObjectId(from.toDate()).toString()),
                    lte("_id", ObjectId(to.toDate()).toString()),
                    eq("userId", userId)
            ))
            .toObservable()

    fun findWithinMonthOfUserGropedByDay(year: Int, month: Int, userId: String): Observable<GroupedObservable<Int, Record>> = collection.find()
            .filter(and(
                    gte("_id", ObjectId(LocalDate.of(year, month, 1).atStartOfDay().toDate()).toString()),
                    lte("_id", ObjectId((if (month == 12) LocalDate.of(year + 1, 1, 1) else LocalDate.of(year, month + 1, 1)).atStartOfDay().toDate()).toString()),
                    eq("userId", userId)
            ))
            .toObservable()
            .groupBy { ObjectId(it._id).date.toLocalDateTime().dayOfMonth }

    fun findLastRecordOfUser(userId: String): Single<Record> = collection.find()
            .filter(eq("userId", userId))
            .sort(Sorts.descending("_id"))
            .first()
            .firstOrDefault(Record(Status.START, userId))
            .toSingle()
}