package io.antrakos.repository

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.rx.client.Success
import io.antrakos.Entity
import org.bson.conversions.Bson
import rx.Observable
import rx.Single

/**
 * @author Taras Zubrei
 */
interface Repository<T> where T : Entity {
    fun insert(item: T): Single<Success>
    fun insertMany(items: List<T>): Observable<Success>
    fun findOne(id: String): Single<T>
    fun findAll(): Observable<T>
    fun delete(id: String): Single<DeleteResult>
    fun update(id: String, update: Bson): Single<UpdateResult>
    fun replace(item: T): Single<UpdateResult>
    fun delete(item: T): Single<DeleteResult>
}