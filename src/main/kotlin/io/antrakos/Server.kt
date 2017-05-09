package io.antrakos

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.mongodb.MongoWriteException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import io.antrakos.exception.Error
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
import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.handling.RequestLogger
import ratpack.jackson.Jackson.fromJson
import ratpack.jackson.Jackson.json
import ratpack.pac4j.RatpackPac4j
import ratpack.rx.RxRatpack
import ratpack.session.SessionModule
import java.time.LocalDate
import java.util.*


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
            bind<MongoCollection<User>>() with singleton { instance<MongoDatabase>().getCollection("user", User::class.java).apply { createIndex(Indexes.ascending("username"), IndexOptions().unique(true)).toBlocking().subscribe() } }
            bind<RecordRepository>() with singleton { RecordRepository(instance()) }
            bind<UserRepository>() with singleton { UserRepository(instance()) }
            bind<PasswordEncoder>() with singleton { BasicSaltedSha512PasswordEncoder("salt") }
            bind<UsernamePasswordAuthenticator>() with singleton { BasicAuthenticator(instance(), instance()) }
            bind<ResourceBundle>() with singleton { ResourceBundle.getBundle("messages", Locale.ENGLISH) }
            bind<ClientErrorHandler>() with singleton { io.antrakos.exception.ClientErrorHandler(instance()) }
            bind<ServerErrorHandler>() with singleton { io.antrakos.exception.ServerErrorHandler() }
        }

        RxRatpack.initialize();
        serverStart {
            ServerConfig {
                env()
            }
            GuiceRegistry {
                module(SessionModule::class.java)
                bindInstance(Kodein::class.java, kodein)
                bindInstance(ObjectMapper::class.java, kodein.instance())
                bindInstance(ClientErrorHandler::class.java, kodein.instance())
                bindInstance(ServerErrorHandler::class.java, kodein.instance())
            }
            Handlers {
                all(RequestLogger.ncsa())
                all(RatpackPac4j.authenticator(DirectBasicAuthClient(kodein.instance())))
                Prefix("auth") {
                    Post("register") {
                        val userRepository = instance<UserRepository>()
                        val passwordEncoder = instance<PasswordEncoder>()
                        parse(fromJson(User::class.java))
                                .toSingle()
                                .map { it.copy(password = passwordEncoder.encode(it.password)) }
                                .flatMap(userRepository::insert)
                                .toPromise()
                                .onError {
                                    when (it) {
                                        is MongoWriteException -> ClientError(Error.DUPLICATE_USERNAME)
                                    }
                                }
                                .then { render(json(it)) }
                    }
                }
                all(RatpackPac4j.requireAuth(DirectBasicAuthClient::class.java))
                Prefix("record") {
                    Get("daily/:date") {
                        val userId = instance<UserProfile>().id
                        val recordRepository = instance<RecordRepository>()
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
                    Get("monthly/:month") {
                        val userId = instance<UserProfile>().id
                        val recordRepository = instance<RecordRepository>()
                        val month = pathTokens["month"]!!.toInt()
                        recordRepository.findWithinMonthOfUserGropedByDay(LocalDate.now().year, month, userId)
                                .flatMap { pair -> pair.map { RecordDto(it.status, it.date()) }.toList().map { DayStatistics.fillInGaps(it) }.map { it to LocalDate.of(LocalDate.now().year, month, pair.key) }.map(::DayStatistics) }
                                .toList()
                                .map { LocalDate.of(LocalDate.now().year, month, 1) to it }
                                .map(::MonthStatistics)
                                .toSingle()
                                .toPromise()
                                .then { render(json(it)) }
                    }
                }
                Post("work/check-in") {
                    val userId = instance<UserProfile>().id
                    val recordRepository = instance<RecordRepository>()
                    recordRepository.findLastRecordOfUser(userId)
                            .map(Record::status)
                            .flatMap { status -> recordRepository.insert(Record(!status, userId)).map { status } }
                            .toPromise()
                            .then { render(json(mapOf("status" to it))) }
                }
            }
        }
    }
}