package io.antrakos.service

import io.antrakos.Record
import io.antrakos.Status
import io.antrakos.repository.impl.RecordRepository
import rx.Single

/**
 * @author Taras Zubrei
 */
class WorkService(val recordRepository: RecordRepository) {
    fun checkIn(userId: String): Single<Status> = recordRepository.findLastRecordOfUser(userId)
            .map(Record::status)
            .flatMap { status -> recordRepository.insert(Record(!status, userId)).map { !status } }
}