package io.antrakos.repository.impl

import com.mongodb.client.model.Filters.eq
import com.mongodb.rx.client.MongoCollection
import io.antrakos.User
import rx.Single

/**
 * @author Taras Zubrei
 */
class UserRepository(collection: MongoCollection<User>) : AbstractRepository<User>(collection) {
    fun findByUsername(username: String): Single<User> = collection.find().filter(eq("username", username)).first().toSingle()
}