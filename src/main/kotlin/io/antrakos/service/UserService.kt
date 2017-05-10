package io.antrakos.service

import io.antrakos.Record
import io.antrakos.Status
import io.antrakos.UserDto
import io.antrakos.repository.impl.RecordRepository
import io.antrakos.repository.impl.UserRepository
import rx.Observable
import rx.Single

/**
 * @author Taras Zubrei
 */
class UserService(val recordRepository: RecordRepository, val userRepository: UserRepository) {
    fun getStatus(userId: String): Single<Status> = recordRepository.findLastRecordOfUser(userId).map(Record::status)
    fun searchByUsername(username: String): Observable<UserDto> = userRepository.searchByUsername(username).map(::UserDto)
}