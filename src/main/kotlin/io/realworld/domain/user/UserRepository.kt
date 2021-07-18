package io.realworld.domain.user

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.realworld.domain.exception.EmailAlreadyExistsException
import io.realworld.domain.exception.UserNotFoundException
import io.realworld.domain.exception.UsernameAlreadyExistsException
import io.realworld.infrastructure.security.BCryptHashProvider
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Default
import javax.inject.Inject

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, String> {

    @Inject
    @field:Default
    lateinit var hashProvider: BCryptHashProvider

    fun findByEmail(email: String): User? =
        find("upper(email)", email.toUpperCase().trim()).firstResult()

    fun register(newUser: UserRegistrationReq): User = newUser.run {
        findById(username)?.apply { throw UsernameAlreadyExistsException() }
        findByEmail(email)?.apply { throw EmailAlreadyExistsException() }
        User(username, email, password = hashProvider.hash(password))
    }.apply { persist(this) }

    fun update(id: String, newDetails: UserUpdateReq): User = findById(id)?.run {
        if (newDetails.username != null && newDetails.username != username)
            findById(newDetails.username)?.apply { throw UsernameAlreadyExistsException() }
        if (newDetails.email != null && newDetails.email != email)
            findByEmail(newDetails.email)?.apply { throw EmailAlreadyExistsException() }
        copy(
            username = newDetails.username ?: username,
            email = newDetails.email ?: email,
            password = if (newDetails.password != null) hashProvider.hash(newDetails.password) else password,
            bio = newDetails.bio ?: bio,
            image = newDetails.image ?: image,
        ).apply { persist(this) }
    } ?: throw UserNotFoundException()
}