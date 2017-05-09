package io.antrakos.service

import com.mongodb.rx.client.Success
import io.antrakos.User
import io.antrakos.repository.impl.UserRepository
import org.pac4j.http.credentials.password.PasswordEncoder
import rx.Single

/**
 * @author Taras Zubrei
 */
class AuthenticationService(val userRepository: UserRepository, val passwordEncoder: PasswordEncoder) {
    fun register(data: Single<User>): Single<Success> = data
            .map { it.copy(password = passwordEncoder.encode(it.password)) }
            .flatMap(userRepository::insert)
}