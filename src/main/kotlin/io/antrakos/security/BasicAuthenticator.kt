package io.antrakos.security

import io.antrakos.User
import io.antrakos.repository.impl.UserRepository
import org.pac4j.core.exception.CredentialsException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.util.CommonHelper
import org.pac4j.http.credentials.UsernamePasswordCredentials
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator
import org.pac4j.http.credentials.password.PasswordEncoder
import org.pac4j.http.profile.HttpProfile

/**
 * @author Taras Zubrei
 */
class BasicAuthenticator(val userRepository: UserRepository, val passwordEncoder: PasswordEncoder) : UsernamePasswordAuthenticator {
    override fun validate(credentials: UsernamePasswordCredentials?) {
        if (credentials == null)
            throw CredentialsException("No credential")
        if (CommonHelper.isBlank(credentials.username)) {
            throw CredentialsException("Username cannot be blank")
        }
        if (CommonHelper.isBlank(credentials.password)) {
            throw CredentialsException("Password cannot be blank")
        }
        val user: User = userRepository.findByUsername(credentials.username).toBlocking().value()
                ?: throw CredentialsException("No user found")
        if (user.password != passwordEncoder.encode(credentials.password))
            throw CredentialsException("Wrong password")
        val profile = HttpProfile()
        profile.setId(user._id)
        profile.addAttribute(CommonProfile.USERNAME, credentials.username)
        credentials.userProfile = profile
    }
}