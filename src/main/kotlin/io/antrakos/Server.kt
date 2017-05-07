package io.antrakos

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import io.antrakos.repository.JacksonCodecProvider
import io.antrakos.repository.impl.RecordRepository
import io.antrakos.repository.impl.UserRepository
import io.antrakos.security.BasicAuthenticator
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator
import org.pac4j.http.credentials.password.BasicSaltedSha512PasswordEncoder
import org.pac4j.http.credentials.password.PasswordEncoder
import ratpack.jackson.Jackson.fromJson
import ratpack.jackson.Jackson.json
import ratpack.pac4j.RatpackPac4j
import ratpack.rx.RxRatpack
import ratpack.session.SessionModule
import java.lang.IllegalArgumentException
import java.time.LocalDate


/**
 * @author Taras Zubrei
 */
object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val kodein = Kodein {
            bind<ObjectMapper>() with singleton {
                ObjectMapper().findAndRegisterModules()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
            bind<JacksonCodecProvider>() with singleton { JacksonCodecProvider(instance()) }
            bind<CodecRegistry>() with singleton {
                CodecRegistries.fromRegistries(
                        MongoClients.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(instance<JacksonCodecProvider>())
                )
            }
            bind<MongoClient>() with singleton { MongoClients.create() }
            bind<MongoDatabase>() with singleton { instance<MongoClient>().getDatabase("journaling").withCodecRegistry(instance()) }
            bind<MongoCollection<Record>>() with singleton { instance<MongoDatabase>().getCollection("record", Record::class.java) }
            bind<MongoCollection<User>>() with singleton { instance<MongoDatabase>().getCollection("user", User::class.java) }
            bind<RecordRepository>() with singleton { RecordRepository(instance()) }
            bind<UserRepository>() with singleton { UserRepository(instance()) }
            bind<PasswordEncoder>() with singleton { BasicSaltedSha512PasswordEncoder("salt") }
            bind<UsernamePasswordAuthenticator>() with singleton { BasicAuthenticator(instance(), instance()) }
        }

        RxRatpack.initialize();
        serverStart {
            guiceRegistry {
                module(SessionModule::class.java)
                add(ObjectMapper::class.java, kodein.instance())
            }
            kHandlers {
                all(RatpackPac4j.authenticator(DirectBasicAuthClient(kodein.instance())))
                kPrefix("auth") {
                    post("register") {
                        val userRepository = kodein.instance<UserRepository>()
                        val passwordEncoder = kodein.instance<PasswordEncoder>()
                        parse(fromJson(User::class.java))
                                .toSingle()
                                .map { it.copy(password = passwordEncoder.encode(it.password)) }
                                .flatMap(userRepository::insert)
                                .toPromise()
                                .onError { clientError(400) }
                                .then { render(json(it)) } //TODO: unique username
                    }
                }
                all(RatpackPac4j.requireAuth(DirectBasicAuthClient::class.java))
                kPrefix("record") {
                    get("daily/:date") {
                        val userId = context[UserProfile::class.java].id
                        val recordRepository = kodein.instance<RecordRepository>()
                        val day = LocalDate.parse(pathTokens["date"])
                        recordRepository.findWithinOfUser(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), userId)
                                .map { RecordDto(it.status, it.date()) }
                                .toList()
                                .toSingle()
                                .map { DayStatistics.fillInGaps(it) }
                                .map { it to day }
                                .map(::DayStatistics)
                                .toPromise()
                                .then { render(json(it)) }
                    }
                    get("monthly/:month") {
                        val userId = context[UserProfile::class.java].id
                        val recordRepository = kodein.instance<RecordRepository>()
                        val month = pathTokens["month"]!!.toInt()
                        recordRepository.findWithinMonthOfUserGropedByDay(LocalDate.now().year, month, userId)
                                .flatMap { pair -> pair.map { RecordDto(it.status, it.date()) }.toList().map { DayStatistics.fillInGaps(it) }.map { it to LocalDate.of(LocalDate.now().year, month, pair.key) }.map(::DayStatistics) }
                                .toMap { it.day.dayOfMonth } //TODO: create DTO instead with some general monthly statistics
                                .toSingle()
                                .toPromise()
                                .then { render(json(it)) }
                    }
                }
                post("work/check-in") {
                    try {
                        val userId = context[UserProfile::class.java].id
                        val recordRepository = kodein.instance<RecordRepository>()
                        recordRepository.findLastRecordOfUser(userId)
                                .map(Record::status)
                                .flatMap { recordRepository.insert(Record(!it, userId)).map { success -> !it to success } }
                                .toPromise()
                                .then { render(json(mapOf("status" to it.first))) }
                    } catch (ex: IllegalArgumentException) {
                        clientError(400)
                    }
                }
            }
        }
    }
}