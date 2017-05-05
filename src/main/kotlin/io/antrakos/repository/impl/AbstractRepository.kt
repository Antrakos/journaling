package io.antrakos.repository.impl

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.Success
import io.antrakos.Entity
import io.antrakos.repository.Repository
import org.bson.conversions.Bson
import rx.Observable
import rx.Single

/**
 * @author Taras Zubrei
 */
open class AbstractRepository<T>(val collection: MongoCollection<T>) : Repository<T> where T : Entity {
    override fun insert(item: T): Single<Success> = collection.insertOne(item).toSingle()
    override fun insertMany(items: List<T>): Observable<Success> = collection.insertMany(items)
    override fun findOne(id: String): Single<T> = collection.find(eq("_id", id)).first().toSingle()
    override fun findAll(): Observable<T> = collection.find().toObservable()
    override fun delete(id: String): Single<DeleteResult> = collection.deleteOne(eq("_id", id)).toSingle()
    override fun delete(item: T): Single<DeleteResult> = collection.deleteOne(eq("_id", item._id)).toSingle()
    override fun replace(item: T): Single<UpdateResult> = collection.replaceOne(eq("_id", item._id), item).toSingle()
    override fun update(id: String, update: Bson): Single<UpdateResult> = collection.updateOne(eq("_id", id), update).toSingle()
}