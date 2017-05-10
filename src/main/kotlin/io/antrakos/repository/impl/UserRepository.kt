package io.antrakos.repository.impl

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.regex
import com.mongodb.rx.client.MongoCollection
import io.antrakos.User
import rx.Observable
import rx.Single

/**
 * @author Taras Zubrei
 */
class UserRepository(collection: MongoCollection<User>) : AbstractRepository<User>(collection) {
    fun findByUsername(username: String): Single<User> = collection.find().filter(eq("username", username)).first().toSingle()
    fun searchByUsername(username: String): Observable<User> = collection.find().filter(regex("username", ".*$username.*")).toObservable()
}